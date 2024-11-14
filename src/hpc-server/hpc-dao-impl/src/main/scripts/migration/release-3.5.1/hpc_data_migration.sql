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

ALTER TABLE HPC_DATA_MIGRATION_TASK add (RETRY_FAILED_ITEMS_ONLY CHAR);
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.RETRY_FAILED_ITEMS_ONLYD IS
                 'Indicator to retry bulk migration task by retrying the failed items only, and skip the collection(s) rescanning';
                 
                 
