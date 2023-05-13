--
-- hpc_dn_search_base.sql
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

CREATE TABLE HPC_DISTINGUISHED_NAME_SEARCH
(
  "BASE_PATH" VARCHAR2(2700) PRIMARY KEY,
  "USER_SEARCH_BASE" VARCHAR2(2700) NOT NULL,
  "GROUP_SEARCH_BASE" VARCHAR2(2700) NOT NULL
);

COMMENT ON TABLE HPC_DISTINGUISHED_NAME_SEARCH IS 
                 'LDAP search for users/groups for mounted disks on DME server';
COMMENT ON COLUMN HPC_DISTINGUISHED_NAME_SEARCH.BASE_PATH IS 
                 'The path to the mounted drive on DME server';      
COMMENT ON COLUMN HPC_DISTINGUISHED_NAME_SEARCH.USER_SEARCH_BASE IS 
                 'The search base for user';     
COMMENT ON COLUMN HPC_DISTINGUISHED_NAME_SEARCH.GROUP_SEARCH_BASE IS 
                 'The search base for group';             
