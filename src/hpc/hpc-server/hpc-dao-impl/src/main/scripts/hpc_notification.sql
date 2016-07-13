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
  
DROP TABLE IF EXISTS public."HPC_NOTIFICATION_EVENT";
CREATE TABLE public."HPC_NOTIFICATION_EVENT"
(
  "ID" SERIAL PRIMARY KEY,
  "USER_ID" text NOT NULL,
  "NOTIFICATION_TYPE" text NOT NULL,
  "PAYLOAD" bytea,
  "CREATED" timestamp DEFAULT current_timestamp
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_NOTIFICATION_EVENT"
  OWNER TO postgres;
  
DROP TABLE IF EXISTS public."HPC_NOTIFICATION_DELIVERY_RECEIPT";
CREATE TABLE public."HPC_NOTIFICATION_DELIVERY_RECEIPT"
(
  "ID" integer NOT NULL,
  "USER_ID" text NOT NULL,
  "NOTIFICATION_TYPE" text NOT NULL,
  "NOTIFICATION_DELIVERY_METHOD" text NOT NULL,
  "PAYLOAD" bytea,
  "DELIVERY_STATUS" boolean NOT NULL,
  "CREATED" date NOT NULL,
  "DELIVERED" timestamp DEFAULT current_timestamp
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public."HPC_NOTIFICATION_DELIVERY_RECEIPT"
  OWNER TO postgres;