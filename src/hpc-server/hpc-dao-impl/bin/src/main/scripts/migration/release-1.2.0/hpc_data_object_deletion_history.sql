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
