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
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>
--

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_DOWNLOAD_TASK";
CREATE TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "PATH" text,
  "CONFIGURATION_ID" text,
  "S3_ARCHIVE_CONFIGURATION_ID" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DATA_TRANSFER_STATUS" text,
  "DOWNLOAD_FILE_PATH" text,
  "ARCHIVE_LOCATION_FILE_CONTAINER_ID" text,
  "ARCHIVE_LOCATION_FILE_ID" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "DESTINATION_TYPE" text,
  "S3_ACCOUNT_ACCESS_KEY" bytea,
  "S3_ACCOUNT_SECRET_KEY" bytea,
  "S3_ACCOUNT_REGION" text,
  "GOOGLE_DRIVE_ACCESS_TOKEN" bytea,
  "COMPLETION_EVENT" boolean,
  "PERCENT_COMPLETE" integer,
  "SIZE" bigint,
  "CREATED" timestamp,
  "PROCESSED" timestamp,
  "PRIORITY" integer DEFAULT 100,
  "IN_PROCESS" boolean NOT NULL DEFAULT false,
  "RETRY_TASK_ID" varchar2(50)
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
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CONFIGURATION_ID" IS 
                  'The configuration ID to use in downloading the data object';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."S3_ARCHIVE_CONFIGURATION_ID" IS 
                  'The S3 Archive configuration ID to use if downloading from S3 Archive';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (S3 or Globus) request ID that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer (S3 or Globus) that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_STATUS" IS 
                  'The data transfer status (S3 or Globus)';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DOWNLOAD_FILE_PATH" IS 
                  'The file path used in the 2-hop download';
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
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."GOOGLE_DRIVE_ACCESS_TOKEN" IS 
                  'The Google Drive Access Token';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event needs to be generated when the task is completed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PERCENT_COMPLETE" IS 
                  'The download task completion %';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."SIZE" IS 
                  'The data object size';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PROCESSED" IS 
                  'The date and time the task was processed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PRIORITY" IS 
                  'The download task priority';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."IN_PROCESS" IS 
                  'An indicator whether this task is in-process, i.e. a thread is working on submitting it';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."RETRY_TASK_ID" IS 
                  'The task ID to be re-tried';


DROP TABLE IF EXISTS public."HPC_COLLECTION_DOWNLOAD_TASK";
CREATE TABLE public."HPC_COLLECTION_DOWNLOAD_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "TYPE" text NOT NULL,
  "PATH" text,
  "CONFIGURATION_ID" text,
  "DATA_OBJECT_PATHS" text[],
  "COLLECTION_PATHS" text[],
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "DESTINATION_OVERWRITE" boolean,
  "S3_ACCOUNT_ACCESS_KEY" bytea,
  "S3_ACCOUNT_SECRET_KEY" bytea,
  "S3_ACCOUNT_REGION" text,
  "GOOGLE_DRIVE_ACCESS_TOKEN" bytea,
  "APPEND_PATH_TO_DOWNLOAD_DESTINATION" boolean,
  "STATUS" text,
  "ITEMS" text,
  "CREATED" timestamp,
  "PRIORITY" integer DEFAULT 100,
  "CANCELLATION_REQUESTED" boolean,
  "IN_PROCESS" boolean NOT NULL DEFAULT false
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" IS 
                 'Active collection or bulk download tasks';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."TYPE" IS 
                  'The type of the request - collection or bulk (list of data objects)';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."PATH" IS 
                  'The collection path to download';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CONFIGURATION_ID" IS 
                  'The configuration ID to use in downloading the collection';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DATA_OBJECT_PATHS" IS 
                  'The list of data object paths to download';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."COLLECTION_PATHS" IS 
                  'The list of collection paths to download';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_OVERWRITE" IS 
                  'An indicator whether files at the download destination will be overwritten if they exist';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."S3_ACCOUNT_ACCESS_KEY" IS 
                  'The S3 destination account access key';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."S3_ACCOUNT_SECRET_KEY" IS 
                  'The S3 destination account secret key';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."S3_ACCOUNT_REGION" IS 
                  'The S3 destination account region';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."GOOGLE_DRIVE_ACCESS_TOKEN" IS 
                  'The Google Drive Access Token';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."APPEND_PATH_TO_DOWNLOAD_DESTINATION" IS 
                  'An indicator whether to use the full object path at the download destination, or file name only';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."STATUS" IS 
                  'The download task status';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';    
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."PRIORITY" IS 
                  'The download task priority';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CANCELLATION_REQUESTED" IS 
                  'A request to cancel the download was submitted';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."IN_PROCESS" IS 
                  'An indicator whether this task is in-process, i.e. individual file download tasks are submitted';
                                   
DROP TABLE IF EXISTS public."HPC_DOWNLOAD_TASK_RESULT";
CREATE TABLE public."HPC_DOWNLOAD_TASK_RESULT"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text NOT NULL,
  "TYPE" text NOT NULL,
  "PATH" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text NOT NULL,
  "DESTINATION_LOCATION_FILE_CONTAINER_NAME" text,
  "DESTINATION_LOCATION_FILE_ID" text NOT NULL,
  "DESTINATION_TYPE" text,
  "RESULT" text NOT NULL,
  "MESSAGE" text,
  "ITEMS" text,
  "COMPLETION_EVENT" boolean,
  "EFFECTIVE_TRANSFER_SPEED" integer,
  "SIZE" bigint,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DOWNLOAD_TASK_RESULT" IS 
                 'Download task (single file, collection, bulk) results';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."TYPE" IS 
                  'The type of the request - data object, collection or bulk (list of data objects)';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."PATH" IS 
                  'The data object or collection path requested';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (S3 or Globus) request ID that was last used';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer (S3 or Globus) that was last used';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_CONTAINER_NAME" IS 
                  'The download destination container name';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';   
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_TYPE" IS 
                  'The download destination type - either Globus or S3';                     
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."RESULT" IS 
                  'The download task success/fail/canceled indicator';   
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."MESSAGE" IS 
                  'An error message in case the task failed';                     
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';  
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event was generated when the task completed'; 
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."EFFECTIVE_TRANSFER_SPEED" IS 
                  'The download effective transfer speed in bytes per second';   
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."SIZE" IS 
                  'The data object size';                                 
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."CREATED" IS 
                  'The date and time the task was created';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."COMPLETED" IS 
                  'The date and time the task was completed';
