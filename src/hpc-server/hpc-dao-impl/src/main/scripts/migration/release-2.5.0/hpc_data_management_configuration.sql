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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--                

ALTER TABLE IRODS.HPC_S3_ARCHIVE_CONFIGURATION ADD TIERING_BUCKET VARCHAR2(250);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.TIERING_BUCKET IS 'The AWS S3 bucket for tiering from this archive';

ALTER TABLE IRODS.HPC_S3_ARCHIVE_CONFIGURATION ADD TIERING_PROTOCOL VARCHAR2(50);
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.TIERING_PROTOCOL IS 'The tiering protocol used S3 or S3GLACIER';