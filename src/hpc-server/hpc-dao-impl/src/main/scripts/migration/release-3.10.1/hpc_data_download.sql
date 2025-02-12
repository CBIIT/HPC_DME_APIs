--
-- hpc_data_download_archive.sql
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

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (TOTAL_BYTES_TRANSFERRED NUMBER(19));
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.TOTAL_BYTES_TRANSFERRED IS
        'Keep track of total bytes transferred for the collection while the task is in RECEIVED state';