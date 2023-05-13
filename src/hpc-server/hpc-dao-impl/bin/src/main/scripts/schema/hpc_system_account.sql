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
-- @author <a href="mailto:william.liu2@nih.gov">William Y. Liu</a>
--

DROP SEQUENCE IF EXISTS public."HPC_SYSTEM_ACCOUNT_SEQ";
CREATE SEQUENCE public."HPC_SYSTEM_ACCOUNT_SEQ"
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

COMMENT ON SEQUENCE public."HPC_SYSTEM_ACCOUNT_SEQ" IS 
  'Sequence for generation of primary key values in "HPC_SYSTEM_ACCOUNT" table';


DROP TABLE IF EXISTS public."HPC_SYSTEM_ACCOUNT";
CREATE TABLE public."HPC_SYSTEM_ACCOUNT"
(
  "ID" integer DEFAULT nextval('"HPC_SYSTEM_ACCOUNT_SEQ"'::regclass) NOT NULL,
  "SYSTEM" text NOT NULL,
  "CLASSIFIER" text,
  "DATA_TRANSFER_TYPE" text,
  "USERNAME" text NOT NULL,
  "PASSWORD" bytea NOT NULL,
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("ID")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_SYSTEM_ACCOUNT" IS 
                 'System accounts';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."ID" IS 
                 'PK column; the unique ID value for the system account record';                 
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."SYSTEM" IS 
                  'The system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."CLASSIFIER" IS 
                  'A classifier to be more specific at finer grainer than which system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer type (S3 for Cleversafe etc)';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."USERNAME" IS 
                  'The user name';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."PASSWORD" IS 
                  'The password';