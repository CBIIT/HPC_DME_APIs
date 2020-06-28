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
    
-- HPCDATAMGM-1291 Single file download to Google Drive destination.              
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "GOOGLE_DRIVE_ACCESS_TOKEN" bytea;                  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."GOOGLE_DRIVE_ACCESS_TOKEN" IS 
                  'The Google Drive Access Token';  
                  
-- HPCDATAMGM-1298 Bulk download to Google Drive destination.              
ALTER TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" ADD COLUMN "GOOGLE_DRIVE_ACCESS_TOKEN" bytea;                  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."GOOGLE_DRIVE_ACCESS_TOKEN" IS 
                  'The Google Drive Access Token';  
                  
