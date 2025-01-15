--
-- hpc_data_download.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Yuri Dinh</a>
--

ALTER TABLE HPC_DATA_MIGRATION_TASK add (METADATA_ARCHIVE_FILE_CONTAINER_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.METADATA_ARCHIVE_FILE_CONTAINER_ID IS
                 'The file container id (bucket) targeted for metadata migration';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK add (METADATA_ARCHIVE_FILE_ID_PATTERN VARCHAR2(2700));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.METADATA_ARCHIVE_FILE_ID_PATTERN IS
                 'The file id pattern to include in metadata migration';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (METADATA_ARCHIVE_FILE_CONTAINER_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.METADATA_ARCHIVE_FILE_CONTAINER_ID IS
                 'The file container id (bucket) targeted for metadata migration';
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (METADATA_ARCHIVE_FILE_ID_PATTERN VARCHAR2(2700));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.METADATA_ARCHIVE_FILE_ID_PATTERN IS
                 'The file id pattern to include in metadata migration';

                 

