--
-- hpc_update_file_size.sql
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
-- Run The follow SQL query to get a list of data object paths that do not have file size system metadata set.

select object_path from r_data_hierarchy_meta_main where level = 1 and meta_attr_name = 'data_transfer_status' and meta_attr_value = 'ARCHIVED' and object_id not in (select object_id from r_data_hierarchy_meta_main where meta_attr_name = 'source_file_size' and level = 1)

-- After obtaining a list - construct a curl command to update the file-size metadata:
-- curl -k -H "Content-Type: application/json" -H "Authorization: Basic cm9zZW5iZXJnZWE6TWFnaWNib290czEwNyE=-X POST -d '{ "paths" : ["/path-1", "/path-2", "/path3]] }' https://<hpcdme-server>:7738/hpc-server/fileSize

-- Let the materialized views to refresh, and run the SQL query again to confirm file-size metadata was updated.