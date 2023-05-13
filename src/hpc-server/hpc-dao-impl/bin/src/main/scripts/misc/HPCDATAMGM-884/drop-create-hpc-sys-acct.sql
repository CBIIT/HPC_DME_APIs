START TRANSACTION;

CREATE TEMPORARY TABLE hpc_tmp_sys_acct
AS (
   SELECT "SYSTEM" system_name, "CLASSIFIER" classifier, "DATA_TRANSFER_TYPE" transfer_type, 
          "USERNAME" username, "PASSWORD" psswd
   FROM public."HPC_SYSTEM_ACCOUNT"
   ORDER BY "SYSTEM", "CLASSIFIER"
);

DROP TABLE public."HPC_SYSTEM_ACCOUNT";

DROP SEQUENCE IF EXISTS public."HPC_SYSTEM_ACCOUNT_SEQ";

CREATE SEQUENCE public."HPC_SYSTEM_ACCOUNT_SEQ"
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  CACHE 1;

ALTER TABLE public."HPC_SYSTEM_ACCOUNT_SEQ"
  OWNER TO postgres;

CREATE TABLE public."HPC_SYSTEM_ACCOUNT"
(
  "ID" integer NOT NULL DEFAULT nextval('"HPC_SYSTEM_ACCOUNT_SEQ"'::regclass), -- The system account ID
  "SYSTEM" text NOT NULL, -- The system
  "CLASSIFIER" text, -- A classifier to be more specific under/within system
  "DATA_TRANSFER_TYPE" text, -- The data transfer type (S3 for Cleversafe etc)
  "USERNAME" text NOT NULL, -- The user name
  "PASSWORD" bytea NOT NULL, -- The password
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("ID")
)
WITH (
  OIDS=FALSE
);

ALTER TABLE public."HPC_SYSTEM_ACCOUNT" OWNER TO irods;

GRANT ALL ON TABLE public."HPC_SYSTEM_ACCOUNT" TO irods;

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

INSERT INTO "HPC_SYSTEM_ACCOUNT"
  ("SYSTEM", "CLASSIFIER", "DATA_TRANSFER_TYPE", "USERNAME", "PASSWORD")
  SELECT system_name, classifier, transfer_type, username, psswd
  FROM hpc_tmp_sys_acct
  ORDER BY system_name, classifier;

DROP TABLE hpc_tmp_sys_acct;

COMMIT;
--ROLLBACK;
