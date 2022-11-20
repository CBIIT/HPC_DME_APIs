                 
ALTER TABLE HPC_DATA_MIGRATION_TASK add (ALIGN_ARCHIVE_PATH CHAR default '0');
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.ALIGN_ARCHIVE_PATH IS 'Indicator if the file is moved within the same archive to align with logical path';

ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (ALIGN_ARCHIVE_PATH CHAR default '0');
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.ALIGN_ARCHIVE_PATH IS 'Indicator if the file is moved within the same archive to align with logical path';