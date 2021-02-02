--
-- hpc_data_migration.sql
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
  
CREATE TABLE HPC_DATA_MIGRATION_TASK (
	ID VARCHAR2(50) PRIMARY KEY,
	USER_ID VARCHAR2(50),
	PATH VARCHAR2(2700),
	CONFIGURATION_ID VARCHAR2(50),
	FROM_S3_ARCHIVE_CONFIGURATION_ID VARCHAR2(50),
	TO_S3_ARCHIVE_CONFIGURATION_ID VARCHAR2(50),
	TYPE VARCHAR2(50),
	STATUS TYPE VARCHAR2(50),
	CREATED TIMESTAMP(6)
);
        
                  

