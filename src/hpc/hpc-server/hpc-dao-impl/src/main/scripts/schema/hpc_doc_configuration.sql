--
-- hpc_doc_configuration.sql
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

DROP TABLE IF EXISTS public."HPC_DOC_CONFIGURATION";
CREATE TABLE public."HPC_DOC_CONFIGURATION"
(
  "DOC" text NOT NULL,
  "BASE_PATH" text NOT NULL,
  CONSTRAINT "HPC_DOC_pkey" PRIMARY KEY ("DOC")
)
WITH (
  OIDS=FALSE
);
