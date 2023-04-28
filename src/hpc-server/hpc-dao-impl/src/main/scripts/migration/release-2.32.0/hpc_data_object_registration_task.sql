--
-- hpc_data_object_registration_task.sql
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

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_TASK add (UPLOAD_METHOD VARCHAR2(50));
COMMENT ON COLUMN  HPC_BULK_DATA_OBJECT_REGISTRATION_TASK.UPLOAD_METHOD IS
                 'The data transfer upload method used in this bulk registration task';
                 
ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT add (UPLOAD_METHOD VARCHAR2(50));
COMMENT ON COLUMN  HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT.UPLOAD_METHOD IS
                 'The data transfer upload method used in this bulk registration task';
