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
                 
ALTER TABLE public."HPC_DOWNLOAD_TASK_RESULT" ADD COLUMN "DESTINATION_TYPE" text;
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_TYPE" IS 
                  'The download destination type - either Globus or S3';  
                  
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "DESTINATION_TYPE" text;
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_TYPE" IS 
                  'The download destination type - either Globus or S3';  
                  
                     