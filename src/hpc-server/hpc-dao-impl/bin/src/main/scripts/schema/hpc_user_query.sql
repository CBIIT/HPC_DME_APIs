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
--

DROP TABLE IF EXISTS public."HPC_USER_QUERY";
CREATE TABLE public."HPC_USER_QUERY"
(
  "USER_ID" text NOT NULL,
  "QUERY_NAME" text NOT NULL,
  "QUERY" bytea NOT NULL,
  "DETAILED_RESPONSE" boolean NOT NULL,
  "TOTAL_COUNT" boolean NOT NULL,
  "QUERY_TYPE" text NOT NULL,
  "CREATED" timestamp NOT NULL,
  "UPDATED" timestamp NOT NULL,
  CONSTRAINT "HPC_USER_QUERY_pkey" PRIMARY KEY ("USER_ID", "QUERY_NAME")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_USER_QUERY" IS 
                 'User queries';
COMMENT ON COLUMN public."HPC_USER_QUERY"."USER_ID" IS 
                  'The user ID that owns this query';
COMMENT ON COLUMN public."HPC_USER_QUERY"."QUERY" IS 
                  'The compound metadata query';
COMMENT ON COLUMN public."HPC_USER_QUERY"."DETAILED_RESPONSE" IS 
                  'Detailed response indicator to request when using this query';
COMMENT ON COLUMN public."HPC_USER_QUERY"."TOTAL_COUNT" IS 
                  'Total count parameter to set when using this query';
COMMENT ON COLUMN public."HPC_USER_QUERY"."QUERY_TYPE" IS 
                  'The query type - collection or data object';
COMMENT ON COLUMN public."HPC_USER_QUERY"."CREATED" IS 
                  'The date / time the query was created';
COMMENT ON COLUMN public."HPC_USER_QUERY"."UPDATED" IS 
                  'The date / time the query was updated';
