drop materialized view R_REPORT_REGISTERED_BY_S3_ARCHIVE_CONFIGURATION
create materialized view R_REPORT_REGISTERED_BY_S3_ARCHIVE_CONFIGURATION (S3_ARCHIVE_PROVIDER, S3_ARCHIVE_BUCKET, S3_ARCHIVE_CONFIGURATION_ID, OBJECT_ID)
	refresh force on demand
as
SELECT d."PROVIDER",
       d."BUCKET",
       a.meta_attr_value as S3_ARCHIVE_CONFIGURATION_ID,
       b.object_id
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
UNION

SELECT d."PROVIDER",
       d."BUCKET",
       f."S3_DEFAULT_DOWNLOAD_ARCHIVE_CONFIGURATION_ID" as S3_ARCHIVE_CONFIGURATION_ID,
       b.object_id
FROM r_meta_main a,
     r_objt_metamap b,
     r_data_main c,
     "HPC_S3_ARCHIVE_CONFIGURATION" d,
     r_coll_main e,
     "HPC_DATA_MANAGEMENT_CONFIGURATION" f
WHERE a.meta_id = b.meta_id
  AND b.object_id = c.data_id
  AND c.coll_id = e.coll_id
  AND cast(a.meta_attr_name as varchar2(250)) = cast('configuration_id' as varchar2(50))
  AND cast(a.meta_attr_value as varchar2(2700)) = f."ID"
  AND f."S3_DEFAULT_DOWNLOAD_ARCHIVE_CONFIGURATION_ID" = d."ID"
  AND b.object_id not in (select b.object_id from r_meta_main a, r_objt_metamap b where a.meta_id = b.meta_id AND cast(a.meta_attr_name as varchar2(250)) = cast('s3_archive_configuration_id' as varchar2(50)))
  AND INSTR(e.coll_name, '/DME_Deleted_Archive') = 0
GROUP BY d."PROVIDER", d."BUCKET", d."OBJECT_ID",
         a.meta_attr_name,
         a.meta_attr_value,
         f."S3_DEFAULT_DOWNLOAD_ARCHIVE_CONFIGURATION_ID",
         b.object_id,
         b.meta_id,
         c.create_ts