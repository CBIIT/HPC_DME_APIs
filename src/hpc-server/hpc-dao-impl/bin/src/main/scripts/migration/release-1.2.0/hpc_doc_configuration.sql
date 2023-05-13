--
-- hpc_doc_configuration.sql
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

DROP TABLE IF EXISTS public."HPC_DOC_CONFIGURATION";
CREATE TABLE public."HPC_DOC_CONFIGURATION"
(
  "DOC" text NOT NULL,
  "BASE_PATH" text NOT NULL,
  "S3_URL" text NOT NULL,
  "S3_VAULT" text NOT NULL,
  "S3_OBJECT_ID" text NOT NULL,
  "S3_ARCHIVE_TYPE" text NOT NULL,
  "DATA_HIERARCHY" text,
  "COLLECTION_METADATA_VALIDATION_RULES" text,
  "DATA_OBJECT_METADATA_VALIDATION_RULES" text,

  CONSTRAINT "HPC_DOC_pkey" PRIMARY KEY ("DOC")
)
WITH (
  OIDS=FALSE
);
