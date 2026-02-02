--
-- hpc_data_object_download.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

-- HPC_DOWNLOAD_TASK_RESULT
ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (GOOGLE_DRIVE_ACCESS_TOKEN BLOB); 
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.GOOGLE_DRIVE_ACCESS_TOKEN IS 'Google drive access token';  
