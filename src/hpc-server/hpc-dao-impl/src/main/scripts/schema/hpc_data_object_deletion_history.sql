--
-- hpc_data_object_deletion_history.sql
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

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_DELETION_HISTORY";
CREATE TABLE public."HPC_DATA_OBJECT_DELETION_HISTORY"
(
  "USER_ID" text NOT NULL,
  "PATH" text NOT NULL,
  "METADATA" text NOT NULL,
  "ARCHIVE_FILE_CONTAINER_ID" text NOT NULL,
  "ARCHIVE_FILE_ID" text NOT NULL,
  "ARCHIVE_DELETE_STATUS" boolean NOT NULL,
  "DATA_MANAGEMENT_DELETE_STATUS" boolean NOT NULL,
  "MESSAGE" text,
  "DELETED" date NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_OBJECT_DELETION_HISTORY" IS 
                 'Data object deletion requests';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."PATH" IS 
                  'The data object path';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."METADATA" IS 
                  'The data object metadata at the time of deletion';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_FILE_CONTAINER_ID" IS 
                  'The data object archive container ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_FILE_ID" IS 
                  'The data object archive file ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_DELETE_STATUS" IS 
                  'Deletion from archive (Cleversafe) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."DATA_MANAGEMENT_DELETE_STATUS" IS 
                  'Deletion from data management (iRODS) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."MESSAGE" IS 
                  'Error message if the deletion failed (iRODS or Cleversafe)';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."DELETED" IS 
                  'The date/time the request was submitted';
