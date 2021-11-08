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
           
DROP MATERIALIZED VIEW R_DATA_HIERARCHY_USER_META_MAIN;

create materialized view irods.R_DATA_HIERARCHY_USER_META_MAIN as
select * from R_DATA_HIERARCHY_META_MAIN where META_ATTR_NAME not in
('collection_type',
'uuid',
'dme_data_id',
'registered_by',
'registered_by_name',
'configuration_id',
's3_archive_configuration_id',
'source_file_id',
'archive_file_id',
'data_transfer_request_id',
'data_transfer_method',
'data_transfer_type',
'data_transfer_started',
'data_transfer_completed',
'source_file_url',
'source_file_nih_owner',
'source_file_owner',
'source_file_user_dn',
'source_file_nih_user_dn',
'source_file_nih_group',
'source_file_group',
'source_file_group_dn',
'source_file_nih_group_dn',
'source_file_permissions',
'archive_caller_object_id',
'checksum',
'metadata_updated',
'link_source_path',
'deep_archive_status',
'deep_archive_date',
'deleted_date');

create unique index r_data_hierarchy_user_meta_main_unique
    on irods.r_data_hierarchy_user_meta_main (object_id, meta_id, data_level);

create index r_data_hierarchy_user_meta_main_path_query
    on irods.r_data_hierarchy_user_meta_main (object_path);

create index r_data_hierarchy_user_meta_main_id_query
    on irods.r_data_hierarchy_user_meta_main (object_id);

create index r_data_hierarchy_user_meta_main_metadata_query_level
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, meta_attr_value, data_level);

create index r_data_hierarchy_user_meta_main_metadata_query_level_label
    on irods.r_data_hierarchy_user_meta_main (meta_attr_name, level_label);

DROP PROCEDURE refresh_hierarchy_meta_view;

CREATE PROCEDURE refresh_hierarchy_meta_view AS

BEGIN

        DBMS_MVIEW.REFRESH('R_COLL_HIERARCHY_METAMAP,
                        R_COLL_HIERARCHY_META_MAIN,
                        R_CATALOG_META_MAIN,
                        R_DATA_HIERARCHY_METAMAP,
                        R_DATA_HIERARCHY_META_MAIN,
                        R_DATA_HIERARCHY_USER_META_MAIN',
                        METHOD => 'C',  ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE);


END;