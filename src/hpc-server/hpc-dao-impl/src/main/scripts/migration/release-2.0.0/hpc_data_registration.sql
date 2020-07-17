--
-- hpc_data_registration.sql
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

-- HPCDATAMGM-1321 Single file registration auditing
DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_REGISTRATION_RESULT";
CREATE TABLE public."HPC_DATA_OBJECT_REGISTRATION_RESULT"
(
  "ID" text PRIMARY KEY,
  "PATH" text NOT NULL,
  "USER_ID" text NOT NULL,
  "UPLOAD_METHOD" text,
  "RESULT" boolean NOT NULL,
  "MESSAGE" text,
  "EFFECTIVE_TRANSFER_SPEED" integer,
  "DATA_TRANSFER_REQUEST_ID" text,
  "SOURCE_LOCATION_FILE_ID" text,
  "SOURCE_LOCATION_FILE_CONTAINER_ID" text,
  "SOURCE_LOCATION_FILE_CONTAINER_NAME" text,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_OBJECT_REGISTRATION_RESULT" IS 
                 'Data object registration task results';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."ID" IS 
                  'The registered data object ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."PATH" IS 
                  'The registered data object path';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."UPLOAD_METHOD" IS 
                  'The upload method used w/ the registration request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."RESULT" IS 
                  'Registration success/failure indicator';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."MESSAGE" IS 
                  'An error message if the registration failed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."EFFECTIVE_TRANSFER_SPEED" IS 
                  'The upload effective transfer speed in bytes per second';     
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (Globus, S3, etc) request ID';    
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."SOURCE_LOCATION_FILE_ID" IS 
                  'The file ID on the upload source endpoint';    
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."SOURCE_LOCATION_FILE_CONTAINER_ID" IS 
                  'The upload source container ID';    
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."SOURCE_LOCATION_FILE_CONTAINER_NAME" IS 
                  'The upload source container name';    
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."COMPLETED" IS 
                  'The data/time the bulk registration request was completed';