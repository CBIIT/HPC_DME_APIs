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
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_DOWNLOAD_TASK";
CREATE TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "PATH" text,
  "CONFIGURATION_ID" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DATA_TRANSFER_STATUS" text,
  "ARCHIVE_LOCATION_FILE_CONTAINER_ID" text,
  "ARCHIVE_LOCATION_FILE_ID" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "DESTINATION_TYPE" text,
  "S3_ACCOUNT_ACCESS_KEY" bytea,
  "S3_ACCOUNT_SECRET_KEY" bytea,
  "S3_ACCOUNT_REGION" text,
  "COMPLETION_EVENT" boolean,
  "PERCENT_COMPLETE" integer,
  "SIZE" bigint,
  "CREATED" timestamp
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" IS 
                 'Active data object download tasks';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PATH" IS 
                  'The (iRODS) path of the file to be downloaded';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CONFIGURATION_ID" IS 
                  'The configuration ID to use in downloading the data object';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (S3 or Globus) request ID that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer (S3 or Globus) that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_STATUS" IS 
                  'The data transfer status (S3 or Globus)';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ARCHIVE_LOCATION_FILE_CONTAINER_ID" IS 
                  'The archive location container ID of the data object to be downloaded';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ARCHIVE_LOCATION_FILE_ID" IS 
                  'The archive location file ID of the data object to be downloaded';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_TYPE" IS 
                  'The download destination type - either Globus or S3';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_ACCESS_KEY" IS 
                  'The S3 destination account access key';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_SECRET_KEY" IS 
                  'The S3 destination account secret key';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ACCOUNT_REGION" IS 
                  'The S3 destination account region';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event needs to be generated when the task is completed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PERCENT_COMPLETE" IS 
                  'The download task completion %';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."SIZE" IS 
                  'The data object size';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';