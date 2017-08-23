--
-- hpc_report_views.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
-- @version $Id$
--
DROP MATERIALIZED VIEW IF EXISTS r_report_meta_map;
CREATE MATERIALIZED VIEW r_report_meta_map
AS (
select distinct a.object_id from public.r_objt_metamap a 
inner join public.r_meta_main b on  a.meta_id=b.meta_id 
inner join public.r_data_main c on a.object_id = c.data_id 
);

DROP MATERIALIZED VIEW IF EXISTS r_report_data_meta_main;
CREATE MATERIALIZED VIEW r_report_data_meta_main
AS (
select a.meta_attr_value attr, a.meta_attr_name, a.meta_id cnt from r_meta_main a
inner join r_objt_metamap b on a.meta_id=b.meta_id 
inner join r_data_main c on b.object_id=c.data_id
);

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_meta_main;
CREATE MATERIALIZED VIEW r_report_coll_meta_main
AS (
select a.meta_attr_value attr, a.meta_attr_name, a.meta_id cnt from r_meta_main a
inner join r_objt_metamap b on a.meta_id=b.meta_id 
inner join r_coll_main c on b.object_id=c.coll_id
);

DROP MATERIALIZED VIEW IF EXISTS r_report_meta_main;
CREATE MATERIALIZED VIEW r_report_meta_main
AS (
select a.meta_attr_value attr, a.meta_attr_name, a.meta_id, b.object_id, a.create_ts from r_meta_main a
inner join r_objt_metamap b on a.meta_id=b.meta_id 
);

DROP MATERIALIZED VIEW IF EXISTS r_report_source_file_size;
CREATE MATERIALIZED VIEW r_report_source_file_size
AS (
SELECT a.meta_attr_value, b.object_id, c.create_ts 
FROM public.r_meta_main a 
inner join r_objt_metamap b on a.meta_id=b.meta_id 
inner join r_data_main c on b.object_id=c.data_id 
where a.meta_attr_name = 'source_file_size'
);

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_doc;
CREATE MATERIALIZED VIEW r_report_registered_by_doc
AS (
select distinct a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c  
where a.meta_id=b.meta_id and b.object_id = c.data_id and a.meta_attr_name='registered_by_doc'
);

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by;
CREATE MATERIALIZED VIEW r_report_registered_by
AS (
select distinct a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c  
where a.meta_id=b.meta_id and b.object_id = c.data_id and a.meta_attr_name='registered_by' 
);

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by;
CREATE MATERIALIZED VIEW r_report_coll_registered_by
AS (
select distinct a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c  
where a.meta_id=b.meta_id and b.object_id = c.coll_id and a.meta_attr_name='registered_by' 
);

DROP MATERIALIZED VIEW IF EXISTS r_report_collection_type;
CREATE MATERIALIZED VIEW r_report_collection_type
AS (
SELECT a.meta_attr_value, a.meta_attr_name, b.object_id, c.coll_id, c.create_ts 
FROM public.r_meta_main a 
inner join r_objt_metamap b on a.meta_id=b.meta_id 
inner join r_coll_main c on b.object_id=c.coll_id 
where a.meta_attr_name = 'collection_type'
);

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by_doc;
CREATE MATERIALIZED VIEW r_report_coll_registered_by_doc
AS (
select distinct a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c  
where a.meta_id=b.meta_id and b.object_id = c.coll_id and a.meta_attr_name='registered_by_doc'  
);

DROP MATERIALIZED VIEW IF EXISTS r_report_data_objects;
CREATE MATERIALIZED VIEW r_report_data_objects
AS (
SELECT a.data_id, c.meta_attr_name, c.meta_attr_value, a.create_ts FROM public.r_data_main a
inner join public.r_objt_metamap b on a.data_id=b.object_id 
inner join public.r_meta_main c on b.meta_id=c.meta_id
);

