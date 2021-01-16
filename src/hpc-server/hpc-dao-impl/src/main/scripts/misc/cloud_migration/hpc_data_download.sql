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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

ALTER TABLE IRODS.HPC_DATA_OBJECT_DOWNLOAD_TASK ADD RESTORE_REQUESTED CHAR DEFAULT '0' NOT NULL;
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.RESTORE_REQUESTED IS 'Flag to indicate whether restoration was requested for this download';

ALTER TABLE IRODS.HPC_DOWNLOAD_TASK_RESULT ADD RESTORE_REQUESTED CHAR DEFAULT '0' NOT NULL;
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.RESTORE_REQUESTED IS 'Flag to indicate whether restoration was requested for this download';
