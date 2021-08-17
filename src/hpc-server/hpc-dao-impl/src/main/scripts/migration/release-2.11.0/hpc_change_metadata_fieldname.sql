--
-- hpc_data_management_configuration.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sarada.chintala@nih.gov">Sarada Chintala</a>
--

UPDATE R_META_MAIN SET META_ATTR_NAME='registration_event_required' WHERE META_ATTR_NAME='registration_completion_event';
COMMIT;