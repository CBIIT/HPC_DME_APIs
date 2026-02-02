--
-- hpc_auto_tiering.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:rosenbergea@nih.gov">Eran Rosenberg</a>
--

-- HPC_S3_ARCHIVE_CONFIGURATION
ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (
	AUTO_TIERING_SEARCH_PATH VARCHAR2(2700)
	);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.AUTO_TIERING_SEARCH_PATH IS 'The search path for auto-tiering external archive files';

ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (
	AUTO_TIERING_INACTIVITY_MONTHS NUMBER(10)
	);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.AUTO_TIERING_INACTIVITY_MONTHS IS 'The inactivity period in months before files are auto-tiered to Glacier Deep Archive';

