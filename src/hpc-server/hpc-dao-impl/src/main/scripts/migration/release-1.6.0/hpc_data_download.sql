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

DROP TABLE IF EXISTS public."HPC_COLLECTION_DOWNLOAD_TASK";
CREATE TABLE public."HPC_COLLECTION_DOWNLOAD_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "TYPE" text NOT NULL,
  "PATH" text,
  "CONFIGURATION_ID" text,
  "DATA_OBJECT_PATHS" text[],
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "DESTINATION_OVERWRITE" boolean,
  "STATUS" text,
  "ITEMS" text,
  "PERCENT_COMPLETE" integer,
  "CREATED" timestamp
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
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_OVERWRITE" IS 
                  'An indicator whether files at the download destination will be overwritten if they exist';  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."STATUS" IS 
                  'The download task status';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';    
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."PERCENT_COMPLETE" IS 
                  'The download task completion %';        
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
                  
          
ALTER TABLE public."HPC_DOWNLOAD_TASK_RESULT" ADD COLUMN "EFFECTIVE_TRANSFER_SPEED" integer;
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."EFFECTIVE_TRANSFER_SPEED" IS 
                  'The download effective transfer speed in bytes per second';    
                  
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
  "DOWNLOAD_FILE_PATH" text,
  "ARCHIVE_LOCATION_FILE_CONTAINER_ID" text,
  "ARCHIVE_LOCATION_FILE_ID" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
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
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CONFIGURATION_ID" IS 
                  'The configuration ID to use in downloading the data object';
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
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event needs to be generated when the task is completed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PERCENT_COMPLETE" IS 
                  'The download task completion %';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."SIZE" IS 
                  'The data object size';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';                                  
