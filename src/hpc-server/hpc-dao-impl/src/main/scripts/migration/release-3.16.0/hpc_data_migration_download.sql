--
-- hpc_data_migration_download.sql
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

create index HPC_DATA_MIGRATION_TASK_RESULT_PARENT_PATH_ID_IDX
on IRODS.HPC_DATA_MIGRATION_TASK_RESULT (PARENT_ID, PATH, ID);

create index HPC_DOWNLOAD_TASK_RESULT_COLLECTION_TASK_ID_IDX
on IRODS.HPC_DOWNLOAD_TASK_RESULT (COLLECTION_DOWNLOAD_TASK_ID);
