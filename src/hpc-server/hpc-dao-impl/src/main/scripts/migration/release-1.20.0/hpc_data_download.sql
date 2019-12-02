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
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--
                  
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" ADD COLUMN "PROCESSED" timestamp;                  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."PROCESSED" IS 
	'The date and time the task was processed';  
                  
