-- Create new sequence to use for PK values of "HPC_SYSTEM_ACCOUNT" table
CREATE SEQUENCE public."HPC_SYSTEM_ACCOUNT_SEQ"
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER SEQUENCE public."HPC_SYSTEM_ACCOUNT_SEQ"
  OWNER TO postgres;


-- Rename existing "HPC_SYSTEM_ACCOUNT" table and its PK constraint too to avoid conflict 
-- with new table and PK constraint that shall be created
ALTER TABLE IF EXISTS ONLY public."HPC_SYSTEM_ACCOUNT"
  RENAME CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" TO "LEGACY_HPC_SYSTEM_ACCOUNT_pkey";
ALTER TABLE IF EXISTS public."HPC_SYSTEM_ACCOUNT"
  RENAME TO "LEGACY_HPC_SYSTEM_ACCOUNT";


-- Create new "HPC_SYSTEM_ACCOUNT" table
CREATE TABLE public."HPC_SYSTEM_ACCOUNT" (
  "ID" integer DEFAULT nextval('"HPC_SYSTEM_ACCOUNT_SEQ"'::regclass) NOT NULL,
  "SYSTEM" text NOT NULL,
  "CLASSIFIER" text,
  "DATA_TRANSFER_TYPE" text,
  "USERNAME" text NOT NULL,
  "PASSWORD" bytea NOT NULL,
  CONSTRAINT "HPC_SYSTEM_ACCOUNT_pkey" PRIMARY KEY ("ID")
);
ALTER TABLE public."HPC_SYSTEM_ACCOUNT" OWNER TO postgres;


COMMENT ON TABLE public."HPC_SYSTEM_ACCOUNT" IS 'System accounts';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."ID" IS 'PK column; the unique ID value for the system account record';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."SYSTEM" IS 'The system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."CLASSIFIER" IS 'A classifier to be more specific at finer grainer than which system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."DATA_TRANSFER_TYPE" IS 'The data transfer type (S3 for Cleversafe etc)';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."USERNAME" IS 'The user name';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."PASSWORD" IS 'The password';


-- Copy data from legacy table to new table
INSERT INTO public."HPC_SYSTEM_ACCOUNT"("SYSTEM", "DATA_TRANSFER_TYPE", "USERNAME", "PASSWORD")
  SELECT "SYSTEM", "DATA_TRANSFER_TYPE", "USERNAME", "PASSWORD"
  FROM public."LEGACY_HPC_SYSTEM_ACCOUNT";


-- Drop the legacy table
DROP TABLE IF EXISTS public."LEGACY_HPC_SYSTEM_ACCOUNT";