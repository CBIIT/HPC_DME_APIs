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
    
-- HPCDATAMGM-1412 Add capability to download file to 3rd party S3 provider              
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK ADD S3_ACCOUNT_URL VARCHAR2(250);     
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK ADD S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED CHAR(1);  

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK ADD S3_ACCOUNT_URL VARCHAR2(250);     
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK ADD S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED CHAR(1);             

ALTER TABLE IRODS.HPC_DATA_OBJECT_DOWNLOAD_TASK ADD RESTORE_REQUESTED CHAR DEFAULT '0' NOT NULL;
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.RESTORE_REQUESTED IS 'Flag to indicate whether restoration was requested for this download';

ALTER TABLE IRODS.HPC_DOWNLOAD_TASK_RESULT ADD RESTORE_REQUESTED CHAR DEFAULT '0' NOT NULL;
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.RESTORE_REQUESTED IS 'Flag to indicate whether restoration was requested for this download';

