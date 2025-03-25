--
-- hpc_api_calls_audit_archive.sql
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

create table HPC_API_CALLS_AUDIT_2022 parallel 4 nologging as select /*+parallel(HPC_API_CALLS_AUDIT 4)*/ *
from HPC_API_CALLS_AUDIT where created < to_date('20230101','YYYYMMDD');

delete /*+parallel(HPC_API_CALLS_AUDIT 4)*/ from HPC_API_CALLS_AUDIT where created < to_date('20230101','YYYYMMDD');

create table HPC_API_CALLS_AUDIT_2023 parallel 4 nologging as select /*+parallel(HPC_API_CALLS_AUDIT 4)*/ *
from HPC_API_CALLS_AUDIT where created < to_date('20240101','YYYYMMDD');

delete /*+parallel(HPC_API_CALLS_AUDIT 4)*/ from HPC_API_CALLS_AUDIT where created < to_date('20240101','YYYYMMDD');

create table HPC_API_CALLS_AUDIT_2024 parallel 4 nologging as select /*+parallel(HPC_API_CALLS_AUDIT 4)*/ *
from HPC_API_CALLS_AUDIT where created < to_date('20250101','YYYYMMDD');

delete /*+parallel(HPC_API_CALLS_AUDIT 4)*/ from HPC_API_CALLS_AUDIT where created < to_date('20250101','YYYYMMDD');
