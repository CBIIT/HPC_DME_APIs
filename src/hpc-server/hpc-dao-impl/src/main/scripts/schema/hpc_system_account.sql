--
-- hpc_system_account.sql
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

DROP TABLE IF EXISTS public."HPC_SYSTEM_ACCOUNT";
CREATE TABLE public."HPC_SYSTEM_ACCOUNT"
(
  "USERNAME" text NOT NULL,
  "PASSWORD" bytea NOT NULL,
  "SYSTEM" text NOT NULL,
  "DATA_TRANSFER_TYPE" text,
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("SYSTEM")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_SYSTEM_ACCOUNT" IS 
                 'System accounts';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."USERNAME" IS 
                  'The user name';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."PASSWORD" IS 
                  'The password';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."SYSTEM" IS 
                  'The system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer type (S3 for Cleversafe etc)';
