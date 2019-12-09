--
-- hpc_data_download.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--
                  
ALTER TABLE public."HPC_DOWNLOAD_TASK_RESULT" ADD COLUMN "SIZE" bigint;                  
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."SIZE" IS 'The data object size';  

ALTER TABLE public."HPC_DOWNLOAD_TASK_RESULT" ADD COLUMN "DESTINATION_LOCATION_FILE_CONTAINER_NAME" text;                  
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_CONTAINER_NAME" IS 'The download destination container name';    
      
-- HPCDATAMGM-1188 Populate file size column for retrospective data in Download Task Table.
update "HPC_DOWNLOAD_TASK_RESULT" as task
set "SIZE" = CAST(meta.meta_attr_value AS BIGINT)
from r_data_main data, r_objt_metamap map, r_meta_main meta
where task."TYPE" = 'DATA_OBJECT' and task."SIZE" is null
      and substring(data.data_path from length('/var/lib/irods/iRODS/Vault/home') + 1) = task."PATH"
      and map.object_id = data.data_id
      and meta.meta_id = map.meta_id and meta.meta_attr_name = 'source_file_size';