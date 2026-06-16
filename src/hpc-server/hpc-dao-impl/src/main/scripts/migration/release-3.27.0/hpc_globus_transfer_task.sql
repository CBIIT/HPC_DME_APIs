--
-- hpc_globus_transfer_task.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>
--
                
ALTER TABLE HPC_GLOBUS_TRANSFER_TASK add (USER_ID VARCHAR2(50)); 
COMMENT ON COLUMN HPC_GLOBUS_TRANSFER_TASK.USER_ID IS 'The queued for globus transfer'
