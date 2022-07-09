create materialized view R_REPORT_REGISTERED_BY_S3_ARCHIVE_CONFIGURATION (ARCHIVE, META_ATTR_NAME, META_ATTR_VALUE, OBJECT_ID, META_ID, CREATE_TS, COUNT)
	refresh force on demand
as
SELECT d."PROVIDER" || '://' || d."BUCKET" || '/' || d."OBJECT_ID",
       a.meta_attr_name,
       a.meta_attr_value,
       b.object_id,
       b.meta_id,
       c.create_ts,
       count(*)
FROM r_meta_main a,
     r_objt_metamap b,
     r_data_main c,
     "HPC_S3_ARCHIVE_CONFIGURATION" d,
     r_coll_main e
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.data_id
  AND c.coll_id = e.coll_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('s3_archive_configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = d."ID"
  AND INSTR(e.coll_name, '/DME_Deleted_Archive') = 0
GROUP BY d."PROVIDER", d."BUCKET", d."OBJECT_ID",
         a.meta_attr_name,
         a.meta_attr_value,
         b.object_id,
         b.meta_id,
         c.create_ts
/

