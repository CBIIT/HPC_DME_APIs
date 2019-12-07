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
                  
