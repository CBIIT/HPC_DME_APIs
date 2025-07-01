--
-- hpc_browse_view.sql
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

DROP VIEW IRODS.R_BROWSE_META_MAIN;
create view IRODS.R_BROWSE_META_MAIN as
SELECT data.data_id                                                                     AS id,
       (cast(coll.coll_name as varchar2(2700)) || cast('/' as varchar2(1))) || cast(data.data_name as varchar2(2700)) AS path,
       cast(meta_size.meta_attr_value as number(19))                                    AS data_size,
       to_timestamp(meta_uploaded.meta_attr_value,'MM-DD-YYYY HH24:MI:SS') AS uploaded,
       case when meta_softlink.META_ATTR_VALUE is not null then 1 else 0 end as softlink
FROM r_data_main data
         JOIN r_coll_main coll ON data.coll_id = coll.coll_id
         LEFT JOIN (r_objt_metamap map_size
    JOIN r_meta_main meta_size ON meta_size.meta_id = map_size.meta_id AND
                                  meta_size.meta_attr_name = 'source_file_size')
                   ON map_size.object_id = data.data_id
         LEFT JOIN (r_objt_metamap map_uploaded
    JOIN r_meta_main meta_uploaded ON meta_uploaded.meta_id = map_uploaded.meta_id AND
                                      meta_uploaded.meta_attr_name = 'data_transfer_completed')
                   ON map_uploaded.object_id = data.data_id
        LEFT JOIN (r_objt_metamap map_softlink
    JOIN r_meta_main meta_softlink ON meta_softlink.meta_id = map_softlink.meta_id AND
                                  meta_softlink.meta_attr_name = 'link_source_path')
                   ON map_softlink.object_id = data.data_id
UNION
SELECT coll.coll_id                                                             AS id,
       coll.coll_name                                                           AS path,
       cast(NULL as number(19))                                                 AS data_size,
       NEW_TIME(to_timestamp('1970-01-01', 'YYYY-MM-DD') + numtodsinterval(coll.modify_ts, 'SECOND'), 'GMT', 'EST') AS uploaded,
       case when meta_softlink.META_ATTR_VALUE is not null then 1 else 0 end as softlink
FROM r_coll_main coll
        LEFT JOIN (r_objt_metamap map_softlink
    JOIN r_meta_main meta_softlink ON meta_softlink.meta_id = map_softlink.meta_id AND
                                  meta_softlink.meta_attr_name = 'link_source_path')
                   ON map_softlink.object_id = coll.coll_id;