--
-- hpc_bulk_data_object_registration.sql
--
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:rosenbergea@nih.gov">Sunita Menon</a>
--

-- HPC_BULK_DATA_OBJECT_REGISTRATION_TASK

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_TASK
    ADD TOTAL_BYTES_TRANSFERRED NUMBER(19);

COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_TASK.TOTAL_BYTES_TRANSFERRED IS
    'Keep track of total bytes transferred for the bulk registration task';

-- HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT
    ADD TOTAL_BYTES_TRANSFERRED NUMBER(19);

COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT.TOTAL_BYTES_TRANSFERRED IS
    'The total bytes transferred for the bulk registration task';
