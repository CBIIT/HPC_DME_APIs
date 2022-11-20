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

create materialized view irods.r_report_collection_size as
WITH r_collection_size_hierarchy AS (
    select report_collection_path.coll_name,sum(to_number(report_source_file_size.meta_attr_value, '9999999999999999999')) totalSize           
    from r_report_collection_path report_collection_path, r_report_source_file_size report_source_file_size
    where report_collection_path.OBJECT_ID=report_source_file_size.OBJECT_ID
    group by report_collection_path.coll_name
)
select NVL(r_parent_collection_size_hierarchy.column_value, r_collection_size_hierarchy.COLL_NAME) AS COLL_NAME, r_collection_size_hierarchy.totalSize
from r_collection_size_hierarchy,
     table(cast(multiset(
                select SUBSTR(coll_name,1,(INSTR(coll_name,'/',-1,level)-1))
                from dual
                connect by level < regexp_count(coll_name, '/')+1)
         as sys.odciVarchar2List)) r_parent_collection_size_hierarchy;

DROP PROCEDURE refresh_report_meta_view;

CREATE PROCEDURE refresh_report_meta_view AS

BEGIN

        DBMS_MVIEW.REFRESH('R_REPORT_COLL_META_MAIN,
                        R_REPORT_COLL_REGISTERED_BY,
                        R_REPORT_COLL_REGISTERED_BY_BASEPATH,
                        R_REPORT_COLL_REGISTERED_BY_DOC,
                        R_REPORT_COLL_REGISTERED_BY_PATH,
                        R_REPORT_COLLECTION_PATH,
                        R_REPORT_COLLECTION_TYPE,
                        R_REPORT_DATA_META_MAIN,
                        R_REPORT_DATA_OBJECTS,
                        R_REPORT_META_MAIN,
                        R_REPORT_META_MAP,
                        R_REPORT_REGISTERED_BY,
                        R_REPORT_REGISTERED_BY_BASEPATH,
                        R_REPORT_REGISTERED_BY_DOC,
                        R_REPORT_REGISTERED_BY_AUDIT,
                        R_REPORT_REGISTERED_BY_PATH,
                        R_REPORT_SOURCE_FILE_SIZE,
                        R_REPORT_COLLECTION_SIZE',
                        METHOD => 'C', ATOMIC_REFRESH => FALSE, OUT_OF_PLACE => TRUE);

END;