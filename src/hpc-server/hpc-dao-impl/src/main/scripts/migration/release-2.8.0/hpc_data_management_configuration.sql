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
                
ALTER TABLE HPC_S3_ARCHIVE_CONFIGURATION add (STORAGE_CLASS VARCHAR2(50)); 
COMMENT ON COLUMN HPC_S3_ARCHIVE_CONFIGURATION.STORAGE_CLASS IS 
                 'The storage class to use when uploading data';  
       