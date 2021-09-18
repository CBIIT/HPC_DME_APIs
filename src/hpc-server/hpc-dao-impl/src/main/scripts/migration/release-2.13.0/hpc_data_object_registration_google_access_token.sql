--
-- hpc_data_object_registration_google_access_token.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

CREATE TABLE HPC_DATA_OBJECT_REGISTRATION_GOOGLE_ACCESS_TOKEN
(
	ID VARCHAR2(50) PRIMARY KEY,
	ACCESS_TOKEN BLOB NOT NULL,
	ACCESS_TOKEN_TYPE VARCHAR2(20),
	CREATED TIMESTAMP DEFAULT sysdate NOT NULL
	
);
	