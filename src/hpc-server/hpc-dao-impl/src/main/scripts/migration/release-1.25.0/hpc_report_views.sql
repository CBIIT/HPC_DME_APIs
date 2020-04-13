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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--

DROP MATERIALIZED VIEW IF EXISTS r_report_registered_by_audit;
CREATE MATERIALIZED VIEW r_report_registered_by_audit
AS (
select
  data.data_id,
  basepath."BASE_PATH",
  doc."DOC",
  data.data_path,
  CAST(size.meta_attr_value AS BIGINT)      as "SIZE",
  user_id.meta_attr_value   as "USER_ID",
  transferMethod.meta_attr_value      as "DATA_TRANSFER_METHOD",
  transferType.meta_attr_value      as "DATA_TRANSFER_TYPE",
  sourceEndpoint.meta_attr_value	as "SOURCE_ENDPOINT",
  CAST(created.meta_attr_value AS TIMESTAMP)   as "CREATED",
  CAST(completed.meta_attr_value AS TIMESTAMP) as "COMPLETED"
from
  r_data_main data
  join r_report_registered_by_basepath basepath on data.data_id=basepath.object_id
  join r_report_registered_by_doc doc on data.data_id=doc.object_id
  left join (select * from r_report_data_objects where meta_attr_name = 'source_file_size') size on data.data_id=size.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'registered_by') user_id on data.data_id=user_id.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'data_transfer_method') transferMethod on data.data_id=transferMethod.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'data_transfer_type') transferType on data.data_id=transferType.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'data_transfer_started') created on data.data_id=created.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'data_transfer_completed') completed on data.data_id=completed.data_id
  left join (select * from r_report_data_objects where meta_attr_name = 'source_file_container_id') sourceEndpoint on data.data_id=sourceEndpoint.data_id
);
CREATE UNIQUE INDEX r_report_registered_by_audit_uidx1 ON r_report_registered_by_audit(data_id);