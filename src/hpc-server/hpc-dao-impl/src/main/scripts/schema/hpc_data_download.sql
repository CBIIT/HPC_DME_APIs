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
  "ID" SERIAL PRIMARY KEY,
  "USER_ID" text,
  "PATH" text,
  "DOC" text,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text,
  "DOWNLOAD_FILE_PATH" text,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "CREATED" timestamp
)
WITH (
  OIDS=FALSE
);

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_DOWNLOAD_TASK_RESULT";
CREATE TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK_RESULT"
(
  "ID" integer NOT NULL,
  "USER_ID" text NOT NULL,
  "PATH" text NOT NULL,
  "DOC" text NOT NULL,
  "DATA_TRANSFER_REQUEST_ID" text,
  "DATA_TRANSFER_TYPE" text NOT NULL,
  "DESTINATION_LOCATION_FILE_CONTAINER_ID" text,
  "DESTINATION_LOCATION_FILE_ID" text,
  "RESULT" boolean NOT NULL,
  "MESSAGE" text,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);
