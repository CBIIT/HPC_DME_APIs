--
-- hpc_data_transfer_queues.sql
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

DROP TABLE IF EXISTS public."HPC_DATA_TRANSFER_UPLOAD_QUEUE";
CREATE TABLE public."HPC_DATA_TRANSFER_UPLOAD_QUEUE"
(
  "PATH" text PRIMARY KEY,
  "CALLER_OBJECT_ID" text,
  "USER_ID" text NOT NULL,
  "SOURCE_LOCATION_FILE_CONTAINER_ID" text NOT NULL,
  "SOURCE_LOCATION_FILE_ID" text NOT NULL,
  "DOC" text NOT NULL,
  "DATA_TRANSFER_TYPE" text NOT NULL
)
WITH (
  OIDS=FALSE
);


