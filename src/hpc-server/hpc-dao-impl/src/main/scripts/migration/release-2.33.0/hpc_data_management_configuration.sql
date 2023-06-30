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

ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION add (STORAGE_RECOVERY_CONFIGURATION CLOB);
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_CONFIGURATION.STORAGE_RECOVERY_CONFIGURATION IS
                 'Data object storage recovery (cleanup) configuration (JSON)';
                 
ALTER TABLE HPC_DATA_MANAGEMENT_AUDIT add (STORAGE_RECOVERY_CONFIGURATION CLOB);
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_AUDIT.STORAGE_RECOVERY_CONFIGURATION IS
                 'Data object storage recovery (cleanup) configuration (JSON)';
                
