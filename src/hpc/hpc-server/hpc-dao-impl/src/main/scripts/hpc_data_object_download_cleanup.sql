--
-- hpc_data_object_download_cleanup.sql
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

DROP TABLE IF EXISTS public."HPC_DATA_OBJECT_DOWNLOAD_CLEANUP";
CREATE TABLE public."HPC_DATA_OBJECT_DOWNLOAD_CLEANUP"
(
  "DATA_TRANSFER_REQUEST_ID" text NOT NULL,
  "DATA_TRANSFER_TYPE" text NOT NULL,
  "FILE_PATH" text NOT NULL,
  CONSTRAINT "HPC_DOWNLOAD_REQUEST_pkey" PRIMARY KEY ("DATA_TRANSFER_REQUEST_ID")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_DATA_OBJECT_DOWNLOAD_CLEANUP"
  OWNER TO ncif_hpcdm_db;