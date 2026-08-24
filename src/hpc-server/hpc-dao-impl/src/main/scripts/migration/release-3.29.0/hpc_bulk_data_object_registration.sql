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

-- HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT
    ADD REGISTRATION_SIZE NUMBER(20);


COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT.REGISTRATION_SIZE IS 'The total size in bytes of the objects in the registration request';

-- HPC_BULK_DATA_OBJECT_REGISTRATION_TASK

ALTER TABLE HPC_BULK_DATA_OBJECT_REGISTRATION_TASK
    ADD REGISTRATION_SIZE NUMBER(20);

COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_TASK.REGISTRATION_SIZE IS 'The total size in bytes of the objects in the registration request';


