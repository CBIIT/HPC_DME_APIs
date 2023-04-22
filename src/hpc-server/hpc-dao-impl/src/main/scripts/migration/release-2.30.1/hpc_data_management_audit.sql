--
-- hpc_data_management_audit.sql
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

ALTER TABLE HPC_DATA_MANAGEMENT_AUDIT add (DATA_SIZE NUMBER(19));
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_AUDIT.DATA_SIZE IS
                 'The data object or collection size in bytes';
