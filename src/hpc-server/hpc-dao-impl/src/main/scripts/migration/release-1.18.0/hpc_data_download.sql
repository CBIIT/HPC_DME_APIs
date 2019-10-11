--
-- hpc_data_download.sql
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
                  
ALTER TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" ADD COLUMN "APPEND_PATH_TO_DOWNLOAD_DESTINATION" boolean;                  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."APPEND_PATH_TO_DOWNLOAD_DESTINATION" IS 
	'An indicator whether to use the full object path at the download destination, or file name only';  
                  
