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
-- @version $Id$
--
           
DROP MATERIALIZED VIEW r_coll_hierarchy_meta_main;

create materialized view irods.r_coll_hierarchy_meta_main as
SELECT coll_hierarchy_metamap.object_id,
       coll_hierarchy_metamap.object_path,
       coll_hierarchy_metamap.coll_id,
       coll_hierarchy_metamap.meta_id,
       coll_hierarchy_metamap.data_level,
       coll_type_metadata.meta_attr_value AS level_label,
       meta_main.meta_attr_name,
       meta_main.meta_attr_value,
       meta_main.meta_attr_unit
FROM r_coll_hierarchy_metamap coll_hierarchy_metamap
         LEFT JOIN r_meta_main meta_main ON coll_hierarchy_metamap.meta_id = meta_main.meta_id
         LEFT JOIN (r_objt_metamap metamap
    JOIN r_meta_main coll_type_metadata ON metamap.meta_id = coll_type_metadata.meta_id AND
                                           cast(coll_type_metadata.meta_attr_name as varchar2(250)) = cast('collection_type' as varchar2(50)))
                   ON metamap.object_id = coll_hierarchy_metamap.coll_id
UNION
select coll_hierarchy_metamap.object_id,
       coll_hierarchy_metamap.object_path,
       coll_hierarchy_metamap.coll_id,
       0 as meta_id,
       1 as data_level,
       meta_main.meta_attr_value AS level_label,
       'collection_size' AS meta_attr_name,
       to_char(sum(report_collection_size.totalSize)) AS meta_attr_value,
       'EMPTY_ATTR_UNIT' AS meta_attr_unit
FROM r_coll_hierarchy_metamap coll_hierarchy_metamap
         LEFT JOIN r_meta_main meta_main ON coll_hierarchy_metamap.meta_id = meta_main.meta_id
         LEFT JOIN r_report_collection_size report_collection_size ON coll_hierarchy_metamap.OBJECT_PATH = report_collection_size.coll_name
WHERE coll_hierarchy_metamap.data_level = 1 AND meta_main.META_ATTR_NAME='collection_type'
  and (report_collection_size.coll_name like coll_hierarchy_metamap.OBJECT_PATH || '%'
    or report_collection_size.coll_name = coll_hierarchy_metamap.OBJECT_PATH)
group by coll_hierarchy_metamap.object_id,coll_hierarchy_metamap.object_path,coll_hierarchy_metamap.coll_id,meta_main.meta_attr_value
ORDER BY object_id;

comment on column irods.r_coll_hierarchy_meta_main.object_id is 'Collection Hierarchy ID: r_coll_main.coll_id';

comment on column irods.r_coll_hierarchy_meta_main.object_path is 'Collection Hierarchy Path: r_coll_main.coll_name';

comment on column irods.r_coll_hierarchy_meta_main.coll_id is 'Collection (in the hierarchy) ID: r_coll_main.coll_id. Same as object_id if the metadata is associated with the collection itself';

comment on column irods.r_coll_hierarchy_meta_main.meta_id is 'Metadata ID: r_meta_main.meta_id';

comment on column irods.r_coll_hierarchy_meta_main.data_level is 'The level of the metadata in the hierarchy, starting with 1 at the collection level';

comment on column irods.r_coll_hierarchy_meta_main.level_label is 'The level label of the metadata in the hierarchy which is the collection_type value at the same level';

comment on column irods.r_coll_hierarchy_meta_main.meta_attr_name is 'Metadata attribute: r_meta_main.meta_attr_name';

comment on column irods.r_coll_hierarchy_meta_main.meta_attr_value is 'Metadata value: r_meta_main.meta_attr_value';

comment on column irods.r_coll_hierarchy_meta_main.meta_attr_unit is 'Metadata unit: r_meta_main.meta_attr_unit';

create unique index r_coll_hierarchy_meta_main_unique
    on irods.r_coll_hierarchy_meta_main (object_id, meta_id, data_level);

create index r_coll_hierarchy_meta_main_path_query
    on irods.r_coll_hierarchy_meta_main (object_path);

create index r_coll_hierarchy_meta_main_id_query
    on irods.r_coll_hierarchy_meta_main (object_id);

create index r_coll_hierarchy_meta_main_metadata_query_level
    on irods.r_coll_hierarchy_meta_main (meta_attr_name, meta_attr_value, data_level);

create index r_coll_hierarchy_meta_main_metadata_query_level_label
    on irods.r_coll_hierarchy_meta_main (meta_attr_name, level_label);

