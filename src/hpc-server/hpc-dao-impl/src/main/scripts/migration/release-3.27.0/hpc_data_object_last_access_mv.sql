create index irods.hpc_dtr_arc_2016_2022_type_path_completed_ix
    on irods.hpc_download_task_result_archive_2016_2022
       (type, path, completed) PARALLEL 4
    nologging;

create index irods.hpc_dtr_arc_2023_type_path_completed_ix
    on irods.hpc_download_task_result_archive_2023
       (type, path, completed) PARALLEL 4
    nologging;

create index irods.hpc_dtr_arc_2024_type_path_completed_ix
    on irods.hpc_download_task_result_archive_2024
       (type, path, completed) PARALLEL 4
    nologging;

create index irods.hpc_dtr_type_path_completed_ix
    on irods.hpc_download_task_result
       (type, path, completed) PARALLEL 4
    nologging;

create materialized view irods.hpc_data_object_last_access_mv
build immediate
refresh complete on demand
as
with eligible_objects as (
    select object_id,
           object_path,
           coll_id
    from irods.hpc_eligible_data_objects_mv
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
configuration_events as (
    select
           m.object_id,
           c.id as configuration_id,
           c.base_path
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
           data_size,
           doc,
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
           data_size,
           doc,
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
           data_size,
           doc,
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

           max(data_size) keep (
               dense_rank last order by completed, source_priority, id
           ) as data_size,

           max(doc) keep (
               dense_rank last order by completed, source_priority, id
           ) as doc,

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

       de.data_size,
       de.doc,
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
left join configuration_events ce
       on ce.object_id = eo.object_id
left join download_events de
       on de.object_path = eo.object_path;

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

create index irods.hpc_dobj_last_access_mv_bucket_ix
    on irods.hpc_data_object_last_access_mv (bucket)  PARALLEL 4
    nologging;

