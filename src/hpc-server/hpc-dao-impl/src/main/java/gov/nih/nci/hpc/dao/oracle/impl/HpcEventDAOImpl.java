/**
 * HpcEventDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

import gov.nih.nci.hpc.dao.HpcEventDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Event DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcEventDAOImpl implements HpcEventDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String INSERT_EVENT_SQL = "insert into public.\"HPC_EVENT\" ( "
			+ "\"USER_IDS\", \"TYPE\", \"PAYLOAD\", \"CREATED\") " + "values (?, ?, ?, ?)";

	private static final String GET_EVENTS_SQL = "select * from public.\"HPC_EVENT\"";

	private static final String DELETE_EVENT_SQL = "delete from public.\"HPC_EVENT\" where \"ID\" = ?";

	private static final String INSERT_EVENT_HISTORY_SQL = "insert into public.\"HPC_EVENT_HISTORY\" ( "
			+ "\"ID\", \"USER_IDS\", \"TYPE\", \"PAYLOAD\", \"CREATED\") " + "values (?, ?, ?, ?, ?)";

	private static final String GET_EVENT_HISTORY_SQL = "select * from public.\"HPC_EVENT_HISTORY\" where \"ID\" = ?";

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

	// HpcDocConfiguration Table to Object mapper.
	private RowMapper<HpcEvent> eventRowMapper = (rs, rowNum) -> {
		HpcEvent event = new HpcEvent();
		event.setId(rs.getInt("ID"));
		String userIds = rs.getString("USER_IDS");
		for (String userId : userIds.split(",")) {
			event.getUserIds().add(userId);
		}
		event.setType(HpcEventType.fromValue(rs.getString("TYPE")));
		event.getPayloadEntries().addAll(fromJSON(encryptor.decrypt(rs.getBytes("PAYLOAD"))));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		event.setCreated(created);

		return event;
	};

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcEventDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcEventDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void insertEvent(HpcEvent event) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_EVENT_SQL, toString(event.getUserIds()), event.getType().value(),
					encryptor.encrypt(toJSON(event.getPayloadEntries())), event.getCreated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert an event " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcEvent> getEvents() throws HpcException {
		try {
			return jdbcTemplate.query(GET_EVENTS_SQL, eventRowMapper);

		} catch (IncorrectResultSizeDataAccessException notFoundEx) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get notification events: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteEvent(int eventId) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_EVENT_SQL, eventId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a notification event" + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void insertEventHistory(HpcEvent event) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_EVENT_HISTORY_SQL, event.getId(), toString(event.getUserIds()),
					event.getType().value(), encryptor.encrypt(toJSON(event.getPayloadEntries())), event.getCreated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert an event to history table" + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcEvent getEventHistory(int id) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_EVENT_HISTORY_SQL, eventRowMapper, id);

		} catch (IncorrectResultSizeDataAccessException irse) {
			logger.error("Multiple events with the same ID found", irse);
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get an event from history: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert payload entries into a JSON string
	 * 
	 * @param payloadEntries List of payload entries.
	 * @return A JSON representation of the payload entries.
	 */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcEventPayloadEntry> payloadEntries) {
		if (payloadEntries == null || payloadEntries.isEmpty()) {
			return "";
		}

		JSONObject jsonPayload = new JSONObject();
		for (HpcEventPayloadEntry payloadEntry : payloadEntries) {
			jsonPayload.put(payloadEntry.getAttribute(), payloadEntry.getValue());
		}

		return jsonPayload.toJSONString();
	}

	/**
	 * Convert JSON string to payload entries.
	 * 
	 * @param jsonPayloadStr The Payload Entries JSON String.
	 * @return A list of event payload entries.
	 */
	private List<HpcEventPayloadEntry> fromJSON(String jsonPayloadStr) {
		List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();
		if (jsonPayloadStr == null || jsonPayloadStr.isEmpty()) {
			return payloadEntries;
		}

		// Parse the JSON string.
		JSONObject jsonPayload = null;
		try {
			jsonPayload = (JSONObject) (new JSONParser().parse(jsonPayloadStr));

		} catch (ParseException e) {
			return payloadEntries;
		}

		// Map all attributes to payload entries.
		for (Object attribue : jsonPayload.keySet()) {
			HpcEventPayloadEntry entry = new HpcEventPayloadEntry();
			entry.setAttribute(attribue.toString());
			entry.setValue(jsonPayload.get(attribue).toString());
			payloadEntries.add(entry);
		}

		return payloadEntries;
	}

	/**
	 * Map an array of user IDs to a single string.
	 * 
	 * @param userIds A list of user ids.
	 * @return A comma separated user-ids string.
	 */
	private String toString(List<String> userIds) {
		StringBuilder userIdsStr = new StringBuilder();
		for (String userId : userIds) {
			userIdsStr.append(userId + ",");
		}

		return userIdsStr.toString();
	}
}
