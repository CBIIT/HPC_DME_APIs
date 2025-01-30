--
-- hpc_data_download_archive.sql
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

alter session enable parallel dml;

create table HPC_DOWNLOAD_TASK_RESULT_ARCHIVE_2023 parallel 4 nologging as select /*+parallel(HPC_DOWNLOAD_TASK_RESULT 4)*/ *
from HPC_DOWNLOAD_TASK_RESULT where created < to_date('20240101','YYYYMMDD');

delete /*+parallel(HPC_DOWNLOAD_TASK_RESULT 4)*/ from HPC_DOWNLOAD_TASK_RESULT where created < to_date('20240101','YYYYMMDD');

