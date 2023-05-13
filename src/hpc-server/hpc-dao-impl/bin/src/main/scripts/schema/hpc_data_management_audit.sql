--
-- hpc_data_management_audit.sql
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

DROP TABLE IF EXISTS public."HPC_DATA_MANAGEMENT_AUDIT";
CREATE TABLE public."HPC_DATA_MANAGEMENT_AUDIT"
(
  "USER_ID" text NOT NULL,
  "PATH" text NOT NULL,
  "REQUEST_TYPE" text NOT NULL,
  "METADATA_BEFORE" text NOT NULL,
  "METADATA_AFTER" text,
  "ARCHIVE_FILE_CONTAINER_ID" text,
  "ARCHIVE_FILE_ID" text,
  "DATA_MANAGEMENT_STATUS" boolean NOT NULL,
  "DATA_TRANSFER_STATUS" boolean,
  "MESSAGE" text,
  "COMPLETED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_MANAGEMENT_AUDIT" IS 
                 'Data management audit of collection/data-objects update and delete requests';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."PATH" IS 
                  'The collection or data object path';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."REQUEST_TYPE" IS 
                  'The request that is recorded for an audit';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."METADATA_BEFORE" IS 
                  'The collection/data-object metadata before the change';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."METADATA_AFTER" IS 
                  'The collection/data-object metadata after the change';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."ARCHIVE_FILE_CONTAINER_ID" IS 
                  'The data object archive file container ID';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."ARCHIVE_FILE_ID" IS 
                  'The data object archive file ID';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."DATA_MANAGEMENT_STATUS" IS 
                  'Data management (iRODS) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."DATA_TRANSFER_STATUS" IS 
                  'Data transfer (Cleversafe) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."MESSAGE" IS 
                  'Error message if the operation failed (iRODS or Cleversafe)';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_AUDIT"."COMPLETED" IS 
                  'The date/time the request was completed';
