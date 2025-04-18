--Pre deployment
--1. Create new materialized view, R_DATA_HIERARCHY_META_MAIN with the new definition but with a different name and create all indexes.

create materialized view irods.r_data_hierarchy_meta_main_mv as
select OBJECT_ID,OBJECT_PATH,COLL_ID,META_ID,DATA_LEVEL,LEVEL_LABEL,META_ATTR_NAME,META_ATTR_VALUE,META_ATTR_UNIT
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
   

--Deployment (Downtime required)
--2. DME Application shutdown.
--3. Drop and create HPC_DATA_META_MAIN with new columns and partition/sub-partition.

drop table hpc_data_meta_main purge

CREATE TABLE hpc_data_meta_main
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

--4. Populate HPC_DATA_META_MAIN from iRODS tables.

alter session enable parallel dml;

insert /*+ append */ into hpc_data_meta_main nologging
(OBJECT_ID,OBJECT_PATH,COLL_ID,META_ID,DATA_LEVEL,LEVEL_LABEL,META_ATTR_NAME,META_ATTR_VALUE,META_ATTR_UNIT,CREATE_TS)
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


--5. Create indexes on HPC_DATA_META_MAIN.

create index hpc_data_meta_main_path_idx
    on irods.hpc_data_meta_main (object_path);

--6. Drop R_DATA_HIERARCHY_META_MAIN 
drop materialized view irods.r_data_hierarchy_meta_main;

--7. Create public synonym R_DATA_HIERARCHY_META_MAIN and grant select to public pointing synonym to the new MV created in step 1.
CREATE SYNONYM r_data_hierarchy_meta_main FOR irods.r_data_hierarchy_meta_main_mv;

--8. Start DME Application.



--Post deployment

--9. Create materialized view log on HPC_DATA_META_MAIN.

CREATE MATERIALIZED VIEW LOG ON hpc_data_meta_main WITH PRIMARY KEY
INCLUDING NEW VALUES;

--10. Create FAST REFRESH materialized view R_DATA_META_MAIN on HPC_DATA_META_MAIN.

create materialized view irods.r_data_meta_main
BUILD IMMEDIATE
REFRESH FAST
ENABLE QUERY REWRITE
AS
select *
from hpc_data_meta_main;

comment on column irods.r_data_meta_main.object_id is 'Data object Hierarchy ID: r_data_main.data_id';
comment on column irods.r_data_meta_main.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';
comment on column irods.r_data_meta_main.meta_id is 'Metadata ID: r_meta_main.meta_id';
comment on column irods.r_data_meta_main.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';
comment on column irods.r_data_meta_main.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';
comment on column irods.r_data_meta_main.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';
comment on column irods.r_data_meta_main.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';
comment on column irods.r_data_meta_main.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';


--11. Create indexes on R_DATA_META_MAIN.
 
create unique index r_data_meta_main_unique
    on irods.r_data_meta_main (object_id, meta_id, data_level);
create index r_data_meta_main_id_query
    on irods.r_data_meta_main (object_id);
create index r_data_meta_main_metadata_query_level
    on irods.r_data_meta_main (meta_attr_name, meta_attr_value, data_level);   
create index r_data_meta_main_metadata_query_level_label
    on irods.r_data_meta_main (meta_attr_name, level_label);
create index r_data_meta_main_metadata_query_level_lower
    on irods.r_data_meta_main (meta_attr_name, lower(meta_attr_value), data_level);
create index r_data_meta_main_path_query
    on irods.r_data_meta_main (object_path);

--12. Replace procedure refresh_daily_materialized_view to include fast refresh of R_DATA_META_MAIN.

create or replace PROCEDURE refresh_daily_materialized_view AS

BEGIN

    EXECUTE IMMEDIATE 'alter materialized view irods.r_data_hierarchy_meta_main_mv compile';
    
    DBMS_MVIEW.REFRESH('R_DATA_META_MAIN',
                        METHOD => 'F', ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE);

	DBMS_MVIEW.REFRESH('R_DATA_HIERARCHY_META_MAIN_MV',
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
