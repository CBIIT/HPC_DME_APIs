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
                
ALTER TABLE HPC_DATA_MANAGEMENT_CONFIGURATION ADD (S3_EXTERNAL_CONFIGURATION_ID VARCHAR2(50)); 
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_CONFIGURATION.S3_EXTERNAL_CONFIGURATION_ID IS 
                 'The external archive configured for this DOC';
       