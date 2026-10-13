--
-- hpc_s3_archive_configuration.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:dinhys@nih.gov">Yuri Dinh</a>
--

-- HPC_S3_ARCHIVE_CONFIGURATION
ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION ADD USE_S3_LISTING CHAR(1);

COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.USE_S3_LISTING is 'Use S3 rather than POSIX for external directory listing';
