ALTER TABLE HPC_DATA_OBJECT_DOWNLOAD_TASK add (S3_DOWNLOAD_TASK_SERVER_ID VARCHAR(50));
COMMENT ON COLUMN HPC_DATA_OBJECT_DOWNLOAD_TASK.S3_DOWNLOAD_TASK_SERVER_ID IS
                 'Identifier for the server running the S3 download task';