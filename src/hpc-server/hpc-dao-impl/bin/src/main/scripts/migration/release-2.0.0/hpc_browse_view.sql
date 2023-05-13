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

                 
DROP VIEW IF EXISTS public.r_browse_meta_main;
CREATE VIEW public.r_browse_meta_main AS
  select data.data_id                                                         as id,
         coll.coll_name || '/' || data.data_name                              as path,
         cast(meta_size.meta_attr_value as bigint)                            as size,
         to_timestamp(meta_uploaded.meta_attr_value, 'MM-DD-YYYY HH24:MI:SS') as uploaded
  from (r_data_main data join
    r_coll_main coll on data.coll_id = coll.coll_id)
         left outer join
       (r_objt_metamap map_size join
         r_meta_main meta_size on meta_size.meta_id = map_size.meta_id and
                                  meta_size.meta_attr_name = 'source_file_size')
       on map_size.object_id = data.data_id
         left outer join
       (r_objt_metamap map_uploaded join
         r_meta_main meta_uploaded on meta_uploaded.meta_id = map_uploaded.meta_id and
                                      meta_uploaded.meta_attr_name = 'data_transfer_completed')
       on map_uploaded.object_id = data.data_id
  union
  select coll_id as id, coll_name as path, null as size, to_timestamp(cast(modify_ts as integer)) as uploaded
  from r_coll_main;