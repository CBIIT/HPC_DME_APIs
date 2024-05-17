--
-- hpc_data_object_download_task.sql
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

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (APPEND_COLLECTION_NAME_TO_DOWNLOAD_DESTINATION CHAR);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.APPEND_COLLECTION_NAME_TO_DOWNLOAD_DESTINATION IS
                 'The append collection name to download destination indicator';
                 
