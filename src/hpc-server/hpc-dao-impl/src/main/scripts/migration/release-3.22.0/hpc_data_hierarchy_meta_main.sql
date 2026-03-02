--
-- HPCDATAMGM-2149.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>
-- @version $Id$
--
-- Changed UNION to UNION ALL

drop materialized view irods.r_data_hierarchy_meta_main;

create materialized view irods.r_data_hierarchy_meta_main as
select *
from hpc_data_meta_main
UNION ALL
select data.data_id                           as object_id,
       (cast(coll.OBJECT_PATH as varchar2(2700)) || cast('/' as varchar2(1))) ||
       cast(data.data_name as varchar2(2700)) as object_path,
       coll.coll_id                           as coll_id,
       coll.META_ID                           as coll_meta_id,
       coll.DATA_LEVEL + 1                    as coll_data_level,
       coll.LEVEL_LABEL                       as coll_level_label,
       coll.META_ATTR_NAME                    as coll_meta_attr_name,
       coll.META_ATTR_VALUE                   as coll_meta_attr_value,
       coll.META_ATTR_UNIT                    as coll_meta_attr_unit
from r_data_main data,
     r_coll_hierarchy_meta_main coll
where coll.object_id = data.COLL_ID;

comment on column irods.r_data_hierarchy_meta_main_mv.object_id is 'Data object Hierarchy ID: r_data_main.data_id';

comment on column irods.r_data_hierarchy_meta_main_mv.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main_mv.meta_id is 'Metadata ID: r_meta_main.meta_id';

comment on column irods.r_data_hierarchy_meta_main_mv.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';

comment on column irods.r_data_hierarchy_meta_main_mv.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main_mv.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';

comment on column irods.r_data_hierarchy_meta_main_mv.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';

comment on column irods.r_data_hierarchy_meta_main_mv.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';


create unique index r_data_hierarchy_meta_main_mv_unique
    on irods.r_data_hierarchy_meta_main_mv (object_id, meta_id, data_level);

create index r_data_hierarchy_meta_main_mv_id_query
    on irods.r_data_hierarchy_meta_main_mv (object_id);

create index r_data_hierarchy_meta_main_mv_metadata_query_level
    on irods.r_data_hierarchy_meta_main_mv (meta_attr_name, meta_attr_value, data_level);

create index r_data_hierarchy_meta_main_mv_metadata_query_level_label
    on irods.r_data_hierarchy_meta_main_mv (meta_attr_name, level_label);

create index r_data_hierarchy_meta_main_mv_metadata_query_level_lower
    on irods.r_data_hierarchy_meta_main_mv (meta_attr_name, lower(meta_attr_value), data_level);

create index r_data_hierarchy_meta_main_mv_path_query
    on irods.r_data_hierarchy_meta_main_mv (object_path);
