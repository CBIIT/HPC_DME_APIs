--
-- hpc_notification.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
-- @version $Id:$
--

DROP TABLE IF EXISTS public."HPC_NOTIFICATION_SUBSCRIPTION";
CREATE TABLE public."HPC_NOTIFICATION_SUBSCRIPTION"
(
  "USER_ID" text NOT NULL,
  "NOTIFICATION_TYPE" text NOT NULL,
  "NOTIFICATION_DELIVERY_METHODS" text NOT NULL,
  CONSTRAINT "HPC_NOTIFICATION_SUBSCRIPTION_pkey" PRIMARY KEY ("USER_ID", "NOTIFICATION_TYPE")
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_NOTIFICATION_SUBSCRIPTION"
  OWNER TO postgres;