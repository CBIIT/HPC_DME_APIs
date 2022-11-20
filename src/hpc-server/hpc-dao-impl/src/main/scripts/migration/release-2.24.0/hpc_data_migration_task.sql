                 
ALTER TABLE HPC_DATA_MIGRATION_TASK_RESULT add (SERVER_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.SERVER_ID IS
                 'The server Id that handles the migration task';