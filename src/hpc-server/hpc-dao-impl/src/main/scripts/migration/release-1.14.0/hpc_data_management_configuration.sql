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

ALTER TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" ADD COLUMN "ARCHIVE_TYPE" text;
UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" SET "ARCHIVE_TYPE"= 'CLEVERSAFE' WHERE "GLOBUS_ARCHIVE_TYPE" = 'TEMPORARY_ARCHIVE';
UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" SET "ARCHIVE_TYPE"= 'POSIX' WHERE "GLOBUS_ARCHIVE_TYPE" = 'ARCHIVE';
ALTER TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" ALTER COLUMN "ARCHIVE_TYPE" SET NOT NULL;

COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ARCHIVE_TYPE" IS 
                  'The archive type (Cleversafe or POSIX)';