--
-- hpc_data_object_download_task_update.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sarada.chintala@nih.gov">Sarada Chintala</a>
--
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (
    EXTERNAL_ARCHIVE_FLAG CHAR default '0'
	);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.EXTERNAL_ARCHIVE_FLAG IS 'Indicates if the data is stored externally';
