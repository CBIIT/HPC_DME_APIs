--
-- hpc_data_management_configuration.sql
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

DROP TABLE IF EXISTS public."HPC_DATA_MANAGEMENT_CONFIGURATION";
CREATE TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION"
(
  "ID" text PRIMARY KEY,
  "BASE_PATH" text NOT NULL,
  "DOC" text NOT NULL,
  "S3_URL" text NOT NULL,
  "S3_VAULT" text NOT NULL,
  "S3_OBJECT_ID" text NOT NULL,
  "S3_ARCHIVE_TYPE" text NOT NULL,
  "DATA_HIERARCHY" text,
  "COLLECTION_METADATA_VALIDATION_RULES" text,
  "DATA_OBJECT_METADATA_VALIDATION_RULES" text
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" IS 
                 'The data management configurations supported by HPC-DME';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ID" IS 
                  'The configuration ID';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."BASE_PATH" IS 
                  'The base path to apply this configuration to';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DOC" IS 
                  'The DOC that own this configuration';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_URL" IS 
                  'The S3 archive (Cleversafe) URL';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_VAULT" IS 
                  'The S3 archive (Cleversafe) vault';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_OBJECT_ID" IS 
                  'The S3 archive (Cleversafe) object id prefix';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_ARCHIVE_TYPE" IS 
                  'The S3 archive type (Archive / Temp Archive). Note: Temp Archive is currently not used';  
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_HIERARCHY" IS 
                  'The data hierarchy policy';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."COLLECTION_METADATA_VALIDATION_RULES" IS 
                  'The collection metadata validation rules';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_OBJECT_METADATA_VALIDATION_RULES" IS 
                  'The data object metadata validation rules';           
