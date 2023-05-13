--
-- hpc_catalog_metadata.sql
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
           
DROP MATERIALIZED VIEW r_catalog_meta_main;

create materialized view irods.r_catalog_meta_main as
With config_meta(meta_id, meta_namespace,meta_attr_name, meta_attr_value,meta_attr_unit, r_comment,
                 create_ts, modify_ts,object_id, meta_id_1,create_ts_1, modify_ts_1) as
         (SELECT * from r_meta_main meta_main_1
                            JOIN r_objt_metamap metamap ON cast(meta_main_1.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50)) AND
                                                           metamap.meta_id = meta_main_1.meta_id)

SELECT config."DOC",
       config."BASE_PATH",
       meta_main.object_id,
       meta_main.object_path,
       meta_main.meta_id,
       meta_main.meta_attr_name,
       meta_main.meta_attr_value,
       meta_main.meta_attr_unit
FROM r_coll_hierarchy_meta_main meta_main,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" config,
     "HPC_CATALOG_ATTRIBUTE" catalog,
     config_meta
WHERE (meta_main.object_id IN (SELECT r_coll_hierarchy_meta_main.object_id
                               FROM r_coll_hierarchy_meta_main
                               WHERE cast(r_coll_hierarchy_meta_main.meta_attr_name as varchar2(250)) = cast('access' as varchar2(50))
                                 AND (lower(cast(r_coll_hierarchy_meta_main.meta_attr_value as varchar2(2700))) = ANY
                                      (cast('controlled access' as varchar2(50)), cast('open access' as varchar(50))))
                                 AND cast(r_coll_hierarchy_meta_main.level_label as varchar2(50)) = cast('Project' as varchar2(50))
                                 AND r_coll_hierarchy_meta_main.data_level = 1))
  AND catalog.level_label = cast(meta_main.level_label as varchar2(50))
  AND catalog.meta_attr_name = cast(meta_main.meta_attr_name as varchar2(250))
  AND cast(config_meta.meta_attr_value as varchar(4000)) = config."ID"
  AND config_meta.object_id = meta_main.object_id
  AND meta_main.object_path not like '%DME_Deleted_Archive%'
ORDER BY meta_main.coll_id;

comment on column irods.r_catalog_meta_main."DOC" is 'The DOC of the catalog entry';

comment on column irods.r_catalog_meta_main."BASE_PATH" is 'The base path of the catalog entry';

comment on column irods.r_catalog_meta_main.object_id is 'Collection ID: r_coll_hierarchy_meta_main.object_id';

comment on column irods.r_catalog_meta_main.object_path is 'Collection Path: r_coll_hierarchy_meta_main.object_path';

comment on column irods.r_catalog_meta_main.meta_id is 'Metadata ID: r_coll_hierarchy_meta_main.meta_id';

comment on column irods.r_catalog_meta_main.meta_attr_name is 'Metadata attribute: r_coll_hierarchy_meta_main.meta_attr_name';

comment on column irods.r_catalog_meta_main.meta_attr_value is 'Metadata value: r_coll_hierarchy_meta_main.meta_attr_value';

comment on column irods.r_catalog_meta_main.meta_attr_unit is 'Metadata unit: r_coll_hierarchy_meta_main.meta_attr_unit';

create unique index r_catalog_meta_main_uidx1
    on irods.r_catalog_meta_main (meta_attr_value, meta_attr_name, meta_id, object_id);
