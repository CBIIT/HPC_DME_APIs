ALTER TABLE HPC_DOWNLOAD_TASK_RESULT add (COLLECTION_PATHS CLOB);
COMMENT ON COLUMN HPC_DOWNLOAD_TASK_RESULT.COLLECTION_PATHS IS
                 'A list of collection paths included in this download task result';
