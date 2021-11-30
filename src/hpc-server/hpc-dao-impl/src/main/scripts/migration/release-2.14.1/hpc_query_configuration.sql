--
-- hpc_query_configuration.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--
           
DROP TABLE IRODS."HPC_QUERY_CONFIGURATION";
CREATE TABLE IRODS."HPC_QUERY_CONFIGURATION"
(
  BASE_PATH VARCHAR2(50) PRIMARY KEY NOT NULL,
  ENCRYPTION_KEY blob NOT NULL,
  ENCRYPT char(1)
);


COMMENT ON COLUMN IRODS."HPC_QUERY_CONFIGURATION"."BASE_PATH" IS 
                  'The base path to apply this configuration to';
COMMENT ON COLUMN IRODS."HPC_QUERY_CONFIGURATION"."ENCRYPTION_KEY" IS 
                  'The key to be used to encrypt the metadata';
COMMENT on column IRODS."HPC_QUERY_CONFIGURATION"."ENCRYPT" IS 
				  'Encryption on/off flag';
                 