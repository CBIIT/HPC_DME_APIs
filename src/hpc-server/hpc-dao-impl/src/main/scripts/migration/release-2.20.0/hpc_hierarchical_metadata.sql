--
-- hpc_hierarchical_metadata.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--

DROP MATERIALIZED VIEW r_coll_hierarchy_data_owner;

create materialized view irods.r_coll_hierarchy_data_owner as
With config_meta(meta_id, meta_namespace, meta_attr_name, meta_attr_value, meta_attr_unit, r_comment,
                 create_ts, modify_ts, object_id, meta_id_1, create_ts_1, modify_ts_1) as
         (SELECT *
          from r_meta_main meta_main_1
                   JOIN r_objt_metamap metamap ON cast(meta_main_1.meta_attr_name as varchar2(250)) =
                                                  cast('configuration_id' as varchar2(50)) AND
                                                  metamap.meta_id = meta_main_1.meta_id)

SELECT config."DOC",
       config."BASE_PATH",
       meta_main.object_path,
       meta_main.meta_attr_value  as data_owner,
       meta_main2.meta_attr_value as data_curator
FROM r_coll_hierarchy_meta_main meta_main,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" config,
     config_meta,
     R_OBJT_ACCESS objt_access,
     IRODS.R_COLL_HIERARCHY_META_MAIN meta_main2
WHERE (meta_main.object_id IN (SELECT r_coll_hierarchy_meta_main.object_id
                               FROM r_coll_hierarchy_meta_main
                               WHERE (r_coll_hierarchy_meta_main.level_label like 'PI%'
                                   or r_coll_hierarchy_meta_main.level_label = 'Domain')
                                 AND r_coll_hierarchy_meta_main.data_level = 1))
  AND config_meta.meta_attr_value = config."ID"
  AND config_meta.object_id = meta_main.object_id
  AND meta_main.META_ATTR_NAME in ('pi_name', 'data_owner')
  AND objt_access.OBJECT_ID = meta_main.object_id
  AND config."BASE_PATH" not in ('/TEST_Archive', '/TEST_NO_HIER_Archive', '/DME_Deleted_Archive')
  AND meta_main2.OBJECT_ID(+) = meta_main.OBJECT_ID
  AND meta_main2.META_ATTR_NAME(+) = 'data_curator'
  AND meta_main.OBJECT_PATH not like '/ncifprodZone/home/DME_Deleted_Archive%'
GROUP BY config."DOC",
         config."BASE_PATH",
         meta_main.object_path,
         meta_main.meta_attr_value,
         meta_main2.meta_attr_value;

comment on column irods.r_coll_hierarchy_data_owner."DOC" is 'The DOC of the catalog entry';

comment on column irods.r_coll_hierarchy_data_owner.base_path is 'Collection Hierarchy ID: r_coll_main.coll_id';

comment on column irods.r_coll_hierarchy_data_owner.object_path is 'Collection Path: r_coll_hierarchy_meta_main.object_path';

comment on column irods.r_coll_hierarchy_data_owner.data_owner is 'Metadata value: r_coll_hierarchy_meta_main.meta_attr_value for meta_attr_name data_owner';

comment on column irods.r_coll_hierarchy_data_owner.data_curator is 'Metadata ID: r_meta_main.meta_id for meta_attr_name data_curator';

create index r_coll_hierarchy_data_owner_path_query
    on irods.r_coll_hierarchy_data_owner (object_path);
    
    
DROP PROCEDURE refresh_hierarchy_meta_view;

CREATE PROCEDURE refresh_hierarchy_meta_view AS

BEGIN

        DBMS_MVIEW.REFRESH('R_COLL_HIERARCHY_METAMAP,
                        R_COLL_HIERARCHY_META_MAIN,
                        R_CATALOG_META_MAIN,
                        R_DATA_HIERARCHY_METAMAP,
                        R_DATA_HIERARCHY_META_MAIN,
                        R_DATA_HIERARCHY_USER_META_MAIN,
                        R_COLL_HIERARCHY_DATA_OWNER',
                        METHOD => 'C',  ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE);


END;