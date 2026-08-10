--
-- hpc_bulk_data_object_registration_task.sql
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

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_TASK add (
    EXTERNAL_ARCHIVE_FLAG CHAR default '0'
	);
COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_TASK.EXTERNAL_ARCHIVE_FLAG IS 'Indicates if the data is stored externally';