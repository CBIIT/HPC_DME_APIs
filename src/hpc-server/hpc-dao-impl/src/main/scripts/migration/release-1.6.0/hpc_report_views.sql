DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by_path;
CREATE MATERIALIZED VIEW r_report_coll_registered_by_path
AS (
select distinct c.coll_name, a.meta_attr_name, a.meta_attr_value, c.coll_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c
where a.meta_id=b.meta_id and b.object_id = c.coll_id  
);
CREATE UNIQUE INDEX r_report_coll_registered_by_path_uidx1 ON r_report_coll_registered_by_path(meta_attr_name, meta_attr_value, coll_id, coll_name);

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_path;
CREATE MATERIALIZED VIEW r_report_registered_by_path
AS (
select distinct d.coll_name, a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c, r_coll_main d 
	where a.meta_id=b.meta_id and b.object_id = c.data_id and c.coll_id = d.coll_id
);
CREATE UNIQUE INDEX r_report_registered_by_path_uidx1 ON r_report_registered_by_path(meta_attr_name, meta_attr_value, object_id, coll_name);

DROP MATERIALIZED VIEW IF EXISTS r_report_collection_path;
CREATE MATERIALIZED VIEW r_report_collection_path
AS (
select distinct d.coll_name, b.object_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c, r_coll_main d 
	where a.meta_id=b.meta_id and b.object_id = c.data_id and c.coll_id = d.coll_id
);
CREATE UNIQUE INDEX r_report_collection_path_uidx1 ON r_report_collection_path(object_id, coll_name);

