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
                
ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION add (GLOBUS_HYPERFILE_ARCHIVE CHAR(1) default 0 not null); 
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_CONFIGURATION.GLOBUS_HYPERFILE_ARCHIVE IS 
                 'Hyperfile POSIX archive indicator';  
       