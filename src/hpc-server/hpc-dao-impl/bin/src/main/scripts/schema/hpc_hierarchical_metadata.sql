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
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

-- Drop all views first.
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_meta_attr_name;
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_meta_main;
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_metamap;
DROP MATERIALIZED VIEW IF EXISTS r_data_hierarchy_meta_attr_name;
DROP MATERIALIZED VIEW IF EXISTS r_data_hierarchy_meta_main;
DROP MATERIALIZED VIEW IF EXISTS r_data_hierarchy_metamap;

-- Create all materialized views

-- Hierarchy data metamap.
CREATE MATERIALIZED VIEW r_data_hierarchy_metamap AS
WITH RECURSIVE r_data_hierarchy(parent_coll_name, coll_name, coll_id, object_id, object_path, level)
AS (SELECT data_coll.coll_name, cast(null as varchar), cast(null as bigint), data_base.data_id, 
           data_coll.coll_name || '/' || data_base.data_name, 1 
    FROM r_data_main data_base, r_coll_main data_coll 
    WHERE data_base.coll_id = data_coll.coll_id
    UNION ALL SELECT coll_iter.parent_coll_name, coll_iter.coll_name, coll_iter.coll_id, 
                     coll_hierarchy_iter.object_id, coll_hierarchy_iter.object_path, coll_hierarchy_iter.level + 1 
                     FROM r_coll_main coll_iter, r_data_hierarchy coll_hierarchy_iter
                     WHERE coll_iter.coll_name != '/' AND coll_iter.coll_name = coll_hierarchy_iter.parent_coll_name)
SELECT data_hierarchy.object_id, data_hierarchy.object_path, metamap.meta_id, data_hierarchy.coll_id, data_hierarchy.level 
FROM r_data_hierarchy data_hierarchy, r_objt_metamap metamap 
WHERE ((data_hierarchy.object_id = metamap.object_id and data_hierarchy.coll_id is null) or 
       data_hierarchy.coll_id = metamap.object_id);
CREATE UNIQUE INDEX r_data_hierarchy_metamap_unique ON r_data_hierarchy_metamap (object_id, meta_id, level);       
COMMENT ON COLUMN r_data_hierarchy_metamap.object_id IS 
                  'Data object Hierarchy ID: r_data_main.data_id';
COMMENT ON COLUMN r_data_hierarchy_metamap.object_path IS 
                  'Data object Hierarchy Path:  r_coll_main.coll_name / r_data_main.data_name';
COMMENT ON COLUMN r_data_hierarchy_metamap.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_data_hierarchy_metamap.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';
COMMENT ON COLUMN r_data_hierarchy_metamap.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the data-object level';

-- Hierarchy data meta_main
CREATE MATERIALIZED VIEW r_data_hierarchy_meta_main AS
SELECT data_hierarchy_metamap.object_id, data_hierarchy_metamap.object_path, data_hierarchy_metamap.coll_id, 
       data_hierarchy_metamap.meta_id, data_hierarchy_metamap.level, 
       case when(data_hierarchy_metamap.level = 1) then 'DataObject' else coll_type_metadata.meta_attr_value end as level_label,
       meta_main.meta_attr_name, meta_main.meta_attr_value, meta_main.meta_attr_unit
FROM r_data_hierarchy_metamap data_hierarchy_metamap left join r_meta_main meta_main using (meta_id) 
     left outer join (r_objt_metamap metamap join r_meta_main coll_type_metadata 
                      on metamap.meta_id = coll_type_metadata.meta_id and coll_type_metadata.meta_attr_name = 'collection_type')
                on metamap.object_id = data_hierarchy_metamap.coll_id
ORDER BY data_hierarchy_metamap.object_id;
CREATE UNIQUE INDEX r_data_hierarchy_meta_main_unique ON r_data_hierarchy_meta_main (object_id, meta_id, level);
CREATE INDEX r_data_hierarchy_meta_main_path_query ON r_data_hierarchy_meta_main (object_path);
CREATE INDEX r_data_hierarchy_meta_main_id_query ON r_data_hierarchy_meta_main (object_id);
CREATE INDEX r_data_hierarchy_meta_main_metadata_query_level ON r_data_hierarchy_meta_main (meta_attr_name, meta_attr_value, level);
CREATE INDEX r_data_hierarchy_meta_main_metadata_query_level_label ON r_data_hierarchy_meta_main (meta_attr_name, meta_attr_value, level_label);
COMMENT ON COLUMN r_data_hierarchy_meta_main.object_id IS 
                  'Data object Hierarchy ID: r_data_main.data_id';
COMMENT ON COLUMN r_data_hierarchy_metamap.object_path IS 
                  'Data object Hierarchy Path:  r_coll_main.coll_name / r_data_main.data_name';
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_data_hierarchy_meta_main.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';
COMMENT ON COLUMN r_data_hierarchy_meta_main.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the data-object level';
COMMENT ON COLUMN r_data_hierarchy_meta_main.level_label IS 
                  'The level label of the metadata in the hierarchy which is the collection_type value at the same level. "DataObject" if the metadata is associated with the data object itself';
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name';
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_attr_unit IS 
                  'Metadata unit: r_meta_main.meta_attr_unit';

-- Hierarchy collection metamap.
CREATE MATERIALIZED VIEW r_coll_hierarchy_metamap AS
WITH RECURSIVE r_coll_hierarchy(parent_coll_name, coll_name, coll_id, object_id, object_path, level)
AS (SELECT coll_base.parent_coll_name, coll_base.coll_name, coll_base.coll_id, coll_base.coll_id, coll_base.coll_name, 1 
    FROM r_coll_main coll_base
    UNION ALL SELECT coll_iter.parent_coll_name, coll_iter.coll_name, coll_iter.coll_id, 
                     coll_hierarchy_iter.object_id, coll_hierarchy_iter.object_path, coll_hierarchy_iter.level + 1 
                     FROM r_coll_main coll_iter, r_coll_hierarchy coll_hierarchy_iter
                     WHERE coll_iter.coll_name != '/' AND coll_iter.coll_name = coll_hierarchy_iter.parent_coll_name)
SELECT coll_hierarchy.object_id, coll_hierarchy.object_path, metamap.meta_id, coll_hierarchy.coll_id, coll_hierarchy.level 
FROM r_coll_hierarchy coll_hierarchy, r_objt_metamap metamap 
WHERE coll_hierarchy.coll_id = metamap.object_id;
CREATE UNIQUE INDEX r_coll_hierarchy_metamap_unique ON r_coll_hierarchy_metamap (object_id, meta_id, level);
COMMENT ON COLUMN r_coll_hierarchy_metamap.object_id IS 
                  'Collection Hierarchy ID: r_coll_main.coll_id';
COMMENT ON COLUMN r_coll_hierarchy_metamap.object_path IS 
                  'Collection Hierarchy Path: r_coll_main.coll_name';
COMMENT ON COLUMN r_coll_hierarchy_metamap.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_coll_hierarchy_metamap.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Same as object_id if the metadata is associated with the collection itself';
COMMENT ON COLUMN r_coll_hierarchy_metamap.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the collection level';

-- Hierarchy collection meta_main
CREATE MATERIALIZED VIEW r_coll_hierarchy_meta_main AS
SELECT coll_hierarchy_metamap.object_id, coll_hierarchy_metamap.object_path, coll_hierarchy_metamap.coll_id, 
       coll_hierarchy_metamap.meta_id, coll_hierarchy_metamap.level, coll_type_metadata.meta_attr_value as level_label,
       meta_main.meta_attr_name, meta_main.meta_attr_value, meta_main.meta_attr_unit
FROM r_coll_hierarchy_metamap coll_hierarchy_metamap left join r_meta_main meta_main using (meta_id) 
     left outer join (r_objt_metamap metamap join r_meta_main coll_type_metadata 
                      on metamap.meta_id = coll_type_metadata.meta_id and coll_type_metadata.meta_attr_name = 'collection_type')
                on metamap.object_id = coll_hierarchy_metamap.coll_id
ORDER BY coll_hierarchy_metamap.object_id;

CREATE UNIQUE INDEX r_coll_hierarchy_meta_main_unique ON r_coll_hierarchy_meta_main (object_id, meta_id, level);
CREATE INDEX r_coll_hierarchy_meta_main_path_query ON r_coll_hierarchy_meta_main (object_path);
CREATE INDEX r_coll_hierarchy_meta_main_id_query ON r_coll_hierarchy_meta_main (object_id);
CREATE INDEX r_coll_hierarchy_meta_main_metadata_query_level ON r_coll_hierarchy_meta_main (meta_attr_name, meta_attr_value, level);
CREATE INDEX r_coll_hierarchy_meta_main_metadata_query_level_label ON r_coll_hierarchy_meta_main (meta_attr_name, meta_attr_value, level_label);
COMMENT ON COLUMN r_coll_hierarchy_meta_main.object_id IS 
                  'Collection Hierarchy ID: r_coll_main.coll_id';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.object_path IS 
                  'Collection Hierarchy Path: r_coll_main.coll_name';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Same as object_id if the metadata is associated with the collection itself';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the collection level';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.level_label IS 
                  'The level label of the metadata in the hierarchy which is the collection_type value at the same level';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_attr_unit IS 
                  'Metadata unit: r_meta_main.meta_attr_unit';
                  
-- Hierarchy data meta_attr_name
CREATE MATERIALIZED VIEW r_data_hierarchy_meta_attr_name AS
SELECT level_label, meta_attr_name, array_agg(distinct object_id) as object_ids 
FROM r_data_hierarchy_meta_main 
GROUP BY level_label, meta_attr_name;
CREATE UNIQUE INDEX r_data_hierarchy_meta_attr_name_unique ON r_data_hierarchy_meta_attr_name (level_label, meta_attr_name);   

-- Hierarchy collection meta_attr_name
CREATE MATERIALIZED VIEW r_coll_hierarchy_meta_attr_name AS
SELECT level_label, meta_attr_name, array_agg(distinct object_id) as object_ids 
FROM r_coll_hierarchy_meta_main 
GROUP BY level_label, meta_attr_name;
CREATE UNIQUE INDEX r_coll_hierarchy_meta_attr_name_unique ON r_coll_hierarchy_meta_attr_name (level_label, meta_attr_name);

-- Numerical comparison functions based on string input
CREATE OR REPLACE FUNCTION num_less_than(text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value NUMERIC;
DECLARE value NUMERIC;
BEGIN
    attr_value = $1::NUMERIC;
    value = $2::NUMERIC;
    RETURN attr_value < value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION num_less_or_equal(text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value NUMERIC;
DECLARE value NUMERIC;
BEGIN
    attr_value = $1::NUMERIC;
    value = $2::NUMERIC;
    RETURN attr_value <= value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION num_greater_than(text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value NUMERIC;
DECLARE value NUMERIC;
BEGIN
    attr_value = $1::NUMERIC;
    value = $2::NUMERIC;
    RETURN attr_value > value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION num_greater_or_equal(text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value NUMERIC;
DECLARE value NUMERIC;
BEGIN
    attr_value = $1::NUMERIC;
    value = $2::NUMERIC;
    RETURN attr_value >= value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_less_than(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) < to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_greater_than(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) > to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_less_or_equal(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) <= to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION timestamp_greater_or_equal(text, text, text) RETURNS BOOLEAN AS $$
BEGIN
    RETURN to_timestamp($1, $3) >= to_timestamp($2, $3);
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;

-- Refresh views
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_metamap;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_meta_main;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_metamap;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_meta_main;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_meta_attr_name;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_meta_attr_name;

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

