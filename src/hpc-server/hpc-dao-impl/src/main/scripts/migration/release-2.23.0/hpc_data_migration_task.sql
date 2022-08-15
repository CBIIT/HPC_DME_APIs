                 
ALTER TABLE HPC_DATA_MIGRATION_TASK add (IN_PROCESS CHAR(1), SERVER_ID VARCHAR2(50));
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.IN_PROCESS IS
                 'In process indicator, meaning a schedule task is actively processing this task ';
COMMENT ON COLUMN HPC_DATA_MIGRATION_TASK.SERVER_ID IS
                 'The server Id that handles the migration task';