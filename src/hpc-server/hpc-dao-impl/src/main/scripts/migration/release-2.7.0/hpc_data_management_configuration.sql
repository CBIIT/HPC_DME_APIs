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

ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION add (CREATE_ARCHIVE_METADATA CHAR(1) default 1 not nulll); 
COMMENT ON COLUMN  HPC_DATA_MANAGEMENT_CONFIGURATION.CREATE_ARCHIVE_METADATA IS 
                 'An indicator whether archive metadata should be created for uploaded data objects';   