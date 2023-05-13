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
                 
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "PRIORITY" integer DEFAULT 100;
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PRIORITY" IS 
                  'The download task priority';
                  
ALTER TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" ADD COLUMN "PRIORITY" integer DEFAULT 100;                  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."PRIORITY" IS 
                  'The download task priority';
                  
ALTER TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" ADD COLUMN "COLLECTION_PATHS" text[];  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."COLLECTION_PATHS" IS 
                  'The list of collection paths to download';