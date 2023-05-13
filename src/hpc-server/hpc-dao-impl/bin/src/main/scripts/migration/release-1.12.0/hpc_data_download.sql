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
                 
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "S3_ACCOUNT_ACCESS_KEY" bytea;
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_ACCESS_KEY" IS 
                  'The S3 destination account access key';  
                  
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "S3_ACCOUNT_SECRET_KEY" bytea;                  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_SECRET_KEY" IS 
                  'The S3 destination account secret key';  
                  
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "S3_ACCOUNT_REGION" text;
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_REGION" IS 
                  'The S3 destination account region';  
                     