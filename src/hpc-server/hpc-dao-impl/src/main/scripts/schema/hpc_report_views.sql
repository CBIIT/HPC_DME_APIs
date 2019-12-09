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

DROP MATERIALIZED VIEW IF EXISTS r_report_meta_main;
CREATE MATERIALIZED VIEW r_report_meta_main
AS (
	select a.meta_attr_value, a.meta_attr_name, a.meta_id, b.object_id, a.create_ts from r_meta_main a
	inner join r_objt_metamap b on a.meta_id=b.meta_id 
);
CREATE UNIQUE INDEX r_report_meta_main_uidx1 ON r_report_meta_main(meta_attr_value, meta_attr_name, meta_id, object_id);

DROP MATERIALIZED VIEW IF EXISTS r_report_source_file_size;
CREATE MATERIALIZED VIEW r_report_source_file_size
AS (
	SELECT a.meta_attr_name, a.meta_attr_value, b.object_id, c.create_ts 
	FROM public.r_meta_main a 
	inner join r_objt_metamap b on a.meta_id=b.meta_id 
	inner join r_data_main c on b.object_id=c.data_id 
	where a.meta_attr_name = 'source_file_size'
);
CREATE UNIQUE INDEX r_report_source_file_size_uidx1 ON r_report_source_file_size(meta_attr_value, meta_attr_name, object_id);

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_doc;
CREATE MATERIALIZED VIEW r_report_registered_by_doc
AS (
select distinct d."DOC", a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c, public."HPC_DATA_MANAGEMENT_CONFIGURATION" d 
	where a.meta_id=b.meta_id and b.object_id = c.data_id and a.meta_attr_name='configuration_id' and a.meta_attr_value=d."ID"
);
CREATE UNIQUE INDEX r_report_registered_by_doc_uidx1 ON r_report_registered_by_doc(meta_attr_name, meta_attr_value, object_id, "DOC");

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_basepath;
CREATE MATERIALIZED VIEW r_report_registered_by_basepath
AS (
select distinct d."BASE_PATH", a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c, public."HPC_DATA_MANAGEMENT_CONFIGURATION" d 
	where a.meta_id=b.meta_id and b.object_id = c.data_id and a.meta_attr_name='configuration_id' and a.meta_attr_value=d."ID"
);
CREATE UNIQUE INDEX r_report_registered_by_basepath_uidx1 ON r_report_registered_by_basepath(meta_attr_name, meta_attr_value, object_id, "BASE_PATH");

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by;
CREATE MATERIALIZED VIEW r_report_registered_by
AS (
select distinct a.meta_attr_name, a.meta_attr_value, b.object_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_data_main c  
where a.meta_id=b.meta_id and b.object_id = c.data_id and a.meta_attr_name='registered_by' 
);
CREATE UNIQUE INDEX r_report_registered_by_uidx1 ON r_report_registered_by(meta_attr_name, meta_attr_value, object_id);

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by;
CREATE MATERIALIZED VIEW r_report_coll_registered_by
AS (
select distinct a.meta_attr_name, a.meta_attr_value, c.coll_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c  
where a.meta_id=b.meta_id and b.object_id = c.coll_id and a.meta_attr_name='registered_by' 
);
CREATE UNIQUE INDEX r_report_coll_registered_by_uidx1 ON r_report_coll_registered_by(meta_attr_name, meta_attr_value, coll_id);

DROP MATERIALIZED VIEW IF EXISTS r_report_collection_type;
CREATE MATERIALIZED VIEW r_report_collection_type
AS (
SELECT a.meta_attr_value, a.meta_attr_name, b.object_id, c.coll_id, c.coll_name, c.create_ts 
FROM public.r_meta_main a 
inner join r_objt_metamap b on a.meta_id=b.meta_id 
inner join r_coll_main c on b.object_id=c.coll_id 
where a.meta_attr_name = 'collection_type'
);
CREATE UNIQUE INDEX r_report_collection_type_uidx1 ON r_report_collection_type(meta_attr_name, meta_attr_value, object_id);

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by_doc;
CREATE MATERIALIZED VIEW r_report_coll_registered_by_doc
AS (
select distinct d."DOC", a.meta_attr_name, a.meta_attr_value, c.coll_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c, public."HPC_DATA_MANAGEMENT_CONFIGURATION" d
where a.meta_id=b.meta_id and b.object_id = c.coll_id and a.meta_attr_name='configuration_id' and a.meta_attr_value=d."ID" 
);
CREATE UNIQUE INDEX r_report_coll_registered_by_doc_uidx1 ON r_report_coll_registered_by_doc(meta_attr_name, meta_attr_value, coll_id, "DOC");

DROP MATERIALIZED VIEW IF EXISTS r_report_coll_registered_by_basepath;
CREATE MATERIALIZED VIEW r_report_coll_registered_by_basepath
AS (
select distinct d."BASE_PATH", a.meta_attr_name, a.meta_attr_value, c.coll_id, b.meta_id, c.create_ts from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c, public."HPC_DATA_MANAGEMENT_CONFIGURATION" d
where a.meta_id=b.meta_id and b.object_id = c.coll_id and a.meta_attr_name='configuration_id' and a.meta_attr_value=d."ID" 
);
CREATE UNIQUE INDEX r_report_coll_registered_by_basepath_uidx1 ON r_report_coll_registered_by_basepath(meta_attr_name, meta_attr_value, coll_id, "BASE_PATH");

DROP MATERIALIZED VIEW IF EXISTS r_report_data_objects;
CREATE MATERIALIZED VIEW r_report_data_objects
AS (
SELECT a.data_id, c.meta_attr_name, c.meta_attr_value, a.create_ts FROM public.r_data_main a
inner join public.r_objt_metamap b on a.data_id=b.object_id 
inner join public.r_meta_main c on b.meta_id=c.meta_id
);
CREATE UNIQUE INDEX r_report_data_objects_uidx1 ON r_report_data_objects(meta_attr_name, meta_attr_value, data_id);

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

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_audit;
CREATE MATERIALIZED VIEW r_report_registered_by_audit
AS (
select
  size.data_id,
  basepath."BASE_PATH",
  doc."DOC",
  datapath.data_path,
  CAST(size.meta_attr_value AS BIGINT)      as "SIZE",
  user_id.meta_attr_value   as "USER_ID",
  type.meta_attr_value      as "DATA_TRANSFER_TYPE",
  CAST(created.meta_attr_value AS TIMESTAMP)   as "CREATED",
  CAST(completed.meta_attr_value AS TIMESTAMP) as "COMPLETED"
from
  r_report_data_objects size, r_report_data_objects user_id, r_report_data_objects type, r_report_data_objects created,
  r_report_data_objects completed, r_report_registered_by_basepath basepath, r_report_registered_by_doc doc,
  r_data_main datapath
where doc.object_id = size.data_id and basepath.object_id = size.data_id and datapath.data_id = doc.object_id and
      size.meta_attr_name = 'source_file_size'
      and user_id.meta_attr_name = 'registered_by'
      and type.meta_attr_name = 'data_transfer_type'
      and created.meta_attr_name = 'data_transfer_started'
      and completed.meta_attr_name = 'data_transfer_completed'
      and size.data_id = user_id.data_id and size.data_id = type.data_id and size.data_id = created.data_id and
      size.data_id = completed.data_id
);
CREATE UNIQUE INDEX r_report_registered_by_audit_uidx1 ON r_report_registered_by_audit(data_id);


