--
-- hpc_report_views.sql
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

DROP MATERIALIZED VIEW r_report_coll_registered_by;
create materialized view irods.r_report_coll_registered_by as
SELECT a.meta_attr_name,
       a.meta_attr_value,
       c.coll_id,
       b.meta_id,
       c.create_ts
FROM r_meta_main a,
     r_objt_metamap b,
     r_coll_main c
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.coll_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('registered_by' as varchar2(50))
  AND INSTR(c.coll_name, '/DME_Deleted_Archive') = 0
GROUP BY a.meta_attr_name,
         a.meta_attr_value,
         c.coll_id,
         b.meta_id,
         c.create_ts;

create unique index r_report_coll_registered_by_uidx1
    on irods.r_report_coll_registered_by (meta_attr_name, meta_attr_value, coll_id);



DROP MATERIALIZED VIEW r_report_coll_registered_by_basepath;
create materialized view irods.r_report_coll_registered_by_basepath as
SELECT d."BASE_PATH",
       a.meta_attr_name,
       a.meta_attr_value,
       c.coll_id,
       b.meta_id,
       c.create_ts,
       count(*)
FROM r_meta_main a,
     r_objt_metamap b,
     r_coll_main c,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" d
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.coll_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = d."ID"
  AND INSTR(c.coll_name, '/DME_Deleted_Archive') = 0
GROUP BY d."BASE_PATH",
         a.meta_attr_name,
         a.meta_attr_value,
         c.coll_id,
         b.meta_id,
         c.create_ts;

create unique index r_report_coll_registered_by_basepath_uidx1
    on irods.r_report_coll_registered_by_basepath (meta_attr_name, meta_attr_value, coll_id, "BASE_PATH");



DROP MATERIALIZED VIEW r_report_coll_registered_by_doc;
create materialized view irods.r_report_coll_registered_by_doc as
SELECT d."DOC",
       a.meta_attr_name,
       a.meta_attr_value,
       c.coll_id,
       b.meta_id,
       c.create_ts,
       count(*)
FROM r_meta_main a,
     r_objt_metamap b,
     r_coll_main c,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" d
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.coll_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = d."ID"
  AND INSTR(c.coll_name, '/DME_Deleted_Archive') = 0
GROUP BY d."DOC",
         a.meta_attr_name,
         a.meta_attr_value,
         c.coll_id,
         b.meta_id,
         c.create_ts;

create unique index r_report_coll_registered_by_doc_uidx1
    on irods.r_report_coll_registered_by_doc (meta_attr_name, meta_attr_value, coll_id, "DOC");

DROP MATERIALIZED VIEW r_report_data_objects;
create materialized view irods.r_report_data_objects as
SELECT a.rowid arowid,
       b.rowid browid,
       c.rowid crowid,
       a.data_id,
       c.meta_attr_name,
       c.meta_attr_value,
       a.create_ts
FROM r_data_main a,
     r_objt_metamap b,
     r_meta_main c
WHERE
        a.data_id = b.object_id and b.meta_id = c.meta_id
        AND INSTR(a.data_path, '/DME_Deleted_Archive') = 0;

create unique index r_report_data_objects_uidx1
    on irods.r_report_data_objects (meta_attr_name, meta_attr_value, data_id);

DROP MATERIALIZED VIEW r_report_registered_by;
create materialized view irods.r_report_registered_by as
SELECT  a.meta_attr_name,
        a.meta_attr_value,
        b.object_id,
        b.meta_id,
        c.create_ts,
        count(*)
FROM r_meta_main a,
     r_objt_metamap b,
     r_data_main c
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.data_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('registered_by' as varchar2(50))
  AND INSTR(c.data_path, '/DME_Deleted_Archive') = 0
group by  a.meta_attr_name,
          a.meta_attr_value,
          b.object_id,
          b.meta_id,
          c.create_ts;

create unique index r_report_registered_by_uidx1
    on irods.r_report_registered_by (meta_attr_name, meta_attr_value, object_id);



DROP MATERIALIZED VIEW r_report_registered_by_basepath;
create materialized view irods.r_report_registered_by_basepath as
SELECT d."BASE_PATH",
       a.meta_attr_name,
       a.meta_attr_value,
       b.object_id,
       b.meta_id,
       c.create_ts
FROM r_meta_main a,
     r_objt_metamap b,
     r_data_main c,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" d
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.data_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = d."ID"
  AND INSTR(c.data_path, '/DME_Deleted_Archive') = 0
GROUP BY d."BASE_PATH",
         a.meta_attr_name,
         a.meta_attr_value,
         b.object_id,
         b.meta_id,
         c.create_ts;

create unique index r_report_registered_by_basepath_uidx1
    on irods.r_report_registered_by_basepath (meta_attr_name, meta_attr_value, object_id, "BASE_PATH");



DROP MATERIALIZED VIEW r_report_registered_by_doc;
create materialized view irods.r_report_registered_by_doc as
SELECT d."DOC",
       a.meta_attr_name,
       a.meta_attr_value,
       b.object_id,
       b.meta_id,
       c.create_ts,
       count(*)
FROM r_meta_main a,
     r_objt_metamap b,
     r_data_main c,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" d
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.data_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = d."ID"
  AND INSTR(c.data_path, '/DME_Deleted_Archive') = 0
GROUP BY d."DOC",
         a.meta_attr_name,
         a.meta_attr_value,
         b.object_id,
         b.meta_id,
         c.create_ts;

create unique index r_report_registered_by_doc_uidx1
    on irods.r_report_registered_by_doc (meta_attr_name, meta_attr_value, object_id, "DOC");


