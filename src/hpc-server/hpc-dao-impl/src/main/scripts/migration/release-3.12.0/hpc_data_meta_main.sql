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
CREATION_YEAR AS (EXTRACT(YEAR FROM CREATE_TS)),  -- Virtual column
CREATION_MONTH AS (EXTRACT(MONTH FROM CREATE_TS)), -- Virtual column
PRIMARY KEY (ID)
)
PARTITION BY RANGE (CREATION_YEAR)
SUBPARTITION BY RANGE (CREATION_MONTH)
(
PARTITION p_2017 VALUES LESS THAN (2018)
(
   SUBPARTITION sp_jan_17 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_17 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_17 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_17 VALUES LESS THAN (5),
   SUBPARTITION sp_may_17 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_17 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_17 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_17 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_17 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_17 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_17 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_17 VALUES LESS THAN (13)
) 
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2018 VALUES LESS THAN (2019)
(
   SUBPARTITION sp_jan_18 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_18 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_18 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_18 VALUES LESS THAN (5),
   SUBPARTITION sp_may_18 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_18 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_18 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_18 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_18 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_18 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_18 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_18 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2019 VALUES LESS THAN (2020)
(
   SUBPARTITION sp_jan_19 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_19 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_19 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_19 VALUES LESS THAN (5),
   SUBPARTITION sp_may_19 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_19 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_19 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_19 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_19 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_19 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_19 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_19 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2020 VALUES LESS THAN (2021)
(
   SUBPARTITION sp_jan_20 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_20 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_20 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_20 VALUES LESS THAN (5),
   SUBPARTITION sp_may_20 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_20 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_20 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_20 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_20 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_20 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_20 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_20 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2021 VALUES LESS THAN (2022)
(
   SUBPARTITION sp_jan_21 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_21 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_21 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_21 VALUES LESS THAN (5),
   SUBPARTITION sp_may_21 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_21 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_21 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_21 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_21 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_21 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_21 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_21 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2022 VALUES LESS THAN (2023)
(
   SUBPARTITION sp_jan_22 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_22 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_22 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_22 VALUES LESS THAN (5),
   SUBPARTITION sp_may_22 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_22 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_22 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_22 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_22 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_22 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_22 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_22 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2023 VALUES LESS THAN (2024)
(
   SUBPARTITION sp_jan_23 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_23 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_23 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_23 VALUES LESS THAN (5),
   SUBPARTITION sp_may_23 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_23 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_23 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_23 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_23 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_23 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_23 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_23 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2024 VALUES LESS THAN (2025)
(
   SUBPARTITION sp_jan_24 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_24 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_24 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_24 VALUES LESS THAN (5),
   SUBPARTITION sp_may_24 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_24 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_24 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_24 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_24 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_24 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_24 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_24 VALUES LESS THAN (13)
);

ALTER TABLE hpc_data_meta_main
ADD PARTITION p_2025 VALUES LESS THAN (2026)
(
   SUBPARTITION sp_jan_25 VALUES LESS THAN (2),
   SUBPARTITION sp_feb_25 VALUES LESS THAN (3),
   SUBPARTITION sp_mar_25 VALUES LESS THAN (4),
   SUBPARTITION sp_apr_25 VALUES LESS THAN (5),
   SUBPARTITION sp_may_25 VALUES LESS THAN (6),
   SUBPARTITION sp_jun_25 VALUES LESS THAN (7),
   SUBPARTITION sp_jul_25 VALUES LESS THAN (8),
   SUBPARTITION sp_aug_25 VALUES LESS THAN (9),
   SUBPARTITION sp_sep_25 VALUES LESS THAN (10),
   SUBPARTITION sp_oct_25 VALUES LESS THAN (11),
   SUBPARTITION sp_nov_25 VALUES LESS THAN (12),
   SUBPARTITION sp_dec_25 VALUES LESS THAN (13)
);

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

-- Create indexes on hpc_data_meta_main
  
create index hpc_data_meta_main_path_idx
    on irods.hpc_data_meta_main (object_path);

-- Create materialized view logs
CREATE MATERIALIZED VIEW LOG ON hpc_data_meta_main WITH PRIMARY KEY
INCLUDING NEW VALUES;

-- Create new materialized view with hpc_data_meta_main with fast refresh
--drop materialized view irods.r_data_meta_main;
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


-- Create indexes on r_data_meta_main
  
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

-- Replace current materialized view with new definition without hpc_data_meta_main and r_coll_hierarchy_meta_main
drop materialized view irods.r_data_hierarchy_meta_main;
create materialized view irods.r_data_hierarchy_meta_main as
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

comment on column irods.r_data_hierarchy_meta_main.object_id is 'Data object Hierarchy ID: r_data_main.data_id';

comment on column irods.r_data_hierarchy_meta_main.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main.meta_id is 'Metadata ID: r_meta_main.meta_id';

comment on column irods.r_data_hierarchy_meta_main.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the data-object level';

comment on column irods.r_data_hierarchy_meta_main.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';

comment on column irods.r_data_hierarchy_meta_main.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';


-- Recreate indexes on r_data_hierarchy_meta_main
  
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

create index r_data_hierarchy_meta_main_path_query
    on irods.r_data_hierarchy_meta_main (object_path);
    
