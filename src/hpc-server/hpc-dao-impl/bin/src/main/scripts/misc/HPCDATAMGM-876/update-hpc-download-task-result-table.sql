/*
 Updates HPC_DOWNLOAD_TASK_RESULT table within "PATH" column to perform textual 
 replace operations where each occurrence of 'PI_Lab_Hager' is replaced by 
 'PI_Lab_Gordon_Hager'.

 Rows identified by "ID" and were discovered by ad-hoc querying.
 */
UPDATE "HPC_DOWNLOAD_TASK_RESULT" 
SET "PATH" = replace("PATH", 'PI_Lab_Hager', 'PI_Lab_Gordon_Hager')
WHERE "ID" IN ('9e5ebdd2-989b-4daf-a4b8-3cfbd44c40ec', '1b0fcc29-ba99-4593-aa31-fbcfb0815ff5', '786670aa-0f67-4c09-a123-a1ccd5ed888f', '243a07db-b8f0-4651-b893-4c877051ed81', 'a45f7e7b-1ae2-49f9-b5f6-3253112b850a', 'f11a9466-40e2-43b7-bcaa-1d6efd50eab5', 'c1a84e3f-5945-4ffc-95c6-1eb5edcebd19', '3e309bef-ac34-482b-9328-12703d9192e5', '56fa5220-0f9a-40a1-99ce-3d120707cb10');


/*
 Updates HPC_DOWNLOAD_TASK_RESULT table within "ITEMS" column to perform textual 
 replace operations where each occurrence of 'PI_Lab_Hager' is replaced by 
 'PI_Lab_Gordon_Hager'.

 Row identified by "ID" and was discovered by ad-hoc querying.
 */
UPDATE "HPC_DOWNLOAD_TASK_RESULT" 
SET "ITEMS" = replace("PATH", 'PI_Lab_Hager', 'PI_Lab_Gordon_Hager')
WHERE "ID" IN ('56fa5220-0f9a-40a1-99ce-3d120707cb10');

COMMIT;