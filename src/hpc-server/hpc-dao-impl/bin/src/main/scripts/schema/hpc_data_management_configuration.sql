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
  "S3_UPLOAD_ARCHIVE_CONFIGURATION_ID" text,
  "S3_DEFAULT_DOWNLOAD_ARCHIVE_CONFIGURATION_ID" text,
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
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_UPLOAD_ARCHIVE_CONFIGURATION_ID" IS 
                  'The S3 archive to be used for uploading new files for this DOC';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_DEFAULT_DOWNLOAD_ARCHIVE_CONFIGURATION_ID" IS 
                  'The default (was first) S3 archive to use for downloading files for this DOC';                  
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
                  
                  
DROP TABLE IF EXISTS public."HPC_S3_ARCHIVE_CONFIGURATION";
CREATE TABLE public."HPC_S3_ARCHIVE_CONFIGURATION"
(
  "ID" text PRIMARY KEY,
  "PROVIDER" text NOT NULL,
  "DATA_MANAGEMENT_CONFIGURATION_ID" text NOT NULL,
  "URL_OR_REGION" text NOT NULL,
  "BUCKET" text NOT NULL,
  "OBJECT_ID" text NOT NULL,
  "UPLOAD_REQUEST_URL_EXPIRATION" integer NOT NULL
)
WITH (
  OIDS=FALSE
);  

COMMENT ON TABLE public."HPC_S3_ARCHIVE_CONFIGURATION" IS 
                 'The S3 archive configurations (per DOC) supported by HPC-DME';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."ID" IS 
                  'The S3 Configuration ID';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."PROVIDER" IS 
                  'The S3 Provider - Cleversafe, Cloudian, AWS, etc';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."DATA_MANAGEMENT_CONFIGURATION_ID" IS 
                  'The DM config that own this S3 archive configuration';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."URL_OR_REGION" IS 
                  'The S3 archive URL for 3rd Party S3 Provide, or region for AWS';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."BUCKET" IS 
                  'The S3 archive bucket';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."OBJECT_ID" IS 
                  'The S3 archive object id prefix';
COMMENT ON COLUMN public."HPC_S3_ARCHIVE_CONFIGURATION"."UPLOAD_REQUEST_URL_EXPIRATION" IS 
                  'The expiration period (in hours) to set when S3 upload request URL is generated';      
