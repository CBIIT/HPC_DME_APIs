--
-- hpc_globus_transfer_task_index.sql
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

create index HPC_GLOBUS_TRANSFER_TASK_GLOBUS_ACCOUNT_IDX
    on irods.hpc_globus_transfer_task (globus_account);
    