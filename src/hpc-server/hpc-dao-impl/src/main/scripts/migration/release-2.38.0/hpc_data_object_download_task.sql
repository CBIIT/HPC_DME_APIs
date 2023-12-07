ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK ADD (RETRY_TASK_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.RETRY_TASK_ID IS 'The task ID to be re-tried';

--
-- hpc_data_objectt_download_task.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>
--

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK ADD (RETRY_TASK_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.RETRY_TASK_ID IS 'The task ID to be re-tried';
