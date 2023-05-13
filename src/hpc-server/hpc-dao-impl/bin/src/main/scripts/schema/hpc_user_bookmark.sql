--
-- hpc_user_bookmark.sql
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

DROP TABLE IF EXISTS public."HPC_USER_BOOKMARK";
CREATE TABLE public."HPC_USER_BOOKMARK"
(
  "USER_ID" text NOT NULL,
  "BOOKMARK_NAME" text NOT NULL,
  "BOOKMARK_GROUP" text,
  "PATH" text NOT NULL,
  "CREATED" timestamp NOT NULL,
  "UPDATED" timestamp NOT NULL,
  CONSTRAINT "HPC_USER_BOOKMARK_pkey" PRIMARY KEY ("USER_ID", "BOOKMARK_NAME")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_USER_BOOKMARK" IS 
                 'User bookmarks';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."USER_ID" IS 
                  'The user ID that owns this bookmark';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."BOOKMARK_NAME" IS 
                  'The bookmark name';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."BOOKMARK_GROUP" IS 
                  'The bookmark group';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."PATH" IS 
                  'The bookmark path';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."CREATED" IS 
                  'The date / time the bookmark was created';
COMMENT ON COLUMN public."HPC_USER_BOOKMARK"."UPDATED" IS 
                  'The date / time the bookmark was updated';
