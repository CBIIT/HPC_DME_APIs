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

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (BOX_AUTH_CODE BLOB);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.BOX_AUTH_CODE IS
                 'The Box auth code';
                                 
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (BOX_AUTH_CODE BLOB);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.BOX_AUTH_CODE IS
                 'The Box auth code';
