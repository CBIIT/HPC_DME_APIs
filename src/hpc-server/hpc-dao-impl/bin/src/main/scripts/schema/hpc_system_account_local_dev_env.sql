--
-- hpc_system_account_local_dev_env.sql
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
-- This script is intended to be used in setting up a local development environment.
-- This is a convenient way to use the DEV environment system accounts in local dev env.
-- A developer can choose to create a local schema, but then will need to obtain the system accounts credentials
-- and register them locally (using the REST services). This will need to be repeated when credentials are
-- updated, so just pointing to the DEV env is simpler.
--
-- This script needs to be run AFTER the hpc_hierarchical_metadata_local_dev_env.sql script, as
-- it assumes the DEV server is defined.

DROP FOREIGN TABLE IF EXISTS "HPC_SYSTEM_ACCOUNT";
CREATE FOREIGN TABLE "HPC_SYSTEM_ACCOUNT" (
  "ID" integer DEFAULT nextval('"HPC_SYSTEM_ACCOUNT_SEQ"'::regclass) NOT NULL,
  "SYSTEM" text NOT NULL,
  "CLASSIFIER" text,
  "DATA_TRANSFER_TYPE" text,
  "USERNAME" text NOT NULL,
  "PASSWORD" bytea NOT NULL,
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("ID"))
       SERVER hpc_dm_dev
       OPTIONS (schema_name 'public', table_name 'HPC_SYSTEM_ACCOUNT');

