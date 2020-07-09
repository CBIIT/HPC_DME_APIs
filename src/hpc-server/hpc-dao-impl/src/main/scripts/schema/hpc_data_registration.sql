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

DROP TABLE IF EXISTS public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK";
CREATE TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "UI_URL" text,
  "STATUS" text,
  "ITEMS" text,
  "CREATED" timestamp
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK" IS 
                 'Bulk data object registration tasks';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."ID" IS 
                  'The bulk registration task ID';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."UI_URL" IS 
                  'The UI URL to view the task by id.
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."STATUS" IS 
                  'The bulk registration task status';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."ITEMS" IS 
                  'The list individual data object registrations included in this bulk registration request, in JSON format';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';

DROP TABLE IF EXISTS public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT";
CREATE TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text NOT NULL,
  "RESULT" boolean NOT NULL,
  "MESSAGE" text,
  "EFFECTIVE_TRANSFER_SPEED" integer,
  "ITEMS" text,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT" IS 
                 'Bulk data object registration task results';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."ID" IS 
                  'The bulk registration task ID';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."RESULT" IS 
                  'Task success/failure indicator';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."MESSAGE" IS 
                  'An error message if the task failed';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."EFFECTIVE_TRANSFER_SPEED" IS 
                  'The upload effective transfer speed in bytes per second';     
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."ITEMS" IS 
                  'The list individual data object registrations included in this bulk registration request, in JSON format';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."COMPLETED" IS 
                  'The data/time the bulk registration request was completed';
                  
DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_REGISTRATION_RESULT";
CREATE TABLE public."HPC_DATA_OBJECT_REGISTRATION_RESULT"
(
  "PATH" text NOT NULL,
  "USER_ID" text NOT NULL,
  "UPLOAD_METHOD" text,
  "RESULT" boolean NOT NULL,
  "MESSAGE" text,
  "EFFECTIVE_TRANSFER_SPEED" integer,
  "CREATED" timestamp NOT NULL,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_OBJECT_REGISTRATION_RESULT" IS 
                 'Data object registration task results';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."PATH" IS 
                  'The registered path';
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
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_REGISTRATION_RESULT"."COMPLETED" IS 
                  'The data/time the bulk registration request was completed';
