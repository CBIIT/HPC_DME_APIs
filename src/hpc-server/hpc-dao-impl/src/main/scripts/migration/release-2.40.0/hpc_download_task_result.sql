--
-- hpc_download_task_result.sql
--
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
-- JIRA Ticket: HPCDATAMGM-1488
-- Description: Add new columns for source location to aid in debugging
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>

ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (ARCHIVE_LOCATION_FILE_CONTAINER_ID VARCHAR2(200));
COMMENT ON COLUMN ARCHIVE_LOCATION_FILE_CONTAINER_ID IS 'The bucket containing the data object';

ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (ARCHIVE_LOCATION_FILE_ID VARCHAR2(2700));
COMMENT ON COLUMN ARCHIVE_LOCATION_FILE_ID IS 'The key/path of the data object';
