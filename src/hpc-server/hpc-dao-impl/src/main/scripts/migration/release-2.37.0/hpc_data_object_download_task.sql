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

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (ASPERA_ACCOUNT_USER BLOB);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.ASPERA_ACCOUNT_USER IS
                 'The Aspera account user';
                 
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (ASPERA_ACCOUNT_PASSWORD BLOB);
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.ASPERA_ACCOUNT_PASSWORD IS
                 'The Aspera account password';

ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (ASPERA_ACCOUNT_HOST VARCHAR2(250));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.ASPERA_ACCOUNT_HOST IS
                 'The Aspera account host';