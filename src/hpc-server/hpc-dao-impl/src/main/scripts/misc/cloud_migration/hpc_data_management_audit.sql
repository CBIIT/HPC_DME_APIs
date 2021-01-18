
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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

ALTER TABLE IRODS.HPC_DATA_MANAGEMENT_AUDIT ADD FILTER_PREFIX VARCHAR2(2700);
COMMENT ON COLUMN HPC_DATA_MANAGEMENT_AUDIT.FILTER_PREFIX IS 'The tiering filter prefix that was applied';

ALTER TABLE IRODS.HPC_DATA_MANAGEMENT_AUDIT MODIFY METADATA_BEFORE NULL;
