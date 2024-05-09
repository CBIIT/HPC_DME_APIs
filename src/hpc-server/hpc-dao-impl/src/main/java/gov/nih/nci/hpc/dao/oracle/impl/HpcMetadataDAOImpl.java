/**
 * HpcMetadataDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcDupMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryAttributeMatch;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntryForCollection;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Metadata DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcMetadataDAOImpl implements HpcMetadataDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where meta_attr_value = ?";

	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where meta_attr_value <> ?";

	private static final String GET_COLLECTION_IDS_LIKE_SQL = " select object_id from r_coll_hierarchy_meta_main where lower(meta_attr_value) like lower(?)";

	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = " select object_id from r_coll_hierarchy_meta_main where num_less_than(meta_attr_value, ?) = '1'";

	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where num_less_or_equal(meta_attr_value, ?) = '1'";

	private static final String GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL = " select object_id from r_coll_hierarchy_meta_main where num_greater_than(meta_attr_value, ?) = '1'";

	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where num_greater_or_equal(meta_attr_value, ?) = '1'";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_LESS_THAN_SQL = " select object_id from r_coll_hierarchy_meta_main where timestamp_less_than(meta_attr_value, ?, ?) = '1'";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_GREATER_THAN_SQL = " select object_id from r_coll_hierarchy_meta_main where timestamp_greater_than(meta_attr_value, ?, ?) = '1'";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where timestamp_less_or_equal(meta_attr_value, ?, ?) = '1'";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL = " select object_id from r_coll_hierarchy_meta_main where timestamp_greater_or_equal(meta_attr_value, ?, ?) = '1'";

	private static final String GET_COLLECTION_EXACT_ATTRIBUTE_MATCH_FILTER = " and meta_attr_name = ?";

	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and meta_attr_value = ?";

	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and meta_attr_value <> ?";

	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and lower(meta_attr_value) like lower(?)";

	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and num_less_than(meta_attr_value, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and num_less_or_equal(meta_attr_value, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and num_greater_than(meta_attr_value, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and num_greater_or_equal(meta_attr_value, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_THAN_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and timestamp_less_than(meta_attr_value, ?, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_THAN_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and timestamp_greater_than(meta_attr_value, ?, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and timestamp_less_or_equal(meta_attr_value, ?, ?) = '1'";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL = " select object_id from r_data_hierarchy_meta_main where object_path like ? and timestamp_greater_or_equal(meta_attr_value, ?, ?) = '1'";

	private static final String GET_DATA_OBJECT_EXACT_ATTRIBUTE_MATCH_FILTER = " and meta_attr_name = ?";

	private static final String DATA_OBJECT_LEVEL_EQUAL_FILTER = " and data_level = ?";
	private static final String DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER = " and data_level <> ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER = " and data_level < ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and data_level <= ?";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER = " and data_level > ?";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and data_level >= ?";

	private static final String COLLECTION_LEVEL_EQUAL_FILTER = " and data_level = ?";
	private static final String COLLECTION_LEVEL_NOT_EQUAL_FILTER = " and data_level <> ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_THAN_FILTER = " and data_level < ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and data_level <= ?";
	private static final String COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER = " and data_level > ?";
	private static final String COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and data_level >= ?";

	private static final String DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER = " and level_label = ?";
	private static final String DATA_OBJECT_LEVEL_LABEL_NOT_EQUAL_FILTER = " and level_label <> ?";
	private static final String DATA_OBJECT_LEVEL_LABEL_LIKE_FILTER = " and level_label like ?";
	private static final String DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER_TERM = " level_label = ? ";

	private static final String COLLECTION_LEVEL_LABEL_EQUAL_FILTER = " and level_label = ?";
	private static final String COLLECTION_LEVEL_LABEL_NOT_EQUAL_FILTER = " and level_label <> ?";
	private static final String COLLECTION_LEVEL_LABEL_LIKE_FILTER = " and level_label like ?";
	private static final String COLLECTION_LEVEL_LABEL_EQUAL_FILTER_TERM = " level_label = ? ";

	private static final String USER_ACCESS_SQL = "(select 1 from R_USER_MAIN user_main, R_USER_GROUP groups, R_OBJT_ACCESS obj_access "
			+ "where user_main.USER_ID=groups.USER_ID " + "and groups.GROUP_USER_ID=obj_access.USER_ID "
			+ "and obj_access.object_id = main1.object_id " + "and user_main.USER_NAME = ?)";

	private static final String USER_ACCESS_ALL_SQL = "(select 1 from R_USER_MAIN user_main, R_USER_GROUP groups, R_OBJT_ACCESS obj_access "
			+ "where user_main.USER_ID=groups.USER_ID " + "and groups.GROUP_USER_ID=obj_access.USER_ID "
			+ "and obj_access.object_id = data_id " + "and user_main.USER_NAME = ?)";

	private static final String LIMIT_OFFSET_SQL = " order by object_id offset ? rows fetch next ? rows only";

	private static final String LIMIT_OFFSET_ALL_SQL = " offset ? rows fetch next ? rows only ";

	private static final String GET_COLLECTION_PATHS_SQL = "select distinct object_path from r_coll_hierarchy_meta_main mv, (select distinct object_id from ( ";

	private static final String GET_DETAILED_PARENT_COLLECTION_PATHS_SQL = "select mv.object_id, coll.coll_name, mv.object_path, coll.parent_coll_name, coll.coll_owner_name, "
			+ "coll.coll_owner_zone, coll.coll_map_id, coll.coll_inheritance, coll.r_comment, "
			+ "coll.coll_info1, coll.coll_info2, coll.create_ts, coll.r_comment, coll.coll_type, "
			+ "mv.meta_attr_name, mv.meta_attr_value, mv.meta_attr_unit, mv.data_level, mv.level_label, mv.coll_id "
			+ "from r_coll_hierarchy_meta_main mv, r_coll_main coll "
			+ "where mv.object_id = coll.coll_id and mv.object_path in ";
	
	private static final String GET_DETAILED_COLLECTION_PATHS_SQL = "select mv.object_id, coll.coll_name, mv.object_path, coll.parent_coll_name, coll.coll_owner_name, "
			+ "coll.coll_owner_zone, coll.coll_map_id, coll.coll_inheritance, coll.r_comment, "
			+ "coll.coll_info1, coll.coll_info2, coll.create_ts, coll.r_comment, coll.coll_type, "
			+ "mv.meta_attr_name, mv.meta_attr_value, mv.meta_attr_unit, mv.data_level, mv.level_label, mv.coll_id "
			+ "from r_coll_hierarchy_meta_main mv, r_coll_main coll, "
			+ "(select distinct object_id from ( ";

	private static final String GET_COLLECTION_COUNT_SQL = "select count(distinct object_id) from (";

	private static final String GET_DATA_OBJECT_PATHS_SQL = "select distinct object_path from r_data_hierarchy_meta_main mv, (select distinct object_id from ( ";

	private static final String GET_DETAILED_DATA_OBJECT_PATHS_SQL = "select mv.object_id, coll.coll_id, coll.coll_name, mv.object_path, data.data_size, "
			+ "data.data_path, data.data_owner_name, data.create_ts, mv.meta_attr_name, "
			+ "mv.meta_attr_value, mv.meta_attr_unit, mv.data_level, mv.level_label "
			+ "from r_data_hierarchy_meta_main mv, r_data_main data, r_coll_main coll, "
			+ "(select distinct object_id from ( ";

	private static final String GET_ALL_DATA_OBJECT_PATHS_SQL = "select main1.object_id, coll.coll_name, main1.object_path, "
			+ "data.data_owner_name, data.create_ts, main1.meta_attr_name, "
			+ "main1.meta_attr_value, main1.meta_attr_unit, main1.data_level, main1.level_label "
			+ "from (select data_id,coll_id,data_size,data_path,data_owner_name,create_ts from R_DATA_MAIN where ";

	private static final String GET_ALL_DATA_OBJECT_PATHS2_SQL = ") data join r_coll_main coll on data.coll_id=coll.coll_id "
			+ "join r_data_hierarchy_user_meta_main main1 on main1.object_id=data.data_id ";

	private static final String GET_DATA_OBJECT_COUNT_SQL = "select count(distinct object_id) from (";

	private static final String GET_ALL_DATA_OBJECT_COUNT_SQL = "select count(data_id) from R_DATA_MAIN where ";

	private static final String GET_ALL_DATA_OBJECT_COUNT2_SQL = " and exists(select 1 from r_data_hierarchy_user_meta_main where OBJECT_ID=data_id) ";

	private static final String GET_HIERARCHICAL_COLLECTION_METADATA_SQL = "select coll_id, meta_attr_name, meta_attr_value, meta_attr_unit, data_level, level_label "
			+ "from r_coll_hierarchy_meta_main where object_path = ? and data_level >= ? order by data_level";

	private static final String GET_COLLECTION_METADATA_SQL = "select META_ATTR_NAME, META_ATTR_VALUE, META_ATTR_UNIT, 1 as DATA_LEVEL, null as LEVEL_LABEL "
			+ "from R_META_MAIN meta_main, R_OBJT_METAMAP metamap, R_COLL_MAIN coll_main "
			+ "where coll_main.COLL_NAME = ? and meta_main.META_ID = metamap.META_ID and coll_main.COLL_ID = metamap.OBJECT_ID "
			+ "and exists (select 1 from R_USER_MAIN user_main, R_USER_GROUP groups, R_OBJT_ACCESS obj_access "
			+ "where user_main.USER_ID = groups.USER_ID and groups.GROUP_USER_ID = obj_access.USER_ID "
			+ "and obj_access.object_id = coll_main.COLL_ID and user_main.USER_NAME = ?)";

	private static final String GET_HIERARCHICAL_DATA_OBJECT_METADATA_SQL = "select meta_attr_name, meta_attr_value, meta_attr_unit, data_level, level_label "
			+ "from r_data_hierarchy_meta_main where object_path = ? and data_level >= ? order by data_level";

	private static final String GET_DATA_OBJECT_METADATA_SQL = "select META_ATTR_NAME, META_ATTR_VALUE, META_ATTR_UNIT, 1 as DATA_LEVEL, null as LEVEL_LABEL "
			+ "from R_META_MAIN meta_main, R_OBJT_METAMAP metamap, R_DATA_MAIN data_main, R_COLL_MAIN coll_main "
			+ "where coll_main.COLL_NAME = ? and data_main.DATA_NAME = ? "
			+ "and data_main.COLL_ID = coll_main.COLL_ID and meta_main.META_ID = metamap.META_ID and data_main.DATA_ID = metamap.OBJECT_ID "
			+ "and exists (select 1 from R_USER_MAIN user_main, R_USER_GROUP groups, R_OBJT_ACCESS obj_access "
			+ "where user_main.USER_ID = groups.USER_ID and groups.GROUP_USER_ID = obj_access.USER_ID "
			+ "and obj_access.object_id = data_main.DATA_ID and user_main.USER_NAME = ?)";

	private static final String GET_COLLECTION_METADATA_ATTRIBUTES_SQL = "r_coll_meta_attributes ";

	private static final String GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL = "r_data_meta_attributes ";

	private static final String GET_COLLECTION_METADATA_AGGREGATE_SQL = "select level_label, rtrim(xmlagg(xmlelement(e, meta_attr_name, ',').extract('//text()') "
			+ "order by meta_attr_name).getClobVal(),',') as attributes from "
			+ GET_COLLECTION_METADATA_ATTRIBUTES_SQL;

	private static final String GET_DATA_OBJECT_METADATA_AGGREGATE_SQL = "select level_label, rtrim(xmlagg(xmlelement(e, meta_attr_name, ',').extract('//text()') "
			+ "order by meta_attr_name).getClobVal(),',') as attributes from "
			+ GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL;

	private static final String GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL = " group by level_label order by level_label";

	private static final String GET_METADATA_MODIFIED_AT_SQL = "select max(cast(modify_ts as bigint)) from r_objt_metamap where object_id = ?";

	private static final String REFRESH_VIEWS_SQL = "call REFRESH_HOURLY_MATERIALIZED_VIEW()";

	private static final String GET_DATA_OBJECTS_SQL = "select data_main.DATA_ID, data_main.COLL_ID, data_main.DATA_NAME, coll_main.COLL_NAME, "
			+ "data_main.DATA_SIZE, data_main.DATA_PATH, data_main.DATA_OWNER_NAME, data_main.CREATE_TS "
			+ "from R_META_MAIN meta_main, R_OBJT_METAMAP metamap, R_DATA_MAIN data_main, R_COLL_MAIN coll_main "
			+ "where data_main.COLL_ID = coll_main.COLL_ID and meta_main.META_ID = metamap.META_ID and data_main.DATA_ID = metamap.OBJECT_ID "
			+ "and meta_main.META_ATTR_NAME = ? and meta_main.META_ATTR_VALUE = ? "
			+ "and exists (select 1 from R_USER_MAIN user_main, R_USER_GROUP groups, R_OBJT_ACCESS obj_access "
			+ "where user_main.USER_ID = groups.USER_ID and groups.GROUP_USER_ID = obj_access.USER_ID "
			+ "and obj_access.object_id = data_main.DATA_ID and user_main.USER_NAME = ?)";

	private static final String GET_DUP_DATA_OBJECT_METADATA_SQL = "select meta_attr_name, object_id, count(*) "
			+ "FROM r_meta_main a, r_objt_metamap b, r_data_main c "
			+ "WHERE a.meta_id = b.meta_id AND b.object_id = c.data_id "
			+ "GROUP BY a.meta_attr_name, b.object_id having count(*)> 1";
	
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	private int fetchSize = 1000;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// Row mappers.
	private SingleColumnRowMapper<String> objectPathRowMapper = new SingleColumnRowMapper<>();
	private SingleColumnRowMapper<Long> objectIdRowMapper = new SingleColumnRowMapper<>();
	private RowMapper<HpcMetadataLevelAttributes> metadataLevelAttributeRowMapper = (rs, rowNum) -> {
		HpcMetadataLevelAttributes metadataLevelAttributes = new HpcMetadataLevelAttributes();
		metadataLevelAttributes.setLevelLabel(rs.getString("LEVEL_LABEL"));

		// Extract the metadata attributes for this level. Defensive coding to exclude
		// any null values.
		String attributes = rs.getString("ATTRIBUTES");
		if (!StringUtils.isEmpty(attributes)) {
			for (String attribute : attributes.split(",")) {
				metadataLevelAttributes.getMetadataAttributes().add(attribute);
			}
		}

		return metadataLevelAttributes;
	};
	private RowMapper<HpcMetadataEntry> metadataEntryRowMapper = (rs, rowNum) -> {
		HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
		metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
		metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
		String unit = rs.getString("META_ATTR_UNIT");
		metadataEntry.setUnit(unit != null && !unit.isEmpty() ? unit : null);

		Long level = rs.getLong("DATA_LEVEL");
		metadataEntry.setLevel(level != null ? level.intValue() : null);
		metadataEntry.setLevelLabel(rs.getString("LEVEL_LABEL"));

		return metadataEntry;
	};

	private RowMapper<HpcMetadataEntry> collectionMetadataEntryRowMapper = (rs, rowNum) -> {
		HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
		Long collId = rs.getLong("COLL_ID");
		metadataEntry.setCollectionId(collId != null ? collId.intValue() : null);
		metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
		metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
		String unit = rs.getString("META_ATTR_UNIT");
		metadataEntry.setUnit(unit != null && !unit.isEmpty() ? unit : null);

		Long level = rs.getLong("DATA_LEVEL");
		metadataEntry.setLevel(level != null ? level.intValue() : null);
		metadataEntry.setLevelLabel(rs.getString("LEVEL_LABEL"));

		return metadataEntry;
	};

	private RowMapper<HpcSearchMetadataEntry> searchMetadataEntryRowMapper = (rs, rowNum) -> {
		HpcSearchMetadataEntry searchMetadataEntry = new HpcSearchMetadataEntry();
		Long id = rs.getLong(1);
		searchMetadataEntry.setId(id != null ? id.intValue() : null);
		Long collId = rs.getLong(2);
		searchMetadataEntry.setCollectionId(collId != null ? collId.intValue() : null);
		searchMetadataEntry.setCollectionName(rs.getString(3));
		searchMetadataEntry.setAbsolutePath(rs.getString(4));
		Long dataSize = rs.getLong(5);
		searchMetadataEntry.setDataSize(dataSize != null ? dataSize.longValue() : null);
		searchMetadataEntry.setDataPath(rs.getString(6));
		searchMetadataEntry.setDataOwnerName(rs.getString(7));
		String createTs = rs.getString(8);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(Long.parseLong(createTs) * 1000);
		searchMetadataEntry.setCreatedAt(cal);
		searchMetadataEntry.setAttribute(rs.getString(9));
		searchMetadataEntry.setValue(rs.getString(10));
		searchMetadataEntry.setUnit(rs.getString(11));
		Long level = rs.getLong(12);
		searchMetadataEntry.setLevel(level != null ? level.intValue() : null);
		searchMetadataEntry.setLevelLabel(rs.getString(13));

		return searchMetadataEntry;
	};

	private RowMapper<HpcSearchMetadataEntry> searchExtMetadataEntryRowMapper = (rs, rowNum) -> {
		HpcSearchMetadataEntry searchMetadataEntry = new HpcSearchMetadataEntry();
		Long id = rs.getLong(1);
		searchMetadataEntry.setId(id != null ? id.intValue() : null);
		searchMetadataEntry.setCollectionName(rs.getString(2));
		searchMetadataEntry.setAbsolutePath(rs.getString(3));
		searchMetadataEntry.setDataOwnerName(rs.getString(4));
		String createTs = rs.getString(5);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(Long.parseLong(createTs) * 1000);
		searchMetadataEntry.setCreatedAt(cal);
		searchMetadataEntry.setAttribute(rs.getString(6));
		searchMetadataEntry.setValue(rs.getString(7));
		searchMetadataEntry.setUnit(rs.getString(8));
		Long level = rs.getLong(9);
		searchMetadataEntry.setLevel(level != null ? level.intValue() : null);
		searchMetadataEntry.setLevelLabel(rs.getString(10));

		return searchMetadataEntry;
	};

	private RowMapper<HpcSearchMetadataEntryForCollection> searchMetadataEntryForCollRowMapper = (rs, rowNum) -> {
		HpcSearchMetadataEntryForCollection searchMetadataEntry = new HpcSearchMetadataEntryForCollection();
		Long collId = rs.getLong(1);
		searchMetadataEntry.setCollectionId(collId != null ? collId.intValue() : null);
		searchMetadataEntry.setCollectionName(rs.getString(2));
		searchMetadataEntry.setAbsolutePath(rs.getString(3));
		searchMetadataEntry.setCollectionParentName(rs.getString(4));
		searchMetadataEntry.setCollectionOwnerName(rs.getString(5));
		searchMetadataEntry.setCollectionOwnerZone(rs.getString(6));
		Long collMapId = rs.getLong(7);
		searchMetadataEntry.setCollectionMapId(collMapId != null ? collMapId.toString() : null);
		searchMetadataEntry.setCollectionInheritance(rs.getString(8));
		searchMetadataEntry.setComments(rs.getString(9));
		searchMetadataEntry.setInfo1(rs.getString(10));
		searchMetadataEntry.setInfo2(rs.getString(11));
		String createdAt = rs.getString(12);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(Long.parseLong(createdAt) * 1000);
		searchMetadataEntry.setCreatedAt(cal);
		searchMetadataEntry.setComments(rs.getString(13));
		searchMetadataEntry.setSpecColType(rs.getString(14));
		searchMetadataEntry.setAttribute(rs.getString(15));
		searchMetadataEntry.setValue(rs.getString(16));
		searchMetadataEntry.setUnit(rs.getString(17));
		Integer level = rs.getInt(18);
		searchMetadataEntry.setLevel(level != null ? level.intValue() : null);
		searchMetadataEntry.setLevelLabel(rs.getString(19));
		Long metaCollId = rs.getLong(20);
		searchMetadataEntry.setMetaCollectionId(metaCollId != null ? metaCollId.intValue() : null);

		return searchMetadataEntry;
	};
	private RowMapper<HpcCollectionListingEntry> browseMetadataRowMapper = (rs, rowNum) -> {
		HpcCollectionListingEntry metadataEntry = new HpcCollectionListingEntry();
		Integer id = rs.getInt("id");
		metadataEntry.setId(id != null ? id.intValue() : null);
		metadataEntry.setPath(rs.getString("path"));
		Long size = rs.getLong("data_size");
		metadataEntry.setDataSize(size != null ? size.longValue() : null);
		if (rs.getTimestamp("uploaded") != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(rs.getTimestamp("uploaded"));
			metadataEntry.setCreatedAt(cal);
		}
		return metadataEntry;
	};
	private RowMapper<HpcDataObject> dataObjectRowMapper = (rs, rowNum) -> {
		HpcDataObject dataObject = new HpcDataObject();

		dataObject.setId(rs.getInt("DATA_ID"));
		dataObject.setCollectionId(rs.getInt("COLL_ID"));
		dataObject.setCollectionName(rs.getString("COLL_NAME"));
		dataObject.setAbsolutePath(rs.getString("COLL_NAME") + "/" + rs.getString("DATA_NAME"));
		dataObject.setDataSize(rs.getLong("DATA_SIZE"));
		dataObject.setDataPath(rs.getString("DATA_PATH"));
		dataObject.setDataOwnerName(rs.getString("DATA_OWNER_NAME"));

		String createdAtStr = rs.getString("CREATE_TS");
		if (createdAtStr != null) {
			try {
				Calendar createdAt = Calendar.getInstance();
				createdAt.setTimeInMillis(Long.valueOf(createdAtStr) * 1000L);
				dataObject.setCreatedAt(createdAt);

			} catch (NumberFormatException e) {
				logger.error("Unexpected timestamp value: [{}] - {}", dataObject.getAbsolutePath(), createdAtStr);
			}
		}

		return dataObject;
	};

	private RowMapper<HpcDupMetadataEntry> dupMetadataRowMapper = (rs, rowNum) -> {
		HpcDupMetadataEntry dupMetadataEntry = new HpcDupMetadataEntry();

		dupMetadataEntry.setId(rs.getInt("OBJECT_ID"));
		dupMetadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));

		return dupMetadataEntry;
	};
	
	// SQL Maps from operators to queries and filters.
	private HpcSQLMaps dataObjectSQL = new HpcSQLMaps();
	private HpcSQLMaps collectionSQL = new HpcSQLMaps();

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcMetadataDAOImpl(int fetchSize) {
		this.fetchSize = fetchSize;

		dataObjectSQL.queries.put(HpcMetadataQueryOperator.EQUAL, GET_DATA_OBJECT_IDS_EQUAL_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.NOT_EQUAL, GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.LIKE, GET_DATA_OBJECT_IDS_LIKE_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL,
				GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL,
				GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_LESS_THAN,
				GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_THAN_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_GREATER_THAN,
				GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_THAN_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_LESS_OR_EQUAL,
				GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL);
		dataObjectSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_GREATER_OR_EQUAL,
				GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL);

		collectionSQL.queries.put(HpcMetadataQueryOperator.EQUAL, GET_COLLECTION_IDS_EQUAL_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.NOT_EQUAL, GET_COLLECTION_IDS_NOT_EQUAL_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.LIKE, GET_COLLECTION_IDS_LIKE_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, GET_COLLECTION_IDS_NUM_LESS_THAN_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL,
				GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_LESS_THAN,
				GET_COLLECTION_IDS_TIMESTAMP_LESS_THAN_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_GREATER_THAN,
				GET_COLLECTION_IDS_TIMESTAMP_GREATER_THAN_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_LESS_OR_EQUAL,
				GET_COLLECTION_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL);
		collectionSQL.queries.put(HpcMetadataQueryOperator.TIMESTAMP_GREATER_OR_EQUAL,
				GET_COLLECTION_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL);

		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.EQUAL, DATA_OBJECT_LEVEL_EQUAL_FILTER);
		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER);
		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER);
		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL,
				DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN,
				DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER);
		dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL,
				DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);

		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.EQUAL, COLLECTION_LEVEL_EQUAL_FILTER);
		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, COLLECTION_LEVEL_NOT_EQUAL_FILTER);
		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, COLLECTION_LEVEL_NUM_LESS_THAN_FILTER);
		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL,
				COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN,
				COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER);
		collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL,
				COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);

		dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.EQUAL, DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER);
		dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL,
				DATA_OBJECT_LEVEL_LABEL_NOT_EQUAL_FILTER);
		dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.LIKE, DATA_OBJECT_LEVEL_LABEL_LIKE_FILTER);

		collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.EQUAL, COLLECTION_LEVEL_LABEL_EQUAL_FILTER);
		collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL,
				COLLECTION_LEVEL_LABEL_NOT_EQUAL_FILTER);
		collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.LIKE, COLLECTION_LEVEL_LABEL_LIKE_FILTER);

		dataObjectSQL.exactAttributeMatchFilter = GET_DATA_OBJECT_EXACT_ATTRIBUTE_MATCH_FILTER;
		collectionSQL.exactAttributeMatchFilter = GET_COLLECTION_EXACT_ATTRIBUTE_MATCH_FILTER;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcMetadataDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {
		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL,
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter, null), dataManagementUsername, offset,
				limit, false, false, true));
	}

	@Override
	public List<HpcSearchMetadataEntryForCollection> getDetailedCollectionPaths(
			HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {

		return getDetailedPathsForCollection(prepareQuery(GET_DETAILED_COLLECTION_PATHS_SQL,
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter, null), dataManagementUsername, offset,
				limit, true, false, true));
	}

	@Override
	public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {
		return getCount(prepareQuery(GET_COLLECTION_COUNT_SQL,
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter, null), dataManagementUsername, null, null,
				false, true, true));
	}

	@Override
	public List<String> getDataObjectPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter, path), dataManagementUsername, offset,
				limit, false, false, false));
	}

	@Override
	public List<HpcSearchMetadataEntry> getDetailedDataObjectPaths(String path,
			HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {

		return getDetailedPaths(prepareQuery(GET_DETAILED_DATA_OBJECT_PATHS_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter, path), dataManagementUsername, offset,
				limit, true, false, false));
	}

	@Override
	public List<HpcSearchMetadataEntry> getAllDataObjectPaths(String path, String dataManagementUsername, int offset,
			int limit) throws HpcException {
		return getExtDetailedPaths(prepareAllQuery(GET_ALL_DATA_OBJECT_PATHS_SQL, GET_ALL_DATA_OBJECT_PATHS2_SQL, path,
				dataManagementUsername, offset, limit));
	}

	@Override
	public int getDataObjectCount(String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {
		return getCount(prepareQuery(GET_DATA_OBJECT_COUNT_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter, path), dataManagementUsername, null, null,
				false, true, false));
	}

	@Override
	public int getAllDataObjectCount(String path, String dataManagementUsername) throws HpcException {
		return getCount(prepareAllQuery(GET_ALL_DATA_OBJECT_COUNT_SQL, GET_ALL_DATA_OBJECT_COUNT2_SQL, path,
				dataManagementUsername, null, null));
	}

	@Override
	public List<String> getDataObjectParentPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {

		List<String> fullPaths = getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter, path), dataManagementUsername, offset,
				limit, false, false, false));

		if (CollectionUtils.isEmpty(fullPaths))
			return new ArrayList<>();

		// Convert the data object paths to collection paths
		List<String> paths = new ArrayList<>();
		for (String fullPath : fullPaths) {
			String colPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
			if (!paths.contains(colPath)) {
				paths.add(colPath);
			}
		}

		return paths;
	}

	@Override
	public List<HpcSearchMetadataEntryForCollection> getDetailedDataObjectParentPaths(String path,
			HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {

		List<String> fullPaths = getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter, path), dataManagementUsername, offset,
				limit, false, false, false));

		if (CollectionUtils.isEmpty(fullPaths))
			return new ArrayList<>();

		// Convert the data object paths to collection paths
		List<String> paths = new ArrayList<>();
		for (String fullPath : fullPaths) {
			String colPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
			if (!paths.contains(colPath)) {
				paths.add(colPath);
			}
		}

		return getDetailedPathsForCollection(
				prepareQuery(GET_DETAILED_PARENT_COLLECTION_PATHS_SQL, toQuery(paths), null, null, null, true, true, true));
	}

	@Override
	public List<HpcMetadataEntry> getCollectionMetadata(String path, int minLevel) throws HpcException {
		try {
			return jdbcTemplate.query(GET_HIERARCHICAL_COLLECTION_METADATA_SQL, collectionMetadataEntryRowMapper, path,
					minLevel);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection hierarchical metadata: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcMetadataEntry> getCollectionMetadata(String path, String dataManagementUsername)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_METADATA_SQL, metadataEntryRowMapper, path,
					dataManagementUsername);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection metadata: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcMetadataEntry> getDataObjectMetadata(String path, int minLevel) throws HpcException {
		try {
			return jdbcTemplate.query(GET_HIERARCHICAL_DATA_OBJECT_METADATA_SQL, metadataEntryRowMapper, path,
					minLevel);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object hierarchical metadata: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcMetadataEntry> getDataObjectMetadata(String path, String dataManagementUsername)
			throws HpcException {
		try {
			String collectionPath = path.substring(0, path.lastIndexOf('/'));
			String dataObjectName = path.substring(path.lastIndexOf('/') + 1);
			return jdbcTemplate.query(GET_DATA_OBJECT_METADATA_SQL, metadataEntryRowMapper, collectionPath,
					dataObjectName, dataManagementUsername);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object metadata: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataObject> getDataObjects(List<HpcMetadataQuery> metadataQueries, String dataManagementUsername)
			throws HpcException {
		// Input validation
		if (metadataQueries.size() != 1) {
			throw new HpcException("Unexpected number of metadata queries received: " + metadataQueries.size(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		HpcMetadataQuery metadataQuery = metadataQueries.get(0);
		if (!HpcMetadataQueryOperator.EQUAL.equals(metadataQuery.getOperator())) {
			throw new HpcException("Unexpected operator in metadata received: " + metadataQuery.getOperator(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return jdbcTemplate.query(GET_DATA_OBJECTS_SQL, dataObjectRowMapper, metadataQuery.getAttribute(),
				metadataQuery.getValue(), dataManagementUsername);
	}

	@Override
	public List<HpcMetadataLevelAttributes> getCollectionMetadataAttributes(String levelLabel) throws HpcException {
		return getMetadataAttributes(GET_COLLECTION_METADATA_AGGREGATE_SQL, levelLabel,
				COLLECTION_LEVEL_LABEL_EQUAL_FILTER_TERM);
	}

	@Override
	public List<HpcMetadataLevelAttributes> getDataObjectMetadataAttributes(String levelLabel) throws HpcException {
		return getMetadataAttributes(GET_DATA_OBJECT_METADATA_AGGREGATE_SQL, levelLabel,
				DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER_TERM);
	}

	@Override
	public Calendar getMetadataModifiedAt(int id) throws HpcException {
		try {
			Calendar modifiedAt = Calendar.getInstance();
			modifiedAt.setTimeInMillis(
					1000 * jdbcTemplate.queryForObject(GET_METADATA_MODIFIED_AT_SQL, objectIdRowMapper, id));
			return modifiedAt;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection/data-object Paths: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void refreshViews() throws HpcException {
		try {
			jdbcTemplate.execute(REFRESH_VIEWS_SQL);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to refresh hourly materialized views: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcCollectionListingEntry> getBrowseMetadataByIds(List<Integer> ids) throws HpcException {
		List<HpcCollectionListingEntry> entries = new ArrayList<>();
		try {
			List<Integer> queryIds = new ArrayList<>();
			for (int i = 0; i < ids.size(); i += 1000) {
				queryIds.clear();
				queryIds.addAll(ids.subList(i, ids.size() < i + 1000 ? ids.size() : i + 1000));
				String inSql = String.join(",", Collections.nCopies(queryIds.size(), "?"));
				entries.addAll(
						jdbcTemplate.query(String.format("SELECT * FROM r_browse_meta_main WHERE id IN (%s)", inSql),
								queryIds.toArray(), browseMetadataRowMapper));
			}
			return entries;
		} catch (DataAccessException e) {
			throw new HpcException("Failed to get browse metadata : " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDupMetadataEntry> getDupDataObjectMetadataEntries() throws HpcException {
		try {
			return jdbcTemplate.query(GET_DUP_DATA_OBJECT_METADATA_SQL, dupMetadataRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to detect data object duplicate metadata: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}
	
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	// Prepared query.
	private class HpcPreparedQuery {
		private String sql = null;
		private Object[] args = null;
	}

	// SQL Maps from operators to queries and filters.
	private class HpcSQLMaps {
		private EnumMap<HpcMetadataQueryOperator, String> queries = new EnumMap<>(HpcMetadataQueryOperator.class);
		private EnumMap<HpcMetadataQueryOperator, String> levelFilters = new EnumMap<>(HpcMetadataQueryOperator.class);
		private EnumMap<HpcMetadataQueryOperator, String> levelLabelFilters = new EnumMap<>(
				HpcMetadataQueryOperator.class);
		private String exactAttributeMatchFilter = null;
	}

	
	/**
	 * Prepare a SQL query. Map operators to SQL and concatenate them with
	 * 'intersect'.
	 * 
	 * @param getObjectPathsQuery    The query to get object paths based on object
	 *                               IDs.
	 * @param userQuery              The calculated SQL query based on user input
	 *                               (represented by query domain objects).
	 * @param dataManagementUsername The data management user name.
	 * @param offset                 Skip that many path in the returned results.
	 * @param limit                  No more than 'limit' paths will be returned.
	 * @param detail				 True if this is a detailed query. False otherwise
	 * @param count					 True if this is count query. False otherwise
	 * @param isCollection			 True if this is a collection query. False otherwise
	 * @return A prepared query.
	 */
	private HpcPreparedQuery prepareQuery(String getObjectPathsQuery, HpcPreparedQuery userQuery,
			String dataManagementUsername, Integer offset, Integer limit, Boolean detail, Boolean count, Boolean isCollection) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		// Combine the metadata queries into a single SQL statement.
		sqlQueryBuilder.append(getObjectPathsQuery);

		// Add query to search for requested metadata
		if (userQuery != null) {
			sqlQueryBuilder.append(userQuery.sql);
			args.addAll(Arrays.asList(userQuery.args));
			if (!getObjectPathsQuery.equals(GET_DETAILED_PARENT_COLLECTION_PATHS_SQL))
				sqlQueryBuilder.append(") main1 ");
		}

		// Add a query to only include entities the user can access.
		if (dataManagementUsername != null) {
			sqlQueryBuilder.append(" where exists ");
			sqlQueryBuilder.append(USER_ACCESS_SQL);
			args.add(dataManagementUsername);
		}

		if (offset != null && limit != null) {
			sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
			args.add(offset);
			args.add(limit);
		}

		if(isCollection && detail & !count)
			sqlQueryBuilder.append(") hit where hit.object_id = mv.object_id and coll.coll_id = hit.object_id");
		else if (isCollection && !detail & !count)
			sqlQueryBuilder.append(") hit where hit.object_id = mv.object_id");
		else if(detail & !count)
			sqlQueryBuilder.append(") hit where hit.object_id = mv.object_id and data.data_id = mv.object_id and coll.coll_id = data.coll_id");
		else if(!detail & !count)
			sqlQueryBuilder.append(") hit where hit.object_id = mv.object_id");
		
		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();
		return preparedQuery;
	}

	private HpcPreparedQuery prepareAllQuery(String getAllObjectPathsQuery, String getAllObjectPathsQuery2, String path,
			String dataManagementUsername, Integer offset, Integer limit) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		// Combine the two queries into a single SQL statement.
		sqlQueryBuilder.append(getAllObjectPathsQuery);

		// Add query to limit results to within the path if specified
		if (path != null) {
			sqlQueryBuilder.append("data_path LIKE ?");
			args.add("%" + path + "%");
		}

		// Add a query to only include entities the user can access.
		if (dataManagementUsername != null) {
			if (path == null)
				sqlQueryBuilder.append(" exists ");
			else
				sqlQueryBuilder.append(" and exists ");
			sqlQueryBuilder.append(USER_ACCESS_ALL_SQL);
			args.add(dataManagementUsername);
		}

		if (offset != null && limit != null) {
			sqlQueryBuilder.append(LIMIT_OFFSET_ALL_SQL);
			args.add(offset);
			args.add(limit);
		}

		if (getAllObjectPathsQuery2 != null)
			sqlQueryBuilder.append(getAllObjectPathsQuery2);

		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();

		logger.debug("Generated prepared query for {} ", path);
		return preparedQuery;
	}

	/**
	 * Create a SQL statement from List&lt;HpcMetadataQuery&gt;.
	 *
	 * @param sql                The map from query operator to SQL queries and
	 *                           filters.
	 * @param metadataQueries    The metadata queries.
	 * @param operator           The compound metadata query operator to use.
	 * @param defaultLevelFilter A default level filter to use if not provided in
	 *                           the query.
	 * @return A prepared query.
	 * @throws HpcException If invalid metadata query operator provided.
	 */
	private HpcPreparedQuery toQuery(HpcSQLMaps sql, List<HpcMetadataQuery> metadataQueries,
			HpcCompoundMetadataQueryOperator operator, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append("(");
		for (HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sql.queries.get(metadataQuery.getOperator());
			if (sqlQuery == null) {
				throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Append the compound metadata query operator if not the first query in the
			// list.
			if (!args.isEmpty()) {
				sqlQueryBuilder.append(" " + toSQLOperator(operator) + " ");
			}

			// Append the SQL query representing the requested metadata query operator and
			// its arguments.
			sqlQueryBuilder.append(sqlQuery);
			args.add(metadataQuery.getValue());
			if (!StringUtils.isEmpty(metadataQuery.getFormat())) {
				args.add(metadataQuery.getFormat());
			}

			// Optionally append a filter to have exact attribute match.
			if (metadataQuery.getAttributeMatch() == null
					|| metadataQuery.getAttributeMatch().equals(HpcMetadataQueryAttributeMatch.EXACT)) {
				sqlQueryBuilder.append(sql.exactAttributeMatchFilter);
				args.add(metadataQuery.getAttribute());
			}

			// Add a filter for level.
			HpcMetadataQueryLevelFilter levelFilter = metadataQuery.getLevelFilter() != null
					? metadataQuery.getLevelFilter()
					: defaultLevelFilter;
			if (levelFilter != null) {
				boolean labelFilter = levelFilter.getLabel() != null;
				String sqlLevelFilter = labelFilter ? sql.levelLabelFilters.get(levelFilter.getOperator())
						: sql.levelFilters.get(levelFilter.getOperator());
				if (sqlLevelFilter == null) {
					throw new HpcException("Invalid level operator: " + levelFilter.getOperator(),
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
				sqlQueryBuilder.append(sqlLevelFilter);
				args.add(labelFilter ? levelFilter.getLabel() : levelFilter.getLevel());
			}
		}

		sqlQueryBuilder.append(")");

		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();
		return preparedQuery;
	}

	/**
	 * Create a SQL statement from List&lt;HpcMetadataQuery&gt;.
	 *
	 * @param sql                The map from query operator to SQL queries and
	 *                           filters.
	 * @param metadataQueries    The metadata queries.
	 * @param operator           The compound metadata query operator to use.
	 * @param defaultLevelFilter A default level filter to use if not provided in
	 *                           the query.
	 * @param path               The path to search in.
	 * @return A prepared query.
	 * @throws HpcException If invalid metadata query operator provided.
	 */
	private HpcPreparedQuery toQuery(HpcSQLMaps sql, List<HpcMetadataQuery> metadataQueries,
			HpcCompoundMetadataQueryOperator operator, HpcMetadataQueryLevelFilter defaultLevelFilter,
			String path)
			throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		for (HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sql.queries.get(metadataQuery.getOperator());
			if (sqlQuery == null) {
				throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Append the compound metadata query operator if not the first query in the
			// list.
			if (!args.isEmpty()) {
				sqlQueryBuilder.append(" " + toSetOperator(operator) + " ");
			}

			// Append the SQL query representing the requested metadata query operator and
			// its arguments.
			sqlQueryBuilder.append(sqlQuery);
			
			// Append the path argument to limit results to within the path if specified
			if (path != null)
				args.add(path + "%");
				
			args.add(metadataQuery.getValue());
			if (!StringUtils.isEmpty(metadataQuery.getFormat())) {
				args.add(metadataQuery.getFormat());
			}

			// Optionally append a filter to have exact attribute match.
			if (metadataQuery.getAttributeMatch() == null
					|| metadataQuery.getAttributeMatch().equals(HpcMetadataQueryAttributeMatch.EXACT)) {
				sqlQueryBuilder.append(sql.exactAttributeMatchFilter);
				args.add(metadataQuery.getAttribute());
			}

			// Add a filter for level.
			HpcMetadataQueryLevelFilter levelFilter = metadataQuery.getLevelFilter() != null
					? metadataQuery.getLevelFilter()
					: defaultLevelFilter;
			if (levelFilter != null) {
				boolean labelFilter = levelFilter.getLabel() != null;
				String sqlLevelFilter = labelFilter ? sql.levelLabelFilters.get(levelFilter.getOperator())
						: sql.levelFilters.get(levelFilter.getOperator());
				if (sqlLevelFilter == null) {
					throw new HpcException("Invalid level operator: " + levelFilter.getOperator(),
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
				sqlQueryBuilder.append(sqlLevelFilter);
				args.add(labelFilter ? levelFilter.getLabel() : levelFilter.getLevel());
			}
		}

		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();
		return preparedQuery;
	}
	
	/**
	 * Create a SQL statement from List&lt;String&gt;.
	 *
	 * @param paths The list of paths
	 * @return A prepared query.
	 */
	private HpcPreparedQuery toQuery(List<String> paths) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append("(");
		for (String path : paths) {
			if (sqlQueryBuilder.length() > 1) {
				sqlQueryBuilder.append(",");
			}
			sqlQueryBuilder.append("\'" + path + "\'");
		}
		sqlQueryBuilder.append(")");

		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();
		return preparedQuery;
	}

	/**
	 * Create a SQL statement from HpcCompoundMetadataQuery.
	 *
	 * @param sql                   The map from query operator to SQL queries and
	 *                              filters.
	 * @param compoundMetadataQuery The compound query to create SQL from.
	 * @param defaultLevelFilter    A default level filter to use if not provided in
	 *                              the query.
	 * @param path    				The path to search in
	 * @return A prepared query.
	 * @throws HpcException on service failure.
	 */
	private HpcPreparedQuery toQuery(HpcSQLMaps sql, HpcCompoundMetadataQuery compoundMetadataQuery,
			HpcMetadataQueryLevelFilter defaultLevelFilter, String path) throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		if (compoundMetadataQuery == null) {
			return null;
		}

		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if (compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sql, compoundMetadataQuery.getQueries(),
					compoundMetadataQuery.getOperator(), defaultLevelFilter, path);
			sqlQueryBuilder.append(query.sql);
			args.addAll(Arrays.asList(query.args));
		}

		// Append the nested compound queries.
		if (compoundMetadataQuery.getCompoundQueries() != null
				&& !compoundMetadataQuery.getCompoundQueries().isEmpty()) {
			if (!args.isEmpty()) {
				sqlQueryBuilder.append(" " + toSetOperator(compoundMetadataQuery.getOperator()) + " ");
			}
			boolean firstNestedQuery = true;
			for (HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
				if (!firstNestedQuery) {
					sqlQueryBuilder.append(" " + toSetOperator(compoundMetadataQuery.getOperator()) + " ");
				} else {
					firstNestedQuery = false;
				}
				HpcPreparedQuery query = toQuery(sql, nestedCompoundQuery, defaultLevelFilter, path);
				sqlQueryBuilder.append(query.sql);
				args.addAll(Arrays.asList(query.args));
			}
		}
		sqlQueryBuilder.append(")");

		HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
		preparedQuery.sql = sqlQueryBuilder.toString();
		preparedQuery.args = args.toArray();
		return preparedQuery;
	}


	/**
	 * Execute a SQL query to get collection or data object paths.
	 *
	 * @param preparedQuery The prepared query to execute.
	 * @return A list of paths.
	 * @throws HpcException on database error.
	 */
	private List<String> getPaths(HpcPreparedQuery preparedQuery) throws HpcException {
		try {
			return jdbcTemplate.query(preparedQuery.sql, objectPathRowMapper, preparedQuery.args);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection/data-object Paths: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	/**
	 * Execute a SQL query to get data object detailed paths.
	 *
	 * @param preparedQuery The prepared query to execute.
	 * @return A list of paths.
	 * @throws HpcException on database error.
	 */
	private List<HpcSearchMetadataEntry> getDetailedPaths(HpcPreparedQuery preparedQuery) throws HpcException {
		try {
			return jdbcTemplate.query(preparedQuery.sql, searchMetadataEntryRowMapper, preparedQuery.args);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data-object Detailed Paths: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	/**
	 * Execute a SQL query to get data object detailed paths for external use.
	 *
	 * @param preparedQuery The prepared query to execute.
	 * @return A list of paths.
	 * @throws HpcException on database error.
	 */
	private List<HpcSearchMetadataEntry> getExtDetailedPaths(HpcPreparedQuery preparedQuery) throws HpcException {
		try {
			return jdbcTemplate.query(preparedQuery.sql, searchExtMetadataEntryRowMapper, preparedQuery.args);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data-object Detailed Paths: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	/**
	 * Execute a SQL query to get collection detailed paths.
	 *
	 * @param preparedQuery The prepared query to execute.
	 * @return A list of paths.
	 * @throws HpcException on database error.
	 */
	private List<HpcSearchMetadataEntryForCollection> getDetailedPathsForCollection(HpcPreparedQuery preparedQuery)
			throws HpcException {
		try {
			return jdbcTemplate.query(preparedQuery.sql, searchMetadataEntryForCollRowMapper, preparedQuery.args);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection Detailed Paths: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	/**
	 * Execute a SQL query to get collection or data object count.
	 *
	 * @param preparedQuery The prepared query to execute.
	 * @return The count
	 * @throws HpcException on database error.
	 */
	private int getCount(HpcPreparedQuery preparedQuery) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(preparedQuery.sql, Integer.class, preparedQuery.args);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count collection/data-object: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	/**
	 * Converts a query operator enum to SQL
	 *
	 * @param operator The operator to convert.
	 * @return A SQL operator string.
	 */
	private String toSQLOperator(HpcCompoundMetadataQueryOperator operator) {
		return operator.equals(HpcCompoundMetadataQueryOperator.AND) ? "and" : "or";
	}
	
	/**
	 * Converts a query operator enum to Set operators
	 *
	 * @param operator The operator to convert.
	 * @return A SQL operator string.
	 */
	private String toSetOperator(HpcCompoundMetadataQueryOperator operator) {
		return operator.equals(HpcCompoundMetadataQueryOperator.AND) ? "intersect" : "union";
	}

	/**
	 * Get a list of metadata attributes currently registered.
	 *
	 * @param query                  The query to invoke (for collection or data
	 *                               object metadata attributes).
	 * @param levelLabel             Filter the results by level label. (Optional).
	 * @param sqlLevelLabelFilter    The SQL filter to apply for level label
	 *                               ('where' condition).
	 * @return A list of metadata attributes for each level.
	 * @throws HpcException on database.
	 */
	private List<HpcMetadataLevelAttributes> getMetadataAttributes(String query, String levelLabel,
			String sqlLevelLabelFilter) throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(query);

		// Add level label filter if provided.
		if (levelLabel != null && !levelLabel.isEmpty()) {
			sqlQueryBuilder.append(" where");
			sqlQueryBuilder.append(sqlLevelLabelFilter);
			args.add(levelLabel);
		}

		// Add the grouping and order SQL.
		sqlQueryBuilder.append(GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL);

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), metadataLevelAttributeRowMapper, args.toArray());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get metadata attributes: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	public void init() {
		jdbcTemplate.setFetchSize(fetchSize);
	}
}
