ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK add (DESTINATION_TYPE VARCHAR(50));
COMMENT ON COLUMN HPC_COLLECTION_DOWNLOAD_TASK.DESTINATION_TYPE IS
                 'The download destination type - Globus, S3, Google Drive or Google Cloud';
