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
  "ARCHIVE_TYPE" text NOT NULL,
  "ARCHIVE_S3_URL" text,
  "ARCHIVE_S3_VAULT" text,
  "ARCHIVE_S3_OBJECT_ID" text,
  "ARCHIVE_S3_UPLOAD_REQUEST_URL_EXPIRATION" integer,
  "ARCHIVE_GLOBUS_URL" text NOT NULL,
  "ARCHIVE_GLOBUS_ENDPOINT" text NOT NULL,
  "ARCHIVE_GLOBUS_PATH" text NOT NULL,
  "ARCHIVE_GLOBUS_DIRECTORY" text,
  
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
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_TYPE" IS 
                  'The archive type (Cleversafe or POSIX)';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_S3_URL" IS 
                  'The archive (Cleversafe) S3 URL';     
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_S3_VAULT" IS 
                  'The archive (Cleversafe) S3 vault (bucket)';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_S3_OBJECT_ID" IS 
                  'The archive (Cleversafe) S3 object id prefix';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_S3_UPLOAD_REQUEST_URL_EXPIRATION" IS 
                  'The expiration period (in hours) to set when S3 upload request URL is generated';              
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_GLOBUS_URL" IS 
                  'The Globus authentication URL';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_GLOBUS_ENDPOINT" IS 
                  'The archive Globus endpoint ID';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_GLOBUS_PATH" IS 
                  'The archive Globus endpoint path';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_GLOBUS_DIRECTORY" IS 
                  'The archive Globus directory (direct file system access to the Globus endpoint path )';                  
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_HIERARCHY" IS 
                  'The data hierarchy policy';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."COLLECTION_METADATA_VALIDATION_RULES" IS 
                  'The collection metadata validation rules';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_OBJECT_METADATA_VALIDATION_RULES" IS 
                  'The data object metadata validation rules';           
