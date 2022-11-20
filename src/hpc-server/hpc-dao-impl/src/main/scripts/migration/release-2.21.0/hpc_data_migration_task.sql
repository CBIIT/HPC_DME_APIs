ALTER TABLE HPC_DATA_MIGRATION_TASK add (DATA_OBJECT_PATHS CLOB, COLLECTION_PATHS CLOB);
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.DATA_OBJECT_PATHS IS
                 'A list of data object paths to migrate';
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.COLLECTION_PATHS IS
                 'A list of collection paths to migrate';
                 
                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (DATA_OBJECT_PATHS CLOB, COLLECTION_PATHS CLOB);
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.DATA_OBJECT_PATHS IS
                 'A list of data object paths to migrate';
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK_RESULT.COLLECTION_PATHS IS
                 'A list of collection paths to migrate';

                 
