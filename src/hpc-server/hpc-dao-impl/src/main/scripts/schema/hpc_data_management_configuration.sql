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
  "S3_URL" text,
  "S3_VAULT" text,
  "S3_OBJECT_ID" text,
  "S3_ARCHIVE_TYPE" text,
  "S3_UPLOAD_REQUEST_URL_EXPIRATION" integer,
  "GLOBUS_URL" text NOT NULL,
  "GLOBUS_ARCHIVE_ENDPOINT" text NOT NULL,
  "GLOBUS_ARCHIVE_PATH" text NOT NULL,
  "GLOBUS_ARCHIVE_DIRECTORY" text NOT NULL,
  "GLOBUS_ARCHIVE_TYPE" text NOT NULL,
  "GLOBUS_DOWNLOAD_ENDPOINT" text,
  "GLOBUS_DOWNLOAD_PATH" text,
  "GLOBUS_DOWNLOAD_DIRECTORY" text,
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
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_UPLOAD_REQUEST_URL_EXPIRATION" IS 
                  'The expiration period (in hours) to set when S3 upload request URL is generated'; 
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_URL" IS 
                  'The Globus authentication URL';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_ARCHIVE_ENDPOINT" IS 
                  'The Globus archive endpoint ID';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_ARCHIVE_PATH" IS 
                  'The Globus archive endpoint path';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_ARCHIVE_DIRECTORY" IS 
                  'The Globus archive directory (direct file system access to the endpoint)';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_ARCHIVE_TYPE" IS 
                  'The Globus archiove type (ARCHIVE or TEMPORARY_ARCHIVE)';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_DOWNLOAD_ENDPOINT" IS 
                  'The Globus download endpoint ID';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_DOWNLOAD_PATH" IS 
                  'The Globus download endpoint path';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."GLOBUS_DOWNLOAD_DIRECTORY" IS 
                  'The Globus download directory (direct file system access to the endpoint)';  
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_HIERARCHY" IS 
                  'The data hierarchy policy';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."COLLECTION_METADATA_VALIDATION_RULES" IS 
                  'The collection metadata validation rules';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_OBJECT_METADATA_VALIDATION_RULES" IS 
                  'The data object metadata validation rules';           
