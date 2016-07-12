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
-- @version $Id$
--

DROP TABLE IF EXISTS public."HPC_USER";
CREATE TABLE public."HPC_USER"
(
  "USER_ID" text NOT NULL,
  "FIRST_NAME" text,
  "LAST_NAME" text,
  "DOC" text,
  "IRODS_USERNAME" text,
  "IRODS_PASSWORD" bytea,
  "CREATED" date,
  "LAST_UPDATED" text,
  CONSTRAINT "HPC_USER_pkey" PRIMARY KEY ("USER_ID")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_USER"
  OWNER TO ncif_hpcdm_db;