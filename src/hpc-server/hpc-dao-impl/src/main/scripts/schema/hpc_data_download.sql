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
  "DOC" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DATA_TRANSFER_STATUS" text,
  "DOWNLOAD_FILE_PATH" text,
  "ARCHIVE_LOCATION_FILE_CONTAINER_ID" text,
  "ARCHIVE_LOCATION_FILE_ID" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "COMPLETION_EVENT" boolean,
  "CREATED" timestamp
)
WITH (
  OIDS=FALSE
);

DROP TABLE IF EXISTS public."HPC_COLLECTION_DOWNLOAD_TASK";
CREATE TABLE public."HPC_COLLECTION_DOWNLOAD_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "TYPE" text NOT NULL,
  "PATH" text,
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

DROP TABLE IF EXISTS public."HPC_DOWNLOAD_TASK_RESULT";
CREATE TABLE public."HPC_DOWNLOAD_TASK_RESULT"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text NOT NULL,
  "TYPE" text NOT NULL,
  "PATH" text,
  "DOC" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text NOT NULL,
  "DESTINATION_LOCATION_FILE_ID" text NOT NULL,
  "RESULT" boolean NOT NULL,
  "MESSAGE" text,
  "ITEMS" text,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);


