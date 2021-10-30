--
-- hpc_data_object_registration_google_access_token.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

-- HPC_DATA_OBJECT_DOWNLOAD_TASK
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (COLLECTION_DOWNLOAD_TASK_ID VARCHAR2(50)); 
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.COLLECTION_DOWNLOAD_TASK_ID IS 'Collection download task ID if this data object is part of a collection download task';  


-- HPC_DOWNLOAD_TASK_RESULT
ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (COLLECTION_DOWNLOAD_TASK_ID VARCHAR2(50)); 
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.COLLECTION_DOWNLOAD_TASK_ID IS 'Collection download task ID if this data object is part of a collection download task';  


