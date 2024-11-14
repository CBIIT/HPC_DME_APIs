--
-- hpc_data_download.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Yuri Dinh</a>
--

create index HPC_DATA_OBJECT_DOWNLOAD_TASK_DATA_TRANSFER_STATUS_IDX
on IRODS.HPC_DATA_OBJECT_DOWNLOAD_TASK (DATA_TRANSFER_STATUS);

create index HPC_DOWNLOAD_TASK_RESULT_COMPLETED_IDX
on IRODS.HPC_DOWNLOAD_TASK_RESULT (COMPLETED);
