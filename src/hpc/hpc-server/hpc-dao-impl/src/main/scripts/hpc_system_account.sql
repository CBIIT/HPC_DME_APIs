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
-- @version $Id$
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
ALTER TABLE public."HPC_SYSTEM_ACCOUNT"
  OWNER TO postgres;