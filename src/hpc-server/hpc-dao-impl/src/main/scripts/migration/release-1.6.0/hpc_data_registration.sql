--
-- hpc_data_registration.sql
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

ALTER TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK" ADD COLUMN "UI_URL" text;

COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."UI_URL" IS 
                  'The UI URL to view the task by id';
