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
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (GOOGLE_ACCESS_TOKEN BLOB); 
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.GOOGLE_ACCESS_TOKEN IS 'Google drive/cloud storage access token';  

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (GOOGLE_ACCESS_TOKEN_TYPE VARCHAR2(20)); 
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.GOOGLE_ACCESS_TOKEN_TYPE IS 'Google drive/cloud storage access token type user or service account';  

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK drop column GOOGLE_DRIVE_ACCESS_TOKEN;


-- HPC_COLLECTION_DOWNLOAD_TASK
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (GOOGLE_ACCESS_TOKEN BLOB); 
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.GOOGLE_ACCESS_TOKEN IS 'Google drive/cloud storage access token';  

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (GOOGLE_ACCESS_TOKEN_TYPE VARCHAR2(20)); 
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.GOOGLE_ACCESS_TOKEN_TYPE IS 'Google drive/cloud storage access token type user or service account';  

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK drop column GOOGLE_DRIVE_ACCESS_TOKEN;
