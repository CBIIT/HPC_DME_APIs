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

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (BOX_ACCESS_TOKEN BLOB);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.BOX_ACCESS_TOKEN IS
                 'The Box access token';
                 
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (BOX_REFRESH_TOKEN BLOB);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.BOX_REFRESH_TOKEN IS
                 'The Box refresh token';
                                 
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (BOX_ACCESS_TOKEN BLOB);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.BOX_ACCESS_TOKEN IS
                 'The Box access token';
                 
ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (BOX_REFRESH_TOKEN BLOB);
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.BOX_REFRESH_TOKEN IS
                 'The Box refresh token';
