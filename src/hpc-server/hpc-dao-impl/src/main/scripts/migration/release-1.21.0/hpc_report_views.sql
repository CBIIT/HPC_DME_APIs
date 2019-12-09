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