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

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_LIST_REGISTRATION_TASK";
CREATE TABLE public."HPC_DATA_OBJECT_LIST_REGISTRATION_TASK"
(
  "ID" text PRIMARY KEY,
  "USER_ID" text,
  "DOC" text,
  "STATUS" text,
  "ITEMS" text,
  "CREATED" timestamp
)
WITH (
  OIDS=FALSE
);

