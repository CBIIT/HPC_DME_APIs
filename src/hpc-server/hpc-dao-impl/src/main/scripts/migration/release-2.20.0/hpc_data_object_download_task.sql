ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (FIRST_HOP_RETRIED CHAR(1));
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.FIRST_HOP_RETRIED IS
                 'An indicator if a first hop download was retried in this download task';

                 
ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (FIRST_HOP_RETRIED CHAR(1));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.FIRST_HOP_RETRIED IS
                 'An indicator if a first hop download was retried in this download task';