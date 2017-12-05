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
  "STATUS" text,
  "ITEMS" text,
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
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."STATUS" IS 
                  'The download task status';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';            
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
                                   
