--
-- hpc_user.sql
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

DROP TABLE IF EXISTS public."HPC_USER";
CREATE TABLE public."HPC_USER"
(
  "USER_ID" text NOT NULL,
  "FIRST_NAME" text,
  "LAST_NAME" text,
  "DEFAULT_CONFIGURATION_ID" text,
  "ACTIVE" boolean,
  "CREATED" date,
  "LAST_UPDATED" date,
  "ACTIVE_UPDATED_BY" text,
  CONSTRAINT "HPC_USER_pkey" PRIMARY KEY ("USER_ID")
)
WITH (
  OIDS=FALSE
);
