-- Table: public."HPC_SYSTEM_ACCOUNT"

-- DROP TABLE public."HPC_SYSTEM_ACCOUNT";

CREATE TABLE public."HPC_SYSTEM_ACCOUNT"
(
  "USERNAME" text NOT NULL, -- The user name
  "PASSWORD" bytea NOT NULL, -- The password
  "SYSTEM" text NOT NULL, -- The system
  "DATA_TRANSFER_TYPE" text, -- The data transfer type (S3 for Cleversafe etc)
  "CLASSIFIER" text, -- A classifier to be more specific under/within system
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("SYSTEM")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_SYSTEM_ACCOUNT"
  OWNER TO irods;
GRANT ALL ON TABLE public."HPC_SYSTEM_ACCOUNT" TO irods;
COMMENT ON TABLE public."HPC_SYSTEM_ACCOUNT"
  IS 'System accounts';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."USERNAME" IS 'The user name';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."PASSWORD" IS 'The password';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."SYSTEM" IS 'The system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."DATA_TRANSFER_TYPE" IS 'The data transfer type (S3 for Cleversafe etc)';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."CLASSIFIER" IS 'A classifier to be more specific under/within system';

