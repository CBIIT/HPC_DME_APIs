--
-- hpc_data_object_last_access_mv.sql
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
drop materialized view irods.hpc_data_object_last_access_mv;

create materialized view irods.hpc_data_object_last_access_mv
build immediate
refresh complete on demand
as
with eligible_objects as (
    select object_id,
           object_path,
           coll_id
    from irods.hpc_data_meta_main_mv
    where META_ATTR_NAME='data_transfer_status'
    and META_ATTR_VALUE='ARCHIVED'
    and OBJECT_PATH not like '/ncifprodZone/home/DME_Deleted_Archive/%'
),
upload_events as (
    select
           object_id,
           object_path,
           to_timestamp(nullif(trim(meta_attr_value), '') default null on conversion error, 'MM-DD-YYYY HH24:MI:SS')
           as uploaded_date
    from irods.hpc_data_meta_main_mv
    where meta_attr_name = 'data_transfer_completed'
),
archive_metadata as (
    select
           object_id,
           object_path,
           meta_attr_value as bucket
    from irods.hpc_data_meta_main_mv
    where meta_attr_name = 'archive_file_container_id'
),
size_metadata as (
    select
           object_id,
           object_path,
           meta_attr_value as data_size
    from irods.hpc_data_meta_main_mv
    where meta_attr_name = 'source_file_size'
),
s3_config as (
    select
           object_id,
           object_path,
           meta_attr_value as s3_archive_configuration_id
    from irods.hpc_data_meta_main_mv
    where meta_attr_name = 's3_archive_configuration_id'
),
configuration_events as (
    select
           m.object_id,
           c.id as configuration_id,
           c.s3_default_download_archive_configuration_id as s3_archive_configuration_id,
           c.base_path,
           c.doc
    from irods.hpc_data_meta_main_mv m
    join irods.hpc_data_management_configuration c
      on c.id = m.meta_attr_value
    where m.meta_attr_name = 'configuration_id'
),
download_task_results_all as (
    select
           id,
           user_id,
           type,
           path,
           completed,
           archive_location_file_container_id,
           'HPC_DOWNLOAD_TASK_RESULT_ARCHIVE_2016_2022' as source_table,
           1 as source_priority
    from irods.hpc_download_task_result_archive_2016_2022
    where path is not null
      and type = 'DATA_OBJECT'

    union all

    select
           id,
           user_id,
           type,
           path,
           completed,
           archive_location_file_container_id,
           'HPC_DOWNLOAD_TASK_RESULT_ARCHIVE_2023' as source_table,
           2 as source_priority
    from irods.hpc_download_task_result_archive_2023
    where path is not null
      and type = 'DATA_OBJECT'

    union all

    select
           id,
           user_id,
           type,
           path,
           completed,
           archive_location_file_container_id,
           'HPC_DOWNLOAD_TASK_RESULT_ARCHIVE_2024' as source_table,
           3 as source_priority
    from irods.hpc_download_task_result_archive_2024
    where path is not null
      and type = 'DATA_OBJECT'

    union all

    select
           id,
           user_id,
           type,
           path,
           completed,
           archive_location_file_container_id,
           'HPC_DOWNLOAD_TASK_RESULT' as source_table,
           4 as source_priority
    from irods.hpc_download_task_result
    where path is not null
      and type = 'DATA_OBJECT'
),
download_events as (
    select
           '/' || (select zone_name from r_zone_main where rownum = 1) || '/home' || path as object_path,

           max(completed) as last_downloaded_date,

           max(id) keep (
               dense_rank last order by completed, source_priority, id
           ) as last_download_task_id,

           max(user_id) keep (
               dense_rank last order by completed, source_priority, id
           ) as last_downloaded_by,

           max(archive_location_file_container_id) keep (
               dense_rank last order by completed, source_priority, id
           ) as archive_location_file_container_id,

           max(source_table) keep (
               dense_rank last order by completed, source_priority, id
           ) as last_download_source_table,

           count(*) as download_count
    from download_task_results_all
    group by path
)
select
       eo.object_id,
       substr(eo.object_path,length('/'||(select ZONE_NAME from R_ZONE_MAIN where rownum=1)||'/home')+1) as path,
       eo.coll_id,
       ce.configuration_id,
       nvl(s3.s3_archive_configuration_id, ce.s3_archive_configuration_id) as s3_archive_configuration_id,
       ce.base_path,
       am.bucket,
       ue.uploaded_date,
       de.last_downloaded_date,

       nvl(de.last_downloaded_date, ue.uploaded_date) as effective_accessed_date,

       case
           when de.last_downloaded_date is not null then 'DOWNLOAD'
           when ue.uploaded_date is not null then 'UPLOAD'
           else 'UNKNOWN'
       end as effective_accessed_date_source,

       de.last_download_task_id,
       de.last_downloaded_by,
       de.last_download_source_table,

       sm.data_size,
       ce.doc,
       de.archive_location_file_container_id,

       nvl(de.download_count, 0) as download_count,

       case
           when de.last_downloaded_date is null then 'N'
           else 'Y'
       end as downloaded_flag
from eligible_objects eo
left join upload_events ue
       on ue.object_id = eo.object_id
left join archive_metadata am
       on am.object_id = eo.object_id
left join size_metadata sm
       on sm.object_id = eo.object_id
left join s3_config s3
       on s3.object_id = eo.object_id
left join configuration_events ce
       on ce.object_id = eo.object_id
left join download_events de
       on de.object_path = eo.object_path;

-- Comment on hpc_data_object_last_access_mv columns
comment on materialized view irods.hpc_data_object_last_access_mv is
'Search materialized view for data objects and their effective last-access date. The effective last-access date is the latest download completion date when available; otherwise it is the upload completion date from data_transfer_completed metadata.';
comment on column irods.hpc_data_object_last_access_mv.object_id is
'Data object ID from the eligible data objects materialized view; corresponds to the iRODS data object identifier.';
comment on column irods.hpc_data_object_last_access_mv.path is
'Data object path with the iRODS zone and /home prefix removed. This is the application-facing archive-relative path used for search and reporting.';
comment on column irods.hpc_data_object_last_access_mv.coll_id is
'Collection ID for the collection containing the data object.';
comment on column irods.hpc_data_object_last_access_mv.configuration_id is
'Data management configuration ID associated with the data object, derived from metadata attribute configuration_id and matched to HPC_DATA_MANAGEMENT_CONFIGURATION.ID.';
comment on column irods.hpc_data_object_last_access_mv.s3_archive_configuration_id is
'S3 archive configuration ID for the data object, derived from metadata attribute s3_archive_configuration_id.';
comment on column irods.hpc_data_object_last_access_mv.base_path is
'Base archive path from HPC_DATA_MANAGEMENT_CONFIGURATION for the data object configuration.';
comment on column irods.hpc_data_object_last_access_mv.bucket is
'Archive bucket or archive file container ID for the data object, derived from metadata attribute archive_file_container_id.';
comment on column irods.hpc_data_object_last_access_mv.uploaded_date is
'Upload completion timestamp for the data object, derived from metadata attribute data_transfer_completed. Invalid or blank metadata date values are converted to NULL.';
comment on column irods.hpc_data_object_last_access_mv.last_downloaded_date is
'Most recent download completion timestamp for the data object across the active download result table and archived download result tables.';
comment on column irods.hpc_data_object_last_access_mv.effective_accessed_date is
'Effective last-access timestamp used for search. Uses last_downloaded_date when present; otherwise falls back to uploaded_date.';
comment on column irods.hpc_data_object_last_access_mv.effective_accessed_date_source is
'Source of effective_accessed_date. DOWNLOAD means the value came from a download result; UPLOAD means the file has no download result and the value came from upload completion metadata; UNKNOWN means neither value is available.';
comment on column irods.hpc_data_object_last_access_mv.last_download_task_id is
'Download task ID for the most recent download event selected across active and archived download result tables.';
comment on column irods.hpc_data_object_last_access_mv.last_downloaded_by is
'User ID that submitted the most recent download request for the data object.';
comment on column irods.hpc_data_object_last_access_mv.last_download_source_table is
'Name of the active or archived download result table that provided the most recent download event.';
comment on column irods.hpc_data_object_last_access_mv.data_size is
'Data object size from the most recent download result record.';
comment on column irods.hpc_data_object_last_access_mv.doc is
'DOC value from from HPC_DATA_MANAGEMENT_CONFIGURATION for the data object configuration.';
comment on column irods.hpc_data_object_last_access_mv.archive_location_file_container_id is
'Archive location file container ID from the most recent download result record.';
comment on column irods.hpc_data_object_last_access_mv.download_count is
'Total number of data-object download result records found across the active and archived download result tables.';
comment on column irods.hpc_data_object_last_access_mv.downloaded_flag is
'Indicator showing whether the data object has at least one download result. Y means downloaded; N means no download result was found.';

create unique index irods.hpc_dobj_last_access_mv_uq
    on irods.hpc_data_object_last_access_mv (object_id) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_path_ix
    on irods.hpc_data_object_last_access_mv (path) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_eff_date_ix
    on irods.hpc_data_object_last_access_mv (effective_accessed_date) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_path_eff_ix
    on irods.hpc_data_object_last_access_mv
       (path, effective_accessed_date) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_source_ix
    on irods.hpc_data_object_last_access_mv
       (effective_accessed_date_source) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_doc_date_ix
    on irods.hpc_data_object_last_access_mv
       (doc, effective_accessed_date) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_config_ix
    on irods.hpc_data_object_last_access_mv (configuration_id) PARALLEL 4
    nologging;
    
create index irods.hpc_dobj_last_access_mv_s3_config_ix
    on irods.hpc_data_object_last_access_mv (s3_archive_configuration_id) PARALLEL 4
    nologging;

create index irods.hpc_dobj_last_access_mv_bucket_ix
    on irods.hpc_data_object_last_access_mv (bucket)  PARALLEL 4
    nologging;
