--
-- hpc_data_hierarchy_meta_main_index.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--

alter index R_DATA_HIERARCHY_META_MAIN_METADATA_QUERY_LEVEL_LABEL parallel 4;
alter index R_DATA_HIERARCHY_META_MAIN_METADATA_QUERY_LEVEL_LOWER parallel 4;
alter index R_DATA_HIERARCHY_META_MAIN_PATH_QUERY parallel 4;
alter index R_DATA_HIERARCHY_META_MAIN_UNIQUE parallel 4;
alter index R_DATA_HIERARCHY_META_MAIN_ID_QUERY parallel 4;
alter index R_DATA_HIERARCHY_META_MAIN_METADATA_QUERY_LEVEL parallel 4;

