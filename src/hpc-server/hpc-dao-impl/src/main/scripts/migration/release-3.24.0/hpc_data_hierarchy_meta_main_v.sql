alter session enable parallel dml;


--1. Drop current MV.

drop materialized view irods.r_data_hierarchy_meta_main;
 

--2. Recreate hpc_data_meta_main for fast refresh with new columns and partition/sub-partition and populate coll_id.

drop table irods.hpc_data_meta_main purge;

CREATE TABLE irods.hpc_data_meta_main
(ID 									   NUMBER GENERATED ALWAYS AS IDENTITY,
 OBJECT_ID                                 NUMBER not null,
 OBJECT_PATH                               VARCHAR2(2700),
 COLL_ID                                   NUMBER,
 META_ID                                   NUMBER not null,
 DATA_LEVEL                                NUMBER,
 LEVEL_LABEL                               CHAR(10),
 META_ATTR_NAME                            VARCHAR2(2700) not null,
 META_ATTR_VALUE                           VARCHAR2(2700) not null,
 META_ATTR_UNIT                            VARCHAR2(250),
CREATE_TS                                  DATE DEFAULT SYSDATE NOT NULL,
PRIMARY KEY (ID)
)
PARTITION BY RANGE (CREATE_TS) interval(numtoyminterval(1,'YEAR')) 
(
partition p_2017 values less than (DATE '2018-01-01'),
partition p_2018 values less than (DATE '2019-01-01'),
partition p_2019 values less than (DATE '2020-01-01'),
partition p_2020 values less than (DATE '2021-01-01'),
partition p_2021 values less than (DATE '2022-01-01'),
partition p_2022 values less than (DATE '2023-01-01'),
partition p_2023 values less than (DATE '2024-01-01'),
partition p_2024 values less than (DATE '2025-01-01'),
partition p_2025 values less than (DATE '2026-01-01'),
partition p_2026 values less than (DATE '2027-01-01'),
partition p_2027 values less than (DATE '2028-01-01'),
partition p_2028 values less than (DATE '2029-01-01'),
partition p_2029 values less than (DATE '2030-01-01')
);

--3. Populate HPC_DATA_META_MAIN from iRODS tables.

insert /*+ append */ into hpc_data_meta_main nologging
(OBJECT_ID,OBJECT_PATH,COLL_ID,META_ID,DATA_LEVEL,LEVEL_LABEL,META_ATTR_NAME,META_ATTR_VALUE,META_ATTR_UNIT,CREATE_TS)
SELECT /*+parallel(r_data_main 4)*/
    DATA_ID                                     as object_id,
    data_coll.coll_name || '/' ||
    data_base.data_name                         as object_path,
    data_base.coll_id                           as coll_id,
    meta.META_ID,
    1                                           as data_level,
    'DataObject'                                as level_label,
    meta.META_ATTR_NAME,
    meta.META_ATTR_VALUE,
    meta.META_ATTR_UNIT,
    cast(timestamp '1970-01-01 00:00:00' + numtodsinterval (map.create_ts, 'second') as date) as create_ts
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


--4. Create indexes on HPC_DATA_META_MAIN.

create index hpc_data_meta_main_path_idx
    on irods.hpc_data_meta_main (object_path) nologging;

--5. Create materialized view log on HPC_DATA_META_MAIN.

CREATE MATERIALIZED VIEW LOG ON hpc_data_meta_main WITH PRIMARY KEY
INCLUDING NEW VALUES;

--6. Create FAST REFRESH materialized view HPC_DATA_META_MAIN_MV on HPC_DATA_META_MAIN.

create materialized view irods.hpc_data_meta_main_mv
BUILD IMMEDIATE
REFRESH FAST
ENABLE QUERY REWRITE
AS
select *
from hpc_data_meta_main;

comment on column irods.hpc_data_meta_main_mv.object_id is 'Data object Hierarchy ID: r_data_main.data_id';
comment on column irods.hpc_data_meta_main_mv.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';
comment on column irods.hpc_data_meta_main_mv.meta_id is 'Metadata ID: r_meta_main.meta_id';
comment on column irods.hpc_data_meta_main_mv.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';
comment on column irods.hpc_data_meta_main_mv.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';
comment on column irods.hpc_data_meta_main_mv.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';
comment on column irods.hpc_data_meta_main_mv.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';
comment on column irods.hpc_data_meta_main_mv.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';


--7.1 Create indexes on hpc_data_meta_main_mv.
 
create unique index hpc_data_meta_main_mv_unique
    on irods.hpc_data_meta_main_mv (object_id, meta_id, data_level) nologging;
create index hpc_data_meta_main_mv_metadata_query_level
    on irods.hpc_data_meta_main_mv (meta_attr_name, meta_attr_value, data_level) nologging; 
create index hpc_data_meta_main_mv_metadata_query_level_label
    on irods.hpc_data_meta_main_mv (meta_attr_name, level_label) nologging;
create index hpc_data_meta_main_mv_metadata_query_level_lower
    on irods.hpc_data_meta_main_mv (meta_attr_name, lower(meta_attr_value), data_level) nologging;
create index hpc_data_meta_main_mv_path_query
    on irods.hpc_data_meta_main_mv (object_path) nologging;
create index hpc_data_meta_main_mv_coll_id_query
    on irods.hpc_data_meta_main_mv (coll_id) nologging;
create index hpc_data_meta_main_mv_id_query
    on irods.hpc_data_meta_main_mv (object_id) nologging;
    
--7.2 Create FAST REFRESH materialized view hpc_eligible_data_objects_mv on HPC_DATA_META_MAIN_MV

create materialized view irods.hpc_eligible_data_objects_mv
build immediate
refresh fast
as
select id,
  object_id,
  object_path,
  coll_id
from irods.hpc_data_meta_main
where meta_attr_name = 'data_transfer_status'
  and coll_id is not null;

create unique index irods.hpc_eligible_data_objects_mv_uq
  on irods.hpc_eligible_data_objects_mv (object_id) nologging;

create index irods.hpc_eligible_data_objects_mv_coll_ix
  on irods.hpc_eligible_data_objects_mv (coll_id) nologging;

--7.3 Create materialized view hpc_data_inherited_meta_main_mv

create materialized view irods.hpc_data_inherited_meta_main_mv
    refresh complete on demand
as
select
       d.object_id as object_id,
       d.object_path as object_path,
       d.coll_id as coll_id,
       c.meta_id as meta_id,
       c.data_level + 1 as data_level,
       c.level_label as level_label,
       c.meta_attr_name as meta_attr_name,
       c.meta_attr_value as meta_attr_value,
       c.meta_attr_unit as meta_attr_unit
from hpc_eligible_data_objects_mv d
join r_coll_hierarchy_meta_main c
  on c.object_id = d.coll_id;

create unique index hpc_data_inherited_meta_main_mv_unique
    on irods.hpc_data_inherited_meta_main_mv (object_id, meta_id, data_level) PARALLEL 4 nologging;
create index hpc_data_inherited_meta_main_mv_metadata_query_level
    on irods.hpc_data_inherited_meta_main_mv (meta_attr_name, meta_attr_value, data_level) PARALLEL 4 nologging;
create index hpc_data_inherited_meta_main_mv_metadata_query_level_label
    on irods.hpc_data_inherited_meta_main_mv (meta_attr_name, level_label) PARALLEL 4 nologging;
create index hpc_data_inherited_meta_main_mv_metadata_query_level_lower
    on irods.hpc_data_inherited_meta_main_mv (meta_attr_name, lower(meta_attr_value), data_level) PARALLEL 4 nologging;
create index hpc_data_inherited_meta_main_mv_path_query
    on irods.hpc_data_inherited_meta_main_mv (object_path) PARALLEL 4 nologging;
create index hpc_data_inherited_meta_main_mv_id_query
    on irods.hpc_data_inherited_meta_main_mv(object_id) PARALLEL 4 nologging;

--8. Create view hpc_data_hierarchy_meta_main_v

create or replace view irods.hpc_data_hierarchy_meta_main_v as
select object_id,
  object_path,
  coll_id,
  meta_id,
  data_level,
  level_label,
  meta_attr_name,
  meta_attr_value,
  meta_attr_unit
from irods.hpc_data_meta_main_mv
union all
select object_id,
  object_path,
  coll_id,
  meta_id,
  data_level,
  level_label,
  meta_attr_name,
  meta_attr_value,
  meta_attr_unit
from irods.hpc_data_inherited_meta_main_mv;


--9. Create public synonym R_DATA_HIERARCHY_META_MAIN and grant select to public pointing synonym to the new VIEW created.
CREATE OR REPLACE SYNONYM r_data_hierarchy_meta_main FOR irods.hpc_data_hierarchy_meta_main_v;

--10. Replace procedure refresh_daily_materialized_view to include fast refresh of HPC_DATA_META_MAIN_MV, HPC_ELIGIBLE_DATA_OBJECTS_MV and complete refresh of hpc_data_inherited_meta_main_mv.

create or replace PROCEDURE refresh_daily_materialized_view AS

BEGIN

    EXECUTE IMMEDIATE 'alter materialized view irods.hpc_data_inherited_meta_main_mv compile';
    
    DBMS_MVIEW.REFRESH('HPC_DATA_META_MAIN_MV, HPC_ELIGIBLE_DATA_OBJECTS_MV',
                        METHOD => 'F', ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE);

    DBMS_MVIEW.REFRESH('HPC_DATA_INHERITED_META_MAIN_MV',
                        METHOD => 'C', ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE, PARALLELISM => 4);

	DBMS_MVIEW.REFRESH('R_DATA_HIERARCHY_USER_META_MAIN,
                        R_COLL_HIERARCHY_DATA_OWNER,
                        R_REPORT_COLL_REGISTERED_BY,
                        R_REPORT_COLL_REGISTERED_BY_BASEPATH,
                        R_REPORT_COLL_REGISTERED_BY_DOC,
                        R_REPORT_COLL_REGISTERED_BY_PATH,
                        R_REPORT_COLLECTION_TYPE,
                        R_REPORT_DATA_OBJECTS,
                        R_REPORT_REGISTERED_BY,
                        R_REPORT_REGISTERED_BY_BASEPATH,
                        R_REPORT_REGISTERED_BY_DOC,
                        R_REPORT_REGISTERED_BY_S3_ARCHIVE_CONFIGURATION,
                        R_DATA_META_ATTRIBUTES,
                        R_COLL_META_ATTRIBUTES',
                        METHOD => 'C', ATOMIC_REFRESH => FALSE, PARALLELISM => 4);

END;
/

--11. Gather statistics
begin
   dbms_stats.gather_table_stats('IRODS', 'HPC_DATA_INHERITED_META_MAIN_MV', cascade => TRUE, method_opt=>'FOR ALL INDEXED COLUMNS');
end;
/

--12. Recreate R_DATA_HIERARCHY_USER_META_MAIN
drop materialized view irods.R_DATA_HIERARCHY_USER_META_MAIN;

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

create unique index r_data_hierarchy_user_meta_main_unique
    on irods.r_data_hierarchy_user_meta_main (object_id, meta_id, data_level);

create index r_data_hierarchy_user_meta_main_path_query
    on irods.r_data_hierarchy_user_meta_main (object_path);

create index r_data_hierarchy_user_meta_main_id_query
    on irods.r_data_hierarchy_user_meta_main (object_id);

create index r_data_hierarchy_user_meta_main_metadata_query_level
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, meta_attr_value, data_level);

create index r_data_hierarchy_user_meta_main_metadata_query_level_label
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, level_label);
      
