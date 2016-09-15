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
-- @version $Id$
--


-- Drop all views first.
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_meta_main;
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_meta_main_ovrd;
DROP MATERIALIZED VIEW IF EXISTS r_coll_hierarchy_metamap;
DROP MATERIALIZED VIEW IF EXISTS r_data_hierarchy_meta_main;
DROP MATERIALIZED VIEW IF EXISTS r_data_hierarchy_meta_main_ovrd;
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

-- Hierarchy data meta_main w/ override rules
CREATE MATERIALIZED VIEW r_data_hierarchy_meta_main_ovrd AS
SELECT data_hierarchy_metamap.object_id, 
	   (array_agg(data_hierarchy_metamap.object_path order by data_hierarchy_metamap.level))[1] AS object_path, 
       (array_agg(data_hierarchy_metamap.coll_id order by data_hierarchy_metamap.level))[1] AS coll_id, 
       (array_agg(data_hierarchy_metamap.meta_id order by data_hierarchy_metamap.level))[1] AS meta_id, 
       min(data_hierarchy_metamap.level) AS level, 
       meta_main.meta_attr_name,
       (array_agg(meta_main.meta_attr_value order by data_hierarchy_metamap.level))[1] AS meta_attr_value
FROM r_meta_main meta_main, r_data_hierarchy_metamap data_hierarchy_metamap 
WHERE data_hierarchy_metamap.meta_id = meta_main.meta_id 
GROUP BY data_hierarchy_metamap.object_id, meta_main.meta_attr_name 
ORDER BY data_hierarchy_metamap.object_id;
CREATE UNIQUE INDEX r_data_hierarchy_meta_main_ovrd_unique ON r_data_hierarchy_meta_main_ovrd (object_id, meta_id, level);
CREATE INDEX r_data_hierarchy_meta_main_ovrd_query ON r_data_hierarchy_meta_main_ovrd (meta_attr_name, meta_attr_value);
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.object_id IS 
                  'Data object Hierarchy ID: r_data_main.data_id';
COMMENT ON COLUMN r_data_hierarchy_metamap.object_path IS 
                  'Data object Hierarchy Path:  r_coll_main.coll_name / r_data_main.data_name';
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Null if the metadata is associated with the data object itself';
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the data-object level';
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name. Note: w/ override rules, metadata name is unique within the hierarchy, associated with the metadata value of the lowest level';
COMMENT ON COLUMN r_data_hierarchy_meta_main_ovrd.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';

-- Hierarchy data meta_main w/o override rules
CREATE MATERIALIZED VIEW r_data_hierarchy_meta_main AS
SELECT data_hierarchy_metamap.object_id, data_hierarchy_metamap.object_path, data_hierarchy_metamap.coll_id, 
       data_hierarchy_metamap.meta_id, data_hierarchy_metamap.level, meta_main.meta_attr_name, meta_main.meta_attr_value
FROM r_meta_main meta_main, r_data_hierarchy_metamap data_hierarchy_metamap 
WHERE data_hierarchy_metamap.meta_id = meta_main.meta_id 
ORDER BY data_hierarchy_metamap.object_id;
CREATE UNIQUE INDEX r_data_hierarchy_meta_main_unique ON r_data_hierarchy_meta_main (object_id, meta_id, level);
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
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name';
COMMENT ON COLUMN r_data_hierarchy_meta_main.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';

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

-- Hierarchy collection meta_main w/ override rules
CREATE MATERIALIZED VIEW r_coll_hierarchy_meta_main_ovrd AS
SELECT coll_hierarchy_metamap.object_id, 
       (array_agg(coll_hierarchy_metamap.object_path order by coll_hierarchy_metamap.level))[1] AS object_path, 
       (array_agg(coll_hierarchy_metamap.coll_id order by coll_hierarchy_metamap.level))[1] AS coll_id, 
       (array_agg(coll_hierarchy_metamap.meta_id order by coll_hierarchy_metamap.level))[1] AS meta_id, 
       min(coll_hierarchy_metamap.level) AS level, 
       meta_main.meta_attr_name,
       (array_agg(meta_main.meta_attr_value order by coll_hierarchy_metamap.level))[1] AS meta_attr_value
FROM r_meta_main meta_main, r_coll_hierarchy_metamap coll_hierarchy_metamap 
WHERE coll_hierarchy_metamap.meta_id = meta_main.meta_id 
GROUP BY coll_hierarchy_metamap.object_id, meta_main.meta_attr_name 
ORDER BY coll_hierarchy_metamap.object_id;
CREATE UNIQUE INDEX r_coll_hierarchy_meta_main_ovrd_unique ON r_coll_hierarchy_meta_main_ovrd (object_id, meta_id, level);
CREATE INDEX r_coll_hierarchy_meta_main_ovrd_query ON r_coll_hierarchy_meta_main_ovrd (meta_attr_name, meta_attr_value);
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.object_id IS 
                  'Collection Hierarchy ID: r_coll_main.coll_id';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.object_path IS 
                  'Collection Hierarchy Path: r_coll_main.coll_name';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.meta_id IS 
                  'Metadata ID: r_meta_main.meta_id';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.coll_id IS 
                  'Collection (in the hierarchy) ID: r_coll_main.coll_id. Same as object_id if the metadata is associated with the collection itself';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.level IS 
                  'The level of the metadata in the hierarchy, starting with 1 at the collection level';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name. Note: w/ override rules, metadata name is unique within the hierarchy, associated with the metadata value of the lowest level';
COMMENT ON COLUMN r_coll_hierarchy_meta_main_ovrd.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';

-- Hierarchy collection meta_main w/o override rules
CREATE MATERIALIZED VIEW r_coll_hierarchy_meta_main AS
SELECT coll_hierarchy_metamap.object_id, coll_hierarchy_metamap.object_path, coll_hierarchy_metamap.coll_id, 
       coll_hierarchy_metamap.meta_id, coll_hierarchy_metamap.level, meta_main.meta_attr_name, meta_main.meta_attr_value
FROM r_meta_main meta_main, r_coll_hierarchy_metamap coll_hierarchy_metamap 
WHERE coll_hierarchy_metamap.meta_id = meta_main.meta_id 
ORDER BY coll_hierarchy_metamap.object_id;
CREATE UNIQUE INDEX r_coll_hierarchy_meta_main_unique ON r_coll_hierarchy_meta_main (object_id, meta_id, level);
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
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_attr_name IS 
                  'Metadata attribute: r_meta_main.meta_attr_name';
COMMENT ON COLUMN r_coll_hierarchy_meta_main.meta_attr_value IS 
                  'Metadata value: r_meta_main.meta_attr_value';
                  
-- Numerical comparison functions based on string input
CREATE OR REPLACE FUNCTION num_less_than(text, text) RETURNS BOOLEAN AS $$
DECLARE attribute NUMERIC;
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
DECLARE attribute NUMERIC;
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

CREATE OR REPLACE FUNCTION num_greater_or_equal(text, text) RETURNS BOOLEAN AS $$
DECLARE attr_value NUMERIC;
DECLARE value NUMERIC;
BEGIN
    attr_value = $1::NUMERIC;
    value = $2::NUMERIC;
    RETURN attribute >= value;
EXCEPTION WHEN others THEN
    RETURN FALSE;
END;
$$
STRICT
LANGUAGE plpgsql IMMUTABLE;


-- Refresh views
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_meta_main;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_meta_main_ovrd;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_coll_hierarchy_metamap;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_meta_main;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_meta_main_ovrd;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY r_data_hierarchy_metamap;

