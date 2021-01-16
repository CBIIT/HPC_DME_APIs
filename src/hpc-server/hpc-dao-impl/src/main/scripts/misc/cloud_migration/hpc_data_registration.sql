--
-- hpc_data_registration.sql
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

ALTER TABLE IRODS.HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT ADD REQUEST_TYPE VARCHAR2(50);
COMMENT ON COLUMN HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT.REQUEST_TYPE IS 'The request type of the task';
