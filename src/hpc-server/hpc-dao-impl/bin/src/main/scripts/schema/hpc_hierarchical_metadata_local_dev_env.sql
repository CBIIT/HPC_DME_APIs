--
-- hpc_hierarchical_metadata_local_dev_env.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--
-- This script is intended to be used in setting up a local development environment.
-- In a local development environment, we typically don't have a local iRODS install but instead
-- pointing to the DEV environment iRODS server. However, the HPC-DM server directly queries the hierarchical metadata
-- materialized views (which are built based on iRODS DB, and thus exist on where iRODS is deployed).
-- This script will create the DEV hierarchical metadata materialized views as foriegn tables in the local DB, which
-- allows the local server to query them directly.

CREATE EXTENSION IF NOT EXISTS postgres_fdw;

DROP SERVER IF EXISTS hpc_dm_dev CASCADE;
CREATE SERVER hpc_dm_dev
       FOREIGN DATA WRAPPER postgres_fdw
       OPTIONS (host 'fr-s-hpcdm-gp-d.ncifcrf.gov', port '5432', dbname 'ICAT');

CREATE USER MAPPING FOR postgres
       SERVER hpc_dm_dev
       OPTIONS (user 'postgres', password '<configure me>');

CREATE FOREIGN TABLE r_coll_hierarchy_meta_attr_name (
       level_label text,
       meta_attr_name text,
       object_ids integer[])
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_coll_hierarchy_meta_attr_name');

CREATE FOREIGN TABLE r_coll_hierarchy_meta_main (
       object_id integer,
       object_path text,
       coll_id integer,
       meta_id integer,
       level integer,
       level_label text,
       meta_attr_name text,
       meta_attr_value text,
       meta_attr_unit text)
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_coll_hierarchy_meta_main');
       
CREATE FOREIGN TABLE r_data_hierarchy_meta_attr_name (
	   level_label text,
       meta_attr_name text,
       object_ids integer[])
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_data_hierarchy_meta_attr_name');       

CREATE FOREIGN TABLE r_data_hierarchy_meta_main (
       object_id integer,
       object_path text,
       coll_id integer,
       meta_id integer,
       level integer,
       level_label text,
       meta_attr_name text,
       meta_attr_value text,
       meta_attr_unit text)
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_data_hierarchy_meta_main');
       
 CREATE FOREIGN TABLE r_user_main (
       user_id integer,
       user_name text,
       user_type_name text,
       zone_name text,
       user_info text,
       r_comment text,
       create_ts text,
       modify_ts text)
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_user_main');
       
 CREATE FOREIGN TABLE r_user_group (
       group_user_id integer,
       user_id integer,
       create_ts text,
       modify_ts text)
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_user_group');      
       
CREATE FOREIGN TABLE r_objt_access (
       object_id integer,
       user_id integer,
       access_type_id integer,
       create_ts text,
       modify_ts text)
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'r_objt_access');
