--
-- hpc_data_meta_main.sql
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

-- Creating object_path index for hpc_data_meta_main
create index hpc_data_meta_main_path_query
    on irods.hpc_data_meta_main (object_path);
