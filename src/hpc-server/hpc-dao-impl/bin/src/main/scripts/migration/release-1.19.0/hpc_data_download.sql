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

ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "S3_ARCHIVE_CONFIGURATION_ID" text;                  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ARCHIVE_CONFIGURATION_ID" IS 
                  'The S3 Archive configuration ID to use if downloading from S3 Archive';
                  
