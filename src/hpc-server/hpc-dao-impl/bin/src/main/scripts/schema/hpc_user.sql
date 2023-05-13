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
  "DOC" text,
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

COMMENT ON TABLE public."HPC_USER" IS 
                 'HPC-DME Users';
COMMENT ON COLUMN public."HPC_USER"."USER_ID" IS 
                  'The user ID';
COMMENT ON COLUMN public."HPC_USER"."FIRST_NAME" IS 
                  'The user first name';
COMMENT ON COLUMN public."HPC_USER"."LAST_NAME" IS 
                  'The user last name';
COMMENT ON COLUMN public."HPC_USER"."DOC" IS 
                  'The DOC the user belongs to';
COMMENT ON COLUMN public."HPC_USER"."DEFAULT_CONFIGURATION_ID" IS 
                  'The default configuration ID associated with the user';
COMMENT ON COLUMN public."HPC_USER"."ACTIVE" IS 
                  'User active indicator';
COMMENT ON COLUMN public."HPC_USER"."CREATED" IS 
                  'The date / time the user was created';
COMMENT ON COLUMN public."HPC_USER"."LAST_UPDATED" IS 
                  'The date / time the user was updated';
COMMENT ON COLUMN public."HPC_USER"."ACTIVE_UPDATED_BY" IS 
                  'The administrator user ID that activated this user';
