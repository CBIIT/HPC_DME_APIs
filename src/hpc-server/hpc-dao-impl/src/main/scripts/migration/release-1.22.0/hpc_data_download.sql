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
    
-- HPCDATAMGM-1194 Add column to receive user request to cancel collection download task.              
ALTER TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" ADD COLUMN "CANCELLATION_REQUESTED" boolean;                  
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CANCELLATION_REQUESTED" IS 
                  'A request to cancel the download was submitted';
