--
-- hpc_user_query.sql
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

DROP TABLE IF EXISTS public."HPC_USER_QUERY";
CREATE TABLE public."HPC_USER_QUERY"
(
  "USER_ID" text NOT NULL,
  "QUERY_NAME" text NOT NULL,
  "PAYLOAD" bytea,
  CONSTRAINT "HPC_USER_QUERY_pkey" PRIMARY KEY ("USER_ID", "QUERY_NAME")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_USER_QUERY"
  OWNER TO postgres;