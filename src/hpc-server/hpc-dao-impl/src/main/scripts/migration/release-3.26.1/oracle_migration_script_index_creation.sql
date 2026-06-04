-- oracle_migration_script_index_creation.sql
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:sunita.menon@nih.gov">Sunita Menon</a>

CREATE INDEX hpc_data_meta_main_mv_comp_idx 
ON HPC_DATA_META_MAIN_MV (OBJECT_PATH, META_ATTR_NAME, STANDARD_HASH(META_ATTR_VALUE)) 
ONLINE PARALLEL 4;

ALTER INDEX HPC_DATA_META_MAIN_MV_COMPOSITE_IDX  NOPARALLEL;

DROP INDEX HPC_DATA_META_MAIN_MV_PATH_QUERY;
