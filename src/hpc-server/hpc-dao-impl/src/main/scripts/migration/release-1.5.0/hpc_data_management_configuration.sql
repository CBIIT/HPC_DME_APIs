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

ALTER TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" ADD COLUMN "S3_UPLOAD_REQUEST_URL_EXPIRATION" integer;
UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" SET "S3_UPLOAD_REQUEST_URL_EXPIRATION"= 24;
ALTER TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" ALTER COLUMN "S3_UPLOAD_REQUEST_URL_EXPIRATION" SET NOT NULL;

COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_UPLOAD_REQUEST_URL_EXPIRATION" IS 
                  'The expiration period (in hours) to set when S3 upload request URL is generated'; 