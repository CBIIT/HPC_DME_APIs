--
-- hpc_data_download_index.sql
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

create index HPC_DATA_OBJECT_DOWNLOAD_TASK_COMPLETION_EVENT_IDX
    on irods.hpc_data_object_download_task (completion_event);
