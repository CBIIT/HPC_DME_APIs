With config_meta(meta_id, meta_namespace, meta_attr_name, meta_attr_value, meta_attr_unit, r_comment,
                 create_ts, modify_ts, object_id, meta_id_1, create_ts_1, modify_ts_1) as
         (SELECT *
          from r_meta_main meta_main_1
                   JOIN r_objt_metamap metamap ON cast(meta_main_1.meta_attr_name as varchar2(250)) =
                                                  cast('configuration_id' as varchar2 (50)) AND
                                                  metamap.meta_id = meta_main_1.meta_id)
SELECT DOC, BASE_PATH, OBJECT_PATH, DATA_OWNER, DATA_CURATOR, SUM(coll_size.TOTALSIZE) as collection_size FROM
(
SELECT config."DOC",
       config."BASE_PATH",
       meta_main.object_path,
       meta_main.meta_attr_value as data_owner,
       meta_main2.meta_attr_value as data_curator
FROM r_coll_hierarchy_meta_main meta_main,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" config,
     config_meta,
     R_OBJT_ACCESS objt_access,
     IRODS.R_COLL_HIERARCHY_META_MAIN meta_main2
WHERE (meta_main.object_id IN (SELECT r_coll_hierarchy_meta_main.object_id
                               FROM r_coll_hierarchy_meta_main
                               WHERE cast(r_coll_hierarchy_meta_main.level_label as varchar2(50)) like
                                     cast('PI%' as varchar2(50))
                                 AND r_coll_hierarchy_meta_main.data_level = 1))
  AND cast(config_meta.meta_attr_value as varchar(4000)) = config."ID"
  AND config_meta.object_id = meta_main.object_id
  AND meta_main.META_ATTR_NAME in ('pi_name', 'data_owner')
  AND objt_access.OBJECT_ID = meta_main.object_id
  AND config."BASE_PATH" not in ('/TEST_Archive','/TEST_NO_HIER_Archive','/DME_Deleted_Archive')
  AND meta_main2.OBJECT_ID(+)=meta_main.OBJECT_ID
  AND meta_main2.META_ATTR_NAME(+)='data_curator'
GROUP BY config."DOC",
         config."BASE_PATH",
         meta_main.object_path,
         meta_main.meta_attr_value,
       meta_main2.meta_attr_value
), IRODS.R_REPORT_COLLECTION_SIZE coll_size
where coll_size.COLL_NAME(+) = object_path
and object_path not like '/ncifprodZone/home/DME_Deleted_Archive%'
group by DOC, BASE_PATH, OBJECT_PATH, DATA_OWNER, DATA_CURATOR
ORDER BY DOC, BASE_PATH, OBJECT_PATH;