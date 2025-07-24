--
-- hpc_data_transfer_configuration.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sarada.chintala@nih.gov">Sarada Chintala</a>
--
ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (
	EXTERNAL_STORAGE CHAR default '0'
	);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.EXTERNAL_STORAGE IS 'Indicator if the data is stored externally or internally within DME';

ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (
	POSIX_PATH VARCHAR2(2700)
	);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.POSIX_PATH IS 'Contains the POSIX path';

