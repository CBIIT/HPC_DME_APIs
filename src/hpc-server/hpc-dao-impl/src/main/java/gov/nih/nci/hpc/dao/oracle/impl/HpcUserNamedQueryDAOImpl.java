/**
 * HpcUserNamedQueryDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcUserNamedQueryDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryAttributeMatch;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Named-Query DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcUserNamedQueryDAOImpl implements HpcUserNamedQueryDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_USER_QUERY_SQL = "merge into HPC_USER_QUERY using dual on (USER_ID = ? and QUERY_NAME = ?) "
			+ "when matched then update set QUERY = ?, DETAILED_RESPONSE = ?, TOTAL_COUNT = ?, QUERY_TYPE = ?, CREATED = ?, UPDATED = ? "
			+ "when not matched then insert (USER_ID, QUERY_NAME, QUERY, DETAILED_RESPONSE, TOTAL_COUNT, QUERY_TYPE, CREATED, UPDATED) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String DELETE_USER_QUERY_SQL = "delete from HPC_USER_QUERY where USER_ID = ? and QUERY_NAME = ?";

	private static final String GET_USER_QUERIES_SQL = "select * from HPC_USER_QUERY where USER_ID = ?";

	private static final String GET_USER_QUERY_SQL = "select * from HPC_USER_QUERY where USER_ID = ? and QUERY_NAME = ?";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	// TODO: Remove after Oracle migration
	@Qualifier("hpcOracleJdbcTemplate")
	// TODO: END
	private JdbcTemplate jdbcTemplate = null;

	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;

	// Row mappers.
	private RowMapper<HpcNamedCompoundMetadataQuery> userQueryRowMapper = (rs, rowNum) -> {
		HpcNamedCompoundMetadataQuery namedCompoundQuery = new HpcNamedCompoundMetadataQuery();
		namedCompoundQuery.setCompoundQuery(fromJSON(encryptor.decrypt(rs.getBytes("QUERY"))));
		namedCompoundQuery.setName(rs.getString("QUERY_NAME"));
		namedCompoundQuery.setDetailedResponse(rs.getBoolean("DETAILED_RESPONSE"));
		namedCompoundQuery.setTotalCount(rs.getBoolean("TOTAL_COUNT"));
		namedCompoundQuery.setCompoundQueryType(HpcCompoundMetadataQueryType.fromValue(rs.getString("QUERY_TYPE")));
		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		namedCompoundQuery.setCreated(created);
		Calendar updated = Calendar.getInstance();
		updated.setTime(rs.getTimestamp("UPDATED"));
		namedCompoundQuery.setUpdated(updated);
		return namedCompoundQuery;
	};

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcUserNamedQueryDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcUserNamedQueryDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsertQuery(String nciUserId, HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery)
			throws HpcException {
		try {
			byte[] encryptedQuery = encryptor.encrypt(toJSONString(namedCompoundMetadataQuery.getCompoundQuery()));
			jdbcTemplate.update(UPSERT_USER_QUERY_SQL, nciUserId, namedCompoundMetadataQuery.getName(), encryptedQuery,
					namedCompoundMetadataQuery.getDetailedResponse(), namedCompoundMetadataQuery.getTotalCount(),
					namedCompoundMetadataQuery.getCompoundQueryType().value(), namedCompoundMetadataQuery.getCreated(),
					namedCompoundMetadataQuery.getUpdated(), nciUserId, namedCompoundMetadataQuery.getName(),
					encryptedQuery, namedCompoundMetadataQuery.getDetailedResponse(),
					namedCompoundMetadataQuery.getTotalCount(),
					namedCompoundMetadataQuery.getCompoundQueryType().value(), namedCompoundMetadataQuery.getCreated(),
					namedCompoundMetadataQuery.getUpdated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a user query " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteQuery(String nciUserId, String queryName) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_USER_QUERY_SQL, nciUserId, queryName);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a user query" + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_USER_QUERIES_SQL, userQueryRowMapper, nciUserId);

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get user queries: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_USER_QUERY_SQL, userQueryRowMapper, nciUserId, queryName);

		} catch (IncorrectResultSizeDataAccessException irse) {
			logger.error("Multiple queries with the same name found", irse);
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a user query: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert compound query into a JSON string.
	 * 
	 * @param compoundMetadataQuery The compound metadata query to convert.
	 * @return A JSON representation of the compound query.
	 */
	private String toJSONString(HpcCompoundMetadataQuery compoundMetadataQuery) {
		return toJSON(compoundMetadataQuery).toJSONString();
	}

	/**
	 * Convert compound query into a JSON object.
	 * 
	 * @param compoundMetadataQuery The compound query.
	 * @return A JSON representation of the compound query.
	 */
	@SuppressWarnings("unchecked")
	private JSONObject toJSON(HpcCompoundMetadataQuery compoundMetadataQuery) {
		JSONObject jsonCompoundMetadataQuery = new JSONObject();

		if (compoundMetadataQuery == null) {
			return jsonCompoundMetadataQuery;
		}

		// Map the compound operator
		jsonCompoundMetadataQuery.put("operator", compoundMetadataQuery.getOperator().value());

		// Map the nested metadata queries.
		JSONArray jsonQueries = new JSONArray();
		for (HpcMetadataQuery nestedQuery : compoundMetadataQuery.getQueries()) {
			JSONObject jsonQuery = new JSONObject();
			if (nestedQuery.getAttribute() != null) {
				jsonQuery.put("attribute", nestedQuery.getAttribute());
			}
			jsonQuery.put("operator", nestedQuery.getOperator().value());
			jsonQuery.put("value", nestedQuery.getValue());
			if (nestedQuery.getFormat() != null) {
				jsonQuery.put("format", nestedQuery.getFormat());
			}
			if (nestedQuery.getLevelFilter() != null) {
				jsonQuery.put("level", nestedQuery.getLevelFilter().getLevel());
				jsonQuery.put("levelLabel", nestedQuery.getLevelFilter().getLabel());
				jsonQuery.put("levelOperator", nestedQuery.getLevelFilter().getOperator().value());
			}
			if (nestedQuery.getAttributeMatch() != null) {
				jsonQuery.put("attributeMatch", nestedQuery.getAttributeMatch().value());
			}

			jsonQueries.add(jsonQuery);
		}
		jsonCompoundMetadataQuery.put("queries", jsonQueries);

		// Map the nested compound queries.
		JSONArray jsonCompoundQueries = new JSONArray();
		for (HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
			jsonCompoundQueries.add(toJSON(nestedCompoundQuery));
		}
		jsonCompoundMetadataQuery.put("compoundQueries", jsonCompoundQueries);

		return jsonCompoundMetadataQuery;
	}

	/**
	 * Convert JSON string to HpcCompoundMetadataQuery domain object.
	 * 
	 * @param jsonCompoundMetadataQueryStr The compound query JSON string.
	 * @return A Compound Metadata Query.
	 */
	private HpcCompoundMetadataQuery fromJSON(String jsonCompoundMetadataQueryStr) {
		HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundMetadataQuery();
		if (StringUtils.isEmpty(jsonCompoundMetadataQueryStr)) {
			return compoundMetadataQuery;
		}

		// Parse the JSON string.
		JSONObject jsonCompoundMetadataQuery = null;
		try {
			jsonCompoundMetadataQuery = (JSONObject) (new JSONParser().parse(jsonCompoundMetadataQueryStr));

		} catch (ParseException e) {
			return compoundMetadataQuery;
		}

		return fromJSON(jsonCompoundMetadataQuery);
	}

	/**
	 * Convert JSON string to HpcCompoundMetadataQuery domain object.
	 * 
	 * @param jsonCompoundMetadataQuery The compound query JSON.
	 * @return A Compound Metadata Query.
	 */
	@SuppressWarnings("unchecked")
	private HpcCompoundMetadataQuery fromJSON(JSONObject jsonCompoundMetadataQuery) {
		HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundMetadataQuery();
		compoundMetadataQuery.setOperator(
				HpcCompoundMetadataQueryOperator.fromValue(jsonCompoundMetadataQuery.get("operator").toString()));

		// Map the nested metadata queries.
		JSONArray jsonQueries = (JSONArray) jsonCompoundMetadataQuery.get("queries");
		if (jsonQueries != null) {
			Iterator<JSONObject> queriesIterator = jsonQueries.iterator();
			while (queriesIterator.hasNext()) {
				compoundMetadataQuery.getQueries().add(metadataQueryFromJSON(queriesIterator.next()));
			}
		}

		// Map the nested compound metadata queries.
		JSONArray jsonCompoundQueries = (JSONArray) jsonCompoundMetadataQuery.get("compoundQueries");
		if (jsonCompoundQueries != null) {
			Iterator<JSONObject> compoundQueriesIterator = jsonCompoundQueries.iterator();
			while (compoundQueriesIterator.hasNext()) {
				compoundMetadataQuery.getCompoundQueries().add(fromJSON(compoundQueriesIterator.next()));
			}
		}

		return compoundMetadataQuery;
	}

	/**
	 * Instantiate a HpcMetadataQuery from JSON.
	 *
	 * @param jsonMetadataQuery The metadata query JSON object.
	 * @return A Metadata Query.
	 */
	private HpcMetadataQuery metadataQueryFromJSON(JSONObject jsonMetadataQuery) {
		HpcMetadataQuery metadataQuery = new HpcMetadataQuery();

		Object attribute = jsonMetadataQuery.get("attribute");
		if (attribute != null) {
			metadataQuery.setAttribute(attribute.toString());
		}
		metadataQuery.setOperator(HpcMetadataQueryOperator.fromValue(jsonMetadataQuery.get("operator").toString()));
		metadataQuery.setValue(jsonMetadataQuery.get("value").toString());
		Object level = jsonMetadataQuery.get("level");
		Object levelLabel = jsonMetadataQuery.get("levelLabel");
		Object levelOperator = jsonMetadataQuery.get("levelOperator");
		if ((level != null || levelLabel != null) && levelOperator != null) {
			HpcMetadataQueryLevelFilter levelFilter = new HpcMetadataQueryLevelFilter();
			if (level != null) {
				levelFilter.setLevel(Integer.valueOf(level.toString()));
			}
			if (levelLabel != null) {
				levelFilter.setLabel(levelLabel.toString());
			}
			levelFilter
					.setOperator(HpcMetadataQueryOperator.fromValue(jsonMetadataQuery.get("levelOperator").toString()));
			metadataQuery.setLevelFilter(levelFilter);
		}
		Object format = jsonMetadataQuery.get("format");
		if (format != null) {
			metadataQuery.setFormat(format.toString());
		}
		Object attributeMatch = jsonMetadataQuery.get("attributeMatch");
		if (attributeMatch != null) {
			metadataQuery.setAttributeMatch(HpcMetadataQueryAttributeMatch.fromValue(attributeMatch.toString()));
		}

		return metadataQuery;
	}
}
