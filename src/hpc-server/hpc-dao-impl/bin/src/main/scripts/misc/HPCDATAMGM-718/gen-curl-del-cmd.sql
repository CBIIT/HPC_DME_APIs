-- Generate curl commands that, for all base paths of all test/non-authentic DOCs,
-- deletes all children items.
-- 
-- The resulting curl commands perform both (A) recursive deletion of collections 
-- that are immediate children of the base paths and (B) deletion of data objects 
-- that are also immediate children of those base paths.
--
--  Notes:
--
--  1. <other-curl-options> is placeholder for necessary curl options for things like
--      authentication token, specifying request headers, etc.
--
--  2. <hpc-dme-server> is placeholder for host[:port] of applicable HPC DME REST API server
--
SELECT ('curl -X DELETE <other-curl-options> https://<hpc-dme-server>/' || 
		(CASE WHEN item_type = 'collection' 
		   THEN 'collection'
		 ELSE 'dataObject'
		 END) || 
		dme_path) AS curl_cmd
FROM (
  (SELECT
     substring(t1.coll_name from strpos(t1.coll_name, t2.pathname)) AS dme_path,
     'collection' AS item_type,
     substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)) AS item_basic_name,
     t1.coll_id AS irods_id,
     t1.coll_name AS irods_path
   FROM r_coll_main t1,
        ( SELECT "BASE_PATH" pathname
          FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
          WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')) t2
   WHERE strpos(t1.coll_name, t2.pathname) > 0
     AND strpos(substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)), '/') = 0
     AND trim(both from substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1))) != ''
     AND strpos(t1.coll_name, '/trash/home/') = 0)
  UNION
  (SELECT (aux1.dme_coll_path || '/' || t1.data_name) AS dme_path,
          'data file' AS item_type,
          t1.data_name AS item_basic_name,
          t1.data_id AS irods_id, 
  	      (aux1.coll_name || '/' || t1.data_name) AS irods_path
   FROM r_data_main t1,
        (SELECT s1.coll_id,
   	            s1.coll_name,
	            (substring(coll_name from strpos(s1.coll_name, s2.bpath))) dme_coll_path
         FROM r_coll_main s1,
              (SELECT "BASE_PATH" bpath
               FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
               WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')) s2
         WHERE s1.coll_name LIKE ('%' || s2.bpath)
           AND strpos(s1.coll_name, '/trash/home/') = 0) aux1
    WHERE t1.coll_id = aux1.coll_id)
) base_path_children
ORDER BY base_path_children.dme_path;


-- Generate curl commands for recursive deletion of collections that are
--  immediate children of base paths for all test/non-authentic DOCs
--
--  Notes:
--
--  1. <other-curl-options> is placeholder for necessary curl options for things like
--      authentication token, specifying request headers, etc.
--
--  2. <hpc-dme-server> is placeholder for host[:port] of applicable HPC DME REST API server
--
SELECT 'curl -X DELETE <other-curl-options> https://<hpc-dme-server>/collection' 
       || substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname))) curl_cmd
FROM r_coll_main t1,
  ( SELECT "BASE_PATH" pathname
    FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
    WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')) t2
WHERE strpos(t1.coll_name, t2.pathname) > 0
  AND strpos(substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)), '/') = 0
  AND trim(both from substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1))) != ''
  AND strpos(t1.coll_name, '/trash/home/') = 0
ORDER BY 1;


-- Generate curl commands for deletion of data files that are
--  immediate children of base paths for all test/non-authentic DOCs
--
--  Notes:
--
--  1. <other-curl-options> is placeholder for necessary curl options for things like
--      authentication token, specifying request headers, etc.
--
--  2. <hpc-dme-server> is placeholder for host[:port] of applicable HPC DME REST API server
--
SELECT ('curl -X DELETE <other-curl-options> https://<hpc-dme-server>/collection' || 
         aux1.dme_coll_path || '/' || t1.data_name) AS curl_cmd
FROM r_data_main t1,
     (SELECT s1.coll_id,
	         s1.coll_name,
	         (substring(coll_name from strpos(s1.coll_name, s2.bpath))) dme_coll_path
      FROM r_coll_main s1,
           (SELECT "BASE_PATH" bpath
            FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
            WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')) s2
      WHERE s1.coll_name LIKE ('%' || s2.bpath)
        AND strpos(s1.coll_name, '/trash/home/') = 0) aux1
WHERE t1.coll_id = aux1.coll_id
ORDER BY 1
;