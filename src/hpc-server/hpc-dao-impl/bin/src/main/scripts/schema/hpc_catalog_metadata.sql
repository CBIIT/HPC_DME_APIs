--
-- hpc_catalog_metadata.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--

DROP TABLE IF EXISTS public."HPC_CATALOG_ATTRIBUTE";
CREATE TABLE public."HPC_CATALOG_ATTRIBUTE"
(
  "level_label" text NOT NULL,
  "meta_attr_name" text NOT NULL,
  CONSTRAINT "HPC_CATALOG_ATTRIBUTE_pkey" PRIMARY KEY ("level_label", "meta_attr_name")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_CATALOG_ATTRIBUTE" IS 
                 'Catalog metadata attributes';
COMMENT ON COLUMN public."HPC_CATALOG_ATTRIBUTE"."level_label" IS 
                  'Collection level of the metadata attribute';
COMMENT ON COLUMN public."HPC_CATALOG_ATTRIBUTE"."meta_attr_name" IS 
                  'The metadata attribute name';
                 
DROP MATERIALIZED VIEW IF EXISTS r_catalog_meta_main;
CREATE MATERIALIZED VIEW r_catalog_meta_main
AS (
	select
		config."DOC",
		config."BASE_PATH",
		meta_main.object_id,
		meta_main.object_path,
		meta_main.meta_id,
		meta_main.meta_attr_name,
		meta_main.meta_attr_value,
		meta_main.meta_attr_unit
	from r_coll_hierarchy_meta_main meta_main, public."HPC_DATA_MANAGEMENT_CONFIGURATION" config, "HPC_CATALOG_ATTRIBUTE" catalog,
		(r_meta_main meta_main
			join r_objt_metamap metamap
				on meta_main.meta_attr_name = 'configuration_id' and metamap.meta_id = meta_main.meta_id) config_meta
	where meta_main.object_id in (
		select object_id
		from r_coll_hierarchy_meta_main
		where meta_attr_name = 'access' and lower(meta_attr_value) in ('controlled access','open access') and level_label='Project' and level=1)
			and catalog.level_label=meta_main.level_label and catalog.meta_attr_name=meta_main.meta_attr_name
			and config_meta.meta_attr_value = config."ID" and config_meta.object_id = meta_main.object_id
	order by meta_main.coll_id
);
CREATE UNIQUE INDEX r_catalog_meta_main_uidx1 ON r_catalog_meta_main(meta_attr_value, meta_attr_name, meta_id, object_id);

COMMENT ON COLUMN r_catalog_meta_main."DOC" IS 
                  'The DOC of the catalog entry';
COMMENT ON COLUMN r_catalog_meta_main."BASE_PATH" IS 
                  'The base path of the catalog entry';
COMMENT ON COLUMN r_catalog_meta_main.object_id IS 
                  'Collection ID: r_coll_hierarchy_meta_main.object_id';
COMMENT ON COLUMN r_catalog_meta_main.object_path IS 
                  'Collection Path: r_coll_hierarchy_meta_main.object_path';
COMMENT ON COLUMN r_catalog_meta_main.meta_id IS 
                  'Metadata ID: r_coll_hierarchy_meta_main.meta_id';
COMMENT ON COLUMN r_catalog_meta_main.meta_attr_name IS 
                  'Metadata attribute: r_coll_hierarchy_meta_main.meta_attr_name';
COMMENT ON COLUMN r_catalog_meta_main.meta_attr_value IS 
                  'Metadata value: r_coll_hierarchy_meta_main.meta_attr_value';
COMMENT ON COLUMN r_catalog_meta_main.meta_attr_unit IS 
                  'Metadata unit: r_coll_hierarchy_meta_main.meta_attr_unit';