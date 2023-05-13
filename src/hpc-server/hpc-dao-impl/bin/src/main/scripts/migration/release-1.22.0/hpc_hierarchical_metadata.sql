--
-- hpc_hierarchical_metadata.sql
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

-- Create internal schema
CREATE SCHEMA internal;
-- Set the search path to look for internal schema, then public
ALTER DATABASE "ICAT" SET search_path TO "$user",internal,public;
	
-- Function to prepare for metadata materialized views refresh
CREATE OR REPLACE FUNCTION internal.prepare_hierarchy_meta_view_refresh() RETURNS void AS
$$
BEGIN
	-- 1. Drop the tables in the internal schema, create table and indexes from the materialized views.
	DROP TABLE IF EXISTS internal.temp_r_coll_hierarchy_meta_attr_name;
	DROP TABLE IF EXISTS internal.temp_r_coll_hierarchy_meta_main;
	DROP TABLE IF EXISTS internal.temp_r_data_hierarchy_meta_attr_name;
	DROP TABLE IF EXISTS internal.temp_r_data_hierarchy_meta_main;

	-- Hierarchy data meta_main
	CREATE TABLE internal.temp_r_data_hierarchy_meta_main AS TABLE public.r_data_hierarchy_meta_main;
	CREATE UNIQUE INDEX r_data_hierarchy_meta_main_unique ON internal.temp_r_data_hierarchy_meta_main (object_id, meta_id, level);
	CREATE INDEX r_data_hierarchy_meta_main_path_query ON internal.temp_r_data_hierarchy_meta_main (object_path);
	CREATE INDEX r_data_hierarchy_meta_main_id_query ON internal.temp_r_data_hierarchy_meta_main (object_id);
	CREATE INDEX r_data_hierarchy_meta_main_metadata_query_level ON internal.temp_r_data_hierarchy_meta_main (meta_attr_name, meta_attr_value, level);
	CREATE INDEX r_data_hierarchy_meta_main_metadata_query_level_label ON internal.temp_r_data_hierarchy_meta_main (meta_attr_name, meta_attr_value, level_label);

	-- Hierarchy collection meta_main
	CREATE TABLE internal.temp_r_coll_hierarchy_meta_main AS TABLE public.r_coll_hierarchy_meta_main;
	CREATE UNIQUE INDEX r_coll_hierarchy_meta_main_unique ON internal.temp_r_coll_hierarchy_meta_main (object_id, meta_id, level);
	CREATE INDEX r_coll_hierarchy_meta_main_path_query ON internal.temp_r_coll_hierarchy_meta_main (object_path);
	CREATE INDEX r_coll_hierarchy_meta_main_id_query ON internal.temp_r_coll_hierarchy_meta_main (object_id);
	CREATE INDEX r_coll_hierarchy_meta_main_metadata_query_level ON internal.temp_r_coll_hierarchy_meta_main (meta_attr_name, meta_attr_value, level);
	CREATE INDEX r_coll_hierarchy_meta_main_metadata_query_level_label ON internal.temp_r_coll_hierarchy_meta_main (meta_attr_name, meta_attr_value, level_label);

	-- Hierarchy data meta_attr_name
	CREATE TABLE internal.temp_r_data_hierarchy_meta_attr_name AS TABLE public.r_data_hierarchy_meta_attr_name;
	CREATE UNIQUE INDEX r_data_hierarchy_meta_attr_name_unique ON internal.temp_r_data_hierarchy_meta_attr_name (level_label, meta_attr_name);

	-- Hierarchy collection meta_attr_name
	CREATE TABLE internal.temp_r_coll_hierarchy_meta_attr_name AS TABLE public.r_coll_hierarchy_meta_attr_name;
	CREATE UNIQUE INDEX r_coll_hierarchy_meta_attr_name_unique ON internal.temp_r_coll_hierarchy_meta_attr_name (level_label, meta_attr_name);

	-- 2. Rename internal.temp_xxx tables to internal.xxx (eg. internal.temp_r_data_hierarchy_meta_main to internal.r_data_hierarchy_meta_main)
	ALTER TABLE IF EXISTS internal.temp_r_coll_hierarchy_meta_attr_name RENAME TO r_coll_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.temp_r_coll_hierarchy_meta_main RENAME TO r_coll_hierarchy_meta_main;
	ALTER TABLE IF EXISTS internal.temp_r_data_hierarchy_meta_attr_name RENAME TO r_data_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.temp_r_data_hierarchy_meta_main RENAME TO r_data_hierarchy_meta_main;

END;
$$
LANGUAGE plpgsql;

-- Function to refresh metadata materialized views
CREATE OR REPLACE FUNCTION internal.refresh_hierarchy_meta_view() RETURNS void AS
$$
BEGIN
	-- 1. Sleep for 1 second to allow query against the materialized view to complete if any.
	PERFORM pg_sleep(1);

	-- 2. Refresh the materialized views in the public schema.
	REFRESH MATERIALIZED VIEW public.r_coll_hierarchy_metamap;
	REFRESH MATERIALIZED VIEW public.r_coll_hierarchy_meta_main;
	REFRESH MATERIALIZED VIEW public.r_coll_hierarchy_meta_attr_name;
	REFRESH MATERIALIZED VIEW public.r_data_hierarchy_metamap;
	REFRESH MATERIALIZED VIEW public.r_data_hierarchy_meta_main;
	REFRESH MATERIALIZED VIEW public.r_data_hierarchy_meta_attr_name;
	REFRESH MATERIALIZED VIEW public.r_catalog_meta_main;

	-- 3. Rename internal.xxx to internal.temp_xxx (eg. internal.r_data_hierarchy_meta_main to internal.temp_r_data_hierarchy_meta_main)
	ALTER TABLE IF EXISTS internal.r_coll_hierarchy_meta_attr_name RENAME TO temp_r_coll_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.r_coll_hierarchy_meta_main RENAME TO temp_r_coll_hierarchy_meta_main;
	ALTER TABLE IF EXISTS internal.r_data_hierarchy_meta_attr_name RENAME TO temp_r_data_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.r_data_hierarchy_meta_main RENAME TO temp_r_data_hierarchy_meta_main;

END;
$$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION internal.cleanup_hierarchy_meta_view_refresh() RETURNS void AS
$$
BEGIN
	-- Rename the temp tables if previous refresh was unsuccessful
	ALTER TABLE IF EXISTS internal.r_coll_hierarchy_meta_attr_name RENAME TO temp_r_coll_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.r_coll_hierarchy_meta_main RENAME TO temp_r_coll_hierarchy_meta_main;
	ALTER TABLE IF EXISTS internal.r_data_hierarchy_meta_attr_name RENAME TO temp_r_data_hierarchy_meta_attr_name;
	ALTER TABLE IF EXISTS internal.r_data_hierarchy_meta_main RENAME TO temp_r_data_hierarchy_meta_main;

END;
$$
LANGUAGE plpgsql;

