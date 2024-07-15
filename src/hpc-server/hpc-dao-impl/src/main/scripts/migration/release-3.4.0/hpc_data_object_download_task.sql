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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (GLOBUS_ACCOUNT VARCHAR2(250));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.GLOBUS_ACCOUNT IS
                 'The Globus account';