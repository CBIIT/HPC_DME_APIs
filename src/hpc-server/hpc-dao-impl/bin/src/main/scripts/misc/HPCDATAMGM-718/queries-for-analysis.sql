-- Query base paths of test/non-authentic DOCs
SELECT "BASE_PATH", "DOC"
FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')
ORDER BY "BASE_PATH";


-- Query collections which are immediate children of base paths for all 
-- test/non-authentic DOCs
SELECT
  substring(t1.coll_name from strpos(t1.coll_name, t2.pathname)) dme_path,
  t1.coll_id,
  substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)) coll_simple_name,
  t1.coll_name irods_path
FROM r_coll_main t1,
  ( SELECT "BASE_PATH" pathname
    FROM "HPC_DATA_MANAGEMENT_CONFIGURATION"
    WHERE "DOC" IN ('DUMMY_NO_HIER', 'FS_ARCHIVE', 'NOHIERARCHY', 'TEST')) t2
WHERE strpos(t1.coll_name, '/trash/home/') = 0
  AND strpos(t1.coll_name, t2.pathname) > 0
  AND strpos(substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)), '/') = 0
  AND substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname)) for 1) = '/'
ORDER BY 1
--LIMIT 100
;


-- Query data objects/files which are immediate children of base paths for all 
-- test/non-authentic DOCs
-- Query for data files which are immediate children of base paths of all test/non-authentic DOCs
SELECT (aux1.dme_coll_path || '/' || t1.data_name) dme_path,
       t1.data_id, 
       t1.data_name,
	   (aux1.coll_name || '/' || t1.data_name) irods_path
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


-- Query for all immediate children of base paths for all
-- test/non-authentic DOCs (covering both collections and data objects)
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
 ORDER BY 1;