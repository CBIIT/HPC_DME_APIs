--
-- hpc_data_meta_main.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

alter session enable parallel dml;

create table hpc_data_meta_main parallel 4 nologging as
SELECT /*+parallel(r_data_main 4)*/
    DATA_ID                                     as object_id,
    (cast(data_coll.coll_name as varchar2(2700)) || cast('/' as varchar2(1))) ||
    cast(data_base.data_name as varchar2(2700)) as object_path,
    cast(NULL as number)                        as coll_id,
    meta.META_ID,
    1                                           as data_level,
    'DataObject'                                as level_label,
    meta.META_ATTR_NAME,
    meta.META_ATTR_VALUE,
    meta.META_ATTR_UNIT
FROM r_data_main data_base,
     r_coll_main data_coll,
     R_OBJT_METAMAP map,
     R_META_MAIN meta
where data_base.coll_id = data_coll.coll_id
  and DATA_ID = map.OBJECT_ID
  and map.META_ID = meta.META_ID;
  
comment on column irods.hpc_data_meta_main.object_id is 'Data object Hierarchy ID: r_data_main.data_id';

comment on column irods.hpc_data_meta_main.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';

comment on column irods.hpc_data_meta_main.meta_id is 'Metadata ID: r_meta_main.meta_id';

comment on column irods.hpc_data_meta_main.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';

comment on column irods.hpc_data_meta_main.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';

comment on column irods.hpc_data_meta_main.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';

comment on column irods.hpc_data_meta_main.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';

comment on column irods.hpc_data_meta_main.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';

-- Replace current materialized view with new definition using hpc_data_meta_main and r_coll_hierarchy_meta_main

drop materialized view irods.r_data_hierarchy_meta_main;
create materialized view irods.r_data_hierarchy_meta_main as
select *
from hpc_data_meta_main
union
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


comment on column irods.r_data_hierarchy_meta_main.object_id is 'Data object Hierarchy ID: r_data_main.data_id';

comment on column irods.r_data_hierarchy_meta_main.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main.meta_id is 'Metadata ID: r_meta_main.meta_id';

comment on column irods.r_data_hierarchy_meta_main.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';

comment on column irods.r_data_hierarchy_meta_main.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';


-- Recreate r_data_hierarchy_user_meta_main
    
create materialized view irods.R_DATA_HIERARCHY_USER_META_MAIN as
select * from R_DATA_HIERARCHY_META_MAIN where META_ATTR_NAME not in
('collection_type',
'uuid',
'dme_data_id',
'registered_by',
'registered_by_name',
'configuration_id',
's3_archive_configuration_id',
'source_file_id',
'archive_file_id',
'data_transfer_request_id',
'data_transfer_method',
'data_transfer_type',
'data_transfer_started',
'data_transfer_completed',
'source_file_url',
'source_file_nih_owner',
'source_file_owner',
'source_file_user_dn',
'source_file_nih_user_dn',
'source_file_nih_group',
'source_file_group',
'source_file_group_dn',
'source_file_nih_group_dn',
'source_file_permissions',
'archive_caller_object_id',
'checksum',
'metadata_updated',
'link_source_path',
'deep_archive_status',
'deep_archive_date',
'deleted_date');

-- Recreate indexes on r_data_hierarchy_meta_main and r_data_hierarchy_user_meta_main
  
create unique index r_data_hierarchy_meta_main_unique
    on irods.r_data_hierarchy_meta_main (object_id, meta_id, data_level);

create index r_data_hierarchy_meta_main_id_query
    on irods.r_data_hierarchy_meta_main (object_id);

create index r_data_hierarchy_meta_main_metadata_query_level
    on irods.r_data_hierarchy_meta_main (meta_attr_name, meta_attr_value, data_level);

create index r_data_hierarchy_meta_main_metadata_query_level_label
    on irods.r_data_hierarchy_meta_main (meta_attr_name, level_label);

create index r_data_hierarchy_meta_main_metadata_query_level_lower
    on irods.r_data_hierarchy_meta_main (meta_attr_name, lower(meta_attr_value), data_level);

create unique index r_data_hierarchy_user_meta_main_unique
    on irods.r_data_hierarchy_user_meta_main (object_id, meta_id, data_level);

create index r_data_hierarchy_user_meta_main_id_query
    on irods.r_data_hierarchy_user_meta_main (object_id);

create index r_data_hierarchy_user_meta_main_metadata_query_level
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, meta_attr_value, data_level);

create index r_data_hierarchy_user_meta_main_metadata_query_level_label
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, level_label);

-- Recreating these indexes at the end as it takes the longest.
create index r_data_hierarchy_meta_main_path_query
    on irods.r_data_hierarchy_meta_main (object_path);

create index r_data_hierarchy_user_meta_main_path_query
    on irods.r_data_hierarchy_user_meta_main (object_path);
