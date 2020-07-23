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
package gov.nih.nci.hpc.dao.postgresql.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
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
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and collection.meta_attr_value = ?";

	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and collection.meta_attr_value <> ?";

	private static final String GET_COLLECTION_IDS_LIKE_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and lower(collection.meta_attr_value) like lower(?)";

	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and num_less_than(collection.meta_attr_value, ?) = true";

	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and num_less_or_equal(collection.meta_attr_value, ?) = true";

	private static final String GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and num_greater_than(collection.meta_attr_value, ?) = true";

	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and num_greater_or_equal(collection.meta_attr_value, ?) = true";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_LESS_THAN_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and timestamp_less_than(collection.meta_attr_value, ?, ?) = true";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_GREATER_THAN_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and timestamp_greater_than(collection.meta_attr_value, ?, ?) = true";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and timestamp_less_or_equal(collection.meta_attr_value, ?, ?) = true";

	private static final String GET_COLLECTION_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL = " exists(select 1 from \"r_coll_hierarchy_meta_main\" collection where collection.object_id=collection1.object_id and timestamp_greater_or_equal(collection.meta_attr_value, ?, ?) = true";

	private static final String GET_COLLECTION_EXACT_ATTRIBUTE_MATCH_FILTER = " and collection.meta_attr_name = ?";

	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and dataObject.meta_attr_value = ?";

	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and dataObject.meta_attr_value <> ?";

	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and lower(dataObject.meta_attr_value) like lower(?)";

	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and num_less_than(dataObject.meta_attr_value, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and num_less_or_equal(dataObject.meta_attr_value, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and num_greater_than(dataObject.meta_attr_value, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and num_greater_or_equal(dataObject.meta_attr_value, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_THAN_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and timestamp_less_than(dataObject.meta_attr_value, ?, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_THAN_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and timestamp_greater_than(dataObject.meta_attr_value, ?, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_LESS_OR_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and timestamp_less_or_equal(dataObject.meta_attr_value, ?, ?) = true";

	private static final String GET_DATA_OBJECT_IDS_TIMESTAMP_GREATER_OR_EQUAL_SQL = " exists(select 1 from \"r_data_hierarchy_meta_main\" dataObject where dataObject.object_id=dataObject1.object_id and timestamp_greater_or_equal(dataObject.meta_attr_value, ?, ?) = true";

	private static final String GET_DATA_OBJECT_EXACT_ATTRIBUTE_MATCH_FILTER = " and dataObject.meta_attr_name = ?";

	private static final String DATA_OBJECT_LEVEL_EQUAL_FILTER = " and dataObject.level = ?)";
	private static final String DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER = " and dataObject.level <> ?)";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER = " and dataObject.level < ?)";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and dataObject.level <= ?)";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER = " and dataObject.level > ?)";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and dataObject.level >= ?)";

	private static final String COLLECTION_LEVEL_EQUAL_FILTER = " and collection.level = ?)";
	private static final String COLLECTION_LEVEL_NOT_EQUAL_FILTER = " and collection.level <> ?)";
	private static final String COLLECTION_LEVEL_NUM_LESS_THAN_FILTER = " and collection.level < ?)";
	private static final String COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and collection.level <= ?)";
	private static final String COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER = " and collection.level > ?)";
	private static final String COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and collection.level >= ?)";

	private static final String DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER = " and dataObject.level_label = ?)";
	private static final String DATA_OBJECT_LEVEL_LABEL_NOT_EQUAL_FILTER = " and dataObject.level_label <> ?)";
	private static final String DATA_OBJECT_LEVEL_LABEL_LIKE_FILTER = " and dataObject.level_label like ?)";

	private static final String COLLECTION_LEVEL_LABEL_EQUAL_FILTER = " and collection.level_label = ?)";
	private static final String COLLECTION_LEVEL_LABEL_NOT_EQUAL_FILTER = " and collection.level_label <> ?)";
	private static final String COLLECTION_LEVEL_LABEL_LIKE_FILTER = " and collection.level_label like ?)";

	private static final String USER_ACCESS_SQL = "(select distinct access.object_id from public.\"r_objt_access\" access join public.\"r_user_main\" account "
			+ "using (user_id) where account.user_name = ? union "
			+ "select distinct access.object_id from public.\"r_objt_access\" access join "
			+ "public.\"r_user_group\" user_group on (access.user_id = user_group.group_user_id) "
			+ "where user_group.group_user_id in (select user_group.group_user_id from "
			+ "public.\"r_user_group\" user_group join public.\"r_user_main\" account using (user_id) where "
			+ "account.user_name = ?))";

	private static final String LIMIT_OFFSET_SQL = " order by object_path limit ? offset ?";

	private static final String GET_COLLECTION_PATHS_SQL = "select distinct object_path from \"r_coll_hierarchy_meta_main\" collection1 where ";

	private static final String GET_DETAILED_COLLECTION_PATHS_SQL = "select mv.object_id, coll.coll_name, mv.object_path, coll.parent_coll_name, coll.coll_owner_name, "
			+ "coll.coll_owner_zone, coll.coll_map_id, coll.coll_inheritance, coll.r_comment, "
			+ "coll.coll_info1, coll.coll_info2, coll.create_ts, coll.r_comment, coll.coll_type, "
			+ "mv.meta_attr_name, mv.meta_attr_value, mv.level, mv.level_label, mv.coll_id "
			+ "from \"r_coll_hierarchy_meta_main\" mv, public.\"r_coll_main\" coll "
			+ "where mv.object_id = coll.coll_id and mv.object_path in ";

	private static final String GET_COLLECTION_COUNT_SQL = "select count(distinct object_id) from \"r_coll_hierarchy_meta_main\" collection1 where ";

	private static final String GET_DATA_OBJECT_PATHS_SQL = "select distinct object_path from \"r_data_hierarchy_meta_main\" dataObject1 where ";

	private static final String GET_DETAILED_DATA_OBJECT_PATHS_SQL = "select mv.object_id, coll.coll_id, coll.coll_name, mv.object_path, data.data_size, "
			+ "data.data_path, data.data_owner_name, data.create_ts, mv.meta_attr_name, "
			+ "mv.meta_attr_value, mv.level, mv.level_label "
			+ "from \"r_data_hierarchy_meta_main\" mv, public.\"r_data_main\" data, public.\"r_coll_main\" coll "
			+ "where mv.object_id = data.data_id and data.coll_id = coll.coll_id and mv.object_path in ";

	private static final String GET_DATA_OBJECT_COUNT_SQL = "select count(distinct object_id) from \"r_data_hierarchy_meta_main\" dataObject1 where ";

	private static final String GET_COLLECTION_METADATA_SQL = "select meta_attr_name,  meta_attr_value, meta_attr_unit, level, level_label "
			+ "from \"r_coll_hierarchy_meta_main\" where object_path = ? and level >= ? order by level";

	private static final String GET_DATA_OBJECT_METADATA_SQL = "select meta_attr_name, meta_attr_value, meta_attr_unit, level, level_label "
			+ "from \"r_data_hierarchy_meta_main\" where object_path = ? and level >= ? order by level";

	private static final String PREPARE_REFRESH_VIEW_SQL = "select internal.prepare_hierarchy_meta_view_refresh()";

	private static final String CLEANUP_REFRESH_VIEW_SQL = "select internal.cleanup_hierarchy_meta_view_refresh()";

	private static final String REFRESH_VIEW_SQL = "select internal.refresh_hierarchy_meta_view()";
	
	private static final String GET_COLLECTION_METADATA_ATTRIBUTES_SQL = "select collection.level_label, collection.meta_attr_name "
			+ "from \"r_coll_hierarchy_meta_attr_name\" collection, unnest(collection.object_ids) as object_id "
			+ "where object_id in " + USER_ACCESS_SQL + " GROUP BY level_label, meta_attr_name";

	private static final String GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL = "select dataObject.level_label, dataObject.meta_attr_name "
			+ "from \"r_data_hierarchy_meta_attr_name\" dataObject, unnest(dataObject.object_ids) as object_id "
			+ "where object_id in " + USER_ACCESS_SQL + " GROUP BY level_label, meta_attr_name";

	private static final String GET_COLLECTION_METADATA_AGGREGATE_SQL = "with top_collection as ("
			+ GET_COLLECTION_METADATA_ATTRIBUTES_SQL
			+ ") select level_label, array_agg(distinct meta_attr_name) as attributes " + "FROM top_collection";

	private static final String GET_DATA_OBJECT_METADATA_AGGREGATE_SQL = "with top_dataObject as ("
			+ GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL
			+ ") select level_label, array_agg(distinct meta_attr_name) as attributes " + "FROM top_dataObject";

	private static final String GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL = " group by level_label order by level_label";

	private static final String GET_METADATA_MODIFIED_AT_SQL = "select max(cast(modify_ts as bigint)) from public.\"r_objt_metamap\" where object_id = ?";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mappers.
	private SingleColumnRowMapper<String> objectPathRowMapper = new SingleColumnRowMapper<>();
	private SingleColumnRowMapper<Long> objectIdRowMapper = new SingleColumnRowMapper<>();
	private RowMapper<HpcMetadataLevelAttributes> metadataLevelAttributeRowMapper = (rs, rowNum) -> {
		HpcMetadataLevelAttributes metadataLevelAttributes = new HpcMetadataLevelAttributes();
		metadataLevelAttributes.setLevelLabel(rs.getString("LEVEL_LABEL"));

		// Extract the metadata attributes for this level. Defensive coding to exclude
		// any null values.
		String[] metadataAttributes = (String[]) rs.getArray("ATTRIBUTES").getArray();
		int metadataAttributesSize = metadataAttributes.length;
		for (int i = 0; i < metadataAttributesSize; i++) {
			if (metadataAttributes[i] != null) {
				metadataLevelAttributes.getMetadataAttributes().add(metadataAttributes[i]);
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

		Long level = rs.getLong("LEVEL");
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
		searchMetadataEntry.setDataSize(dataSize != null ? dataSize.intValue() : null);
		searchMetadataEntry.setDataPath(rs.getString(6));
		searchMetadataEntry.setDataOwnerName(rs.getString(7));
		String createTs = rs.getString(8);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(Long.parseLong(createTs) * 1000);
		searchMetadataEntry.setCreatedAt(cal);
		searchMetadataEntry.setAttribute(rs.getString(9));
		searchMetadataEntry.setValue(rs.getString(10));
		Long level = rs.getLong(11);
		searchMetadataEntry.setLevel(level != null ? level.intValue() : null);
		searchMetadataEntry.setLevelLabel(rs.getString(12));

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
		Integer level = rs.getInt(17);
		searchMetadataEntry.setLevel(level != null ? level.intValue() : null);
		searchMetadataEntry.setLevelLabel(rs.getString(18));
		Long metaCollId = rs.getLong(19);
		searchMetadataEntry.setMetaCollectionId(metaCollId != null ? metaCollId.intValue() : null);

		return searchMetadataEntry;
	};
    private RowMapper<HpcCollectionListingEntry> browseMetadataRowMapper = (rs, rowNum) -> {
        HpcCollectionListingEntry metadataEntry = new HpcCollectionListingEntry();
        Integer id = rs.getInt("id");
        metadataEntry.setId(id != null ? id.intValue() : null);
        metadataEntry.setPath(rs.getString("path"));
        Long size = rs.getLong("size");
        metadataEntry.setDataSize(size != null ? size.longValue() : null);
        Calendar cal = Calendar.getInstance();
        cal.setTime(rs.getTimestamp("uploaded"));
        metadataEntry.setCreatedAt(cal);
        return metadataEntry;
    };

	// SQL Maps from operators to queries and filters.
	private HpcSQLMaps dataObjectSQL = new HpcSQLMaps();
	private HpcSQLMaps collectionSQL = new HpcSQLMaps();

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcMetadataDAOImpl() {
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
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, offset,
				limit));
	}

	@Override
	public List<HpcSearchMetadataEntryForCollection> getDetailedCollectionPaths(
			HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {

		List<HpcSearchMetadataEntryForCollection> collPaths = new ArrayList<>();
		List<String> paths = getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL,
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, offset,
				limit));

		if (CollectionUtils.isEmpty(paths))
			return collPaths;

		return getDetailedPathsForCollection(
				prepareQuery(GET_DETAILED_COLLECTION_PATHS_SQL, toQuery(paths), null, null, null));
	}

	@Override
	public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {
		return getCount(prepareQuery(GET_COLLECTION_COUNT_SQL,
				toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, null, null));
	}

	@Override
	public List<String> getDataObjectPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, path,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, offset,
				limit));
	}

	@Override
	public List<HpcSearchMetadataEntry> getDetailedDataObjectPaths(String path, 
			HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit, HpcMetadataQueryLevelFilter defaultLevelFilter)
			throws HpcException {

		List<HpcSearchMetadataEntry> dataPaths = new ArrayList<>();

		List<String> paths = getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, path,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, offset,
				limit));

		if (CollectionUtils.isEmpty(paths))
			return dataPaths;

		return getDetailedPaths(prepareQuery(GET_DETAILED_DATA_OBJECT_PATHS_SQL, toQuery(paths), null, null, null));
	}

	@Override
	public int getDataObjectCount(HpcCompoundMetadataQuery compoundMetadataQuery, String dataManagementUsername,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {
		return getCount(prepareQuery(GET_DATA_OBJECT_COUNT_SQL,
				toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter), dataManagementUsername, null, null));
	}

	@Override
	public List<HpcMetadataEntry> getCollectionMetadata(String path, int minLevel) throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_METADATA_SQL, metadataEntryRowMapper, path, minLevel);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection hierarchical metadata: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcMetadataEntry> getDataObjectMetadata(String path, int minLevel) throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_METADATA_SQL, metadataEntryRowMapper, path, minLevel);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object hierarchical metadata: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcMetadataLevelAttributes> getCollectionMetadataAttributes(String levelLabel,
			String dataManagementUsername) throws HpcException {
		return getMetadataAttributes(GET_COLLECTION_METADATA_AGGREGATE_SQL, levelLabel, dataManagementUsername,
				COLLECTION_LEVEL_LABEL_EQUAL_FILTER);
	}

	@Override
	public List<HpcMetadataLevelAttributes> getDataObjectMetadataAttributes(String levelLabel,
			String dataManagementUsername) throws HpcException {
		return getMetadataAttributes(GET_DATA_OBJECT_METADATA_AGGREGATE_SQL, levelLabel, dataManagementUsername,
				DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void refreshViews() throws HpcException {
		try {
			Connection conn = jdbcTemplate.getDataSource().getConnection();
	        try {
	        	conn.setAutoCommit(false);
	        	Statement statement = conn.createStatement();
				// In case previous refresh is unsuccessful
				conn.createStatement();
				statement.execute(CLEANUP_REFRESH_VIEW_SQL);
				conn.commit();
				// Create internal tables in preparation for the refresh
				statement.execute(PREPARE_REFRESH_VIEW_SQL);
				conn.commit();
				// Refresh the views
				statement.execute(REFRESH_VIEW_SQL);
				conn.commit();
				conn.setAutoCommit(true);
	        } finally {
	          conn.close();
	        }
		}  catch (DataAccessException e) {
			throw new HpcException("Failed to refresh materialized views: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		} catch (SQLException e) {
			throw new HpcException("Failed to refresh materialized views: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}


    @Override
    public List<HpcCollectionListingEntry> getBrowseMetadataByIds(List<Integer> ids)
      throws HpcException {
        try {
            String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
            return jdbcTemplate.query(
                String.format("SELECT * FROM r_browse_meta_main WHERE id IN (%s)", inSql), ids.toArray(),
                browseMetadataRowMapper);
        } catch (DataAccessException e) {
            throw new HpcException(
                "Failed to get browse metadata : " + e.getMessage(),
                HpcErrorType.DATABASE_ERROR,
                HpcIntegratedSystem.POSTGRESQL,
                e);
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
	
	
	
	private HpcPreparedQuery prepareQuery(String getObjectPathsQuery, HpcPreparedQuery userQuery,
			String dataManagementUsername, Integer offset, Integer limit) {
		return prepareQuery(getObjectPathsQuery, null, userQuery,
				dataManagementUsername, offset, limit);
	}

	/**
	 * Prepare a SQL query. Map operators to SQL and concatenate them with
	 * 'intersect'.
	 * 
	 * @param getObjectPathsQuery    The query to get object paths based on object
	 *                               IDs.
	 * @param path             		 The path to search in.                              
	 * @param userQuery              The calculated SQL query based on user input
	 *                               (represented by query domain objects).
	 * @param dataManagementUsername The data management user name.
	 * @param offset                 Skip that many path in the returned results.
	 * @param limit                  No more than 'limit' paths will be returned.
	 * @return A prepared query.
	 */
	private HpcPreparedQuery prepareQuery(String getObjectPathsQuery, String path, HpcPreparedQuery userQuery,
			String dataManagementUsername, Integer offset, Integer limit) {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		// Combine the metadata queries into a single SQL statement.
		sqlQueryBuilder.append(getObjectPathsQuery);
		
		//Add query to limit results to within the path if specified
		if(path != null) {
			sqlQueryBuilder.append("object_path LIKE ?");
			args.add("%" + path + "%");
		}
		
		//Add query to search for requested metadata
		if(userQuery != null) {
			if(path != null) {
				sqlQueryBuilder.append(" and ");
			}
			sqlQueryBuilder.append(userQuery.sql);
			args.addAll(Arrays.asList(userQuery.args));
		}

		// Add a query to only include entities the user can access.
		if (dataManagementUsername != null) {
			sqlQueryBuilder.append(" and object_id in ");
			sqlQueryBuilder.append(USER_ACCESS_SQL);
			args.add(dataManagementUsername);
			args.add(dataManagementUsername);
		}

		if (offset != null && limit != null) {
			sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
			args.add(limit);
			args.add(offset);
		}

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
	 * @return A prepared query.
	 * @throws HpcException on service failure.
	 */
	private HpcPreparedQuery toQuery(HpcSQLMaps sql, HpcCompoundMetadataQuery compoundMetadataQuery,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();
		
		if(compoundMetadataQuery == null) {
			return null;
		}

		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if (compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sql, compoundMetadataQuery.getQueries(),
					compoundMetadataQuery.getOperator(), defaultLevelFilter);
			sqlQueryBuilder.append(query.sql);
			args.addAll(Arrays.asList(query.args));
		}

		// Append the nested compound queries.
		if (compoundMetadataQuery.getCompoundQueries() != null
				&& !compoundMetadataQuery.getCompoundQueries().isEmpty()) {
			if (!args.isEmpty()) {
				sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
			}
			boolean firstNestedQuery = true;
			for (HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
				if (!firstNestedQuery) {
					sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
				} else {
					firstNestedQuery = false;
				}
				HpcPreparedQuery query = toQuery(sql, nestedCompoundQuery, defaultLevelFilter);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
	 * Get a list of metadata attributes currently registered.
	 *
	 * @param query                  The query to invoke (for collection or data
	 *                               object metadata attributes).
	 * @param levelLabel             Filter the results by level label. (Optional).
	 * @param dataManagementUsername The Data Management user name.
	 * @param sqlLevelLabelFilter    The SQL filter to apply for level label
	 *                               ('where' condition).
	 * @return A list of metadata attributes for each level.
	 * @throws HpcException on database.
	 */
	private List<HpcMetadataLevelAttributes> getMetadataAttributes(String query, String levelLabel,
			String dataManagementUsername, String sqlLevelLabelFilter) throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();
		args.add(dataManagementUsername);
		args.add(dataManagementUsername);

		sqlQueryBuilder.append(query);

		// Add level label filter if provided.
		if (levelLabel != null && !levelLabel.isEmpty()) {
			sqlQueryBuilder.append(sqlLevelLabelFilter);
			args.add(levelLabel);
		}

		// Add the grouping and order SQL.
		sqlQueryBuilder.append(GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL);

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), metadataLevelAttributeRowMapper, args.toArray());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get metadata attributes: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
}
