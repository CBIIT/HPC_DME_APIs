--
-- hpc_download_task_result.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

drop index hpc_download_task_request_user_query
create index hpc_download_task_request_user_query
    on HPC_DOWNLOAD_TASK_RESULT (USER_ID, COMPLETION_EVENT);
