--
-- hpc_collection_download_task_update.sql
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

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (
    EXTERNAL_ARCHIVE_FLAG CHAR default '0'
	);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.EXTERNAL_ARCHIVE_FLAG IS 'Indicates if the data is stored externally';

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (
    ARCHIVE_LINK_REGISTRATION_TASK_ID  VARCHAR2(2700)
	);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.ARCHIVE_LINK_REGISTRATION_TASK_ID IS 'Indicates the registration ID for the archive link for files stored externally';
