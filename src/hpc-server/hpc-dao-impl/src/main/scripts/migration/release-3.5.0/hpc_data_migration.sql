--
-- hpc_data_migration.sql
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

ALTER TABLE HPC_DATA_MIGRATION_TASK add (RETRY_TASK_ID VARCHAR(250));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.RETRY_TASK_ID IS
                 'The previous task ID if this is a retry request';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK add (RETRY_USER_ID VARCHAR(250));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.RETRY_USER_ID IS
                 'The user retrying the request if this is a retry request';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (RETRY_TASK_ID VARCHAR(250));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.RETRY_TASK_ID IS
                 'The previous task ID if this is a retry request';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (RETRY_USER_ID VARCHAR(250));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.RETRY_USER_ID IS
                 'The user retrying the request if this is a retry request';
                 
