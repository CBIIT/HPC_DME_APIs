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

ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION add (CREATE_ARCHIVE_METADATA CHAR(1) default 1 not null); 
COMMENT ON COLUMN  HPC_DATA_MANAGEMENT_CONFIGURATION.CREATE_ARCHIVE_METADATA IS 
                 'An indicator whether archive metadata should be created for uploaded data objects';   
                 
ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (ENCRYPTION_KEY VARCHAR2(50), ENCRYPTION_ALGORITHM VARCHAR2(50)); 
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.ENCRYPTION_KEY IS 
                 'Encryption secret key';  
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.ENCRYPTION_ALGORITHM IS 
                 'Encryption algorithm';  
                 

ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION add (GLOBUS_ENCRYPTED_TRANSFER CHAR(1) default 0 not null); 
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_CONFIGURATION.GLOBUS_ENCRYPTED_TRANSFER IS 
                 'Globus encrypter transfer indicator';  

                 
                 