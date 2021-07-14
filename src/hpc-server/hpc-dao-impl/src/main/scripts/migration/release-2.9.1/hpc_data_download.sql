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
                
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (RETRY_TASK_ID VARCHAR2(250)); 
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.RETRY_TASK_ID IS 'A task ID to be re-tried';  
       