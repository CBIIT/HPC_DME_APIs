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

ALTER TABLE HPC_DATA_MIGRATION_TASK add (PARENT_RETRY_ID VARCHAR(250));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.PARENT_RETRY_ID IS
                 'The parent (collection) retry task ID';
                 
                 
