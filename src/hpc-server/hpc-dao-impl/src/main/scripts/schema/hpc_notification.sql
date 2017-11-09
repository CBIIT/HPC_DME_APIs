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
--

DROP TABLE IF EXISTS public."HPC_NOTIFICATION_SUBSCRIPTION" CASCADE;
CREATE TABLE public."HPC_NOTIFICATION_SUBSCRIPTION" 
(
  "ID" SERIAL PRIMARY KEY,
  "USER_ID" text NOT NULL,
  "EVENT_TYPE" text NOT NULL,
  "NOTIFICATION_DELIVERY_METHODS" text[] NOT NULL,
  CONSTRAINT "HPC_NOTIFICATION_SUBSCRIPTION_unique" UNIQUE ("USER_ID", "EVENT_TYPE")
)
WITH (
  OIDS=TRUE
);

COMMENT ON TABLE public."HPC_NOTIFICATION_SUBSCRIPTION" IS 
                 'Notification subscriptions';
COMMENT ON COLUMN public."HPC_NOTIFICATION_SUBSCRIPTION"."ID" IS 
                  'The notification subscription ID';
COMMENT ON COLUMN public."HPC_NOTIFICATION_SUBSCRIPTION"."USER_ID" IS 
                  'The user ID that is subscribed for this notification';
COMMENT ON COLUMN public."HPC_NOTIFICATION_SUBSCRIPTION"."EVENT_TYPE" IS 
                  'The event type to trigger the notification';
COMMENT ON COLUMN public."HPC_NOTIFICATION_SUBSCRIPTION"."NOTIFICATION_DELIVERY_METHODS" IS 
                  'The delivery methods the user would like to receive notifications';
  
DROP TABLE IF EXISTS public."HPC_NOTIFICATION_TRIGGER";
CREATE TABLE public."HPC_NOTIFICATION_TRIGGER"
(
  "NOTIFICATION_SUBSCRIPTION_ID" integer REFERENCES public."HPC_NOTIFICATION_SUBSCRIPTION"("ID") ON DELETE CASCADE ON UPDATE CASCADE,
  "NOTIFICATION_TRIGGER" text[]
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_NOTIFICATION_TRIGGER" IS 
                 'Notification triggers - rules to determine if the notification needs to be sent';
COMMENT ON COLUMN public."HPC_NOTIFICATION_TRIGGER"."NOTIFICATION_SUBSCRIPTION_ID" IS 
                  'The notification subscription ID to apply the trigger rules';
COMMENT ON COLUMN public."HPC_NOTIFICATION_TRIGGER"."NOTIFICATION_TRIGGER" IS 
                  'A list of rules that operate on event payload data to determine if the notification should be sent';
  
DROP TABLE IF EXISTS public."HPC_EVENT";
CREATE TABLE public."HPC_EVENT"
(
  "ID" SERIAL PRIMARY KEY,
  "USER_IDS" text NOT NULL,
  "TYPE" text NOT NULL,
  "PAYLOAD" bytea,
  "CREATED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_EVENT" IS 
                 'Active events';
COMMENT ON COLUMN public."HPC_EVENT"."ID" IS 
                  'The event ID';
COMMENT ON COLUMN public."HPC_EVENT"."USER_IDS" IS 
                  'A list of user ID that are subscribed to be notified for this event';
COMMENT ON COLUMN public."HPC_EVENT"."TYPE" IS 
                  'The event type';
COMMENT ON COLUMN public."HPC_EVENT"."PAYLOAD" IS 
                  'The event payload';
COMMENT ON COLUMN public."HPC_EVENT"."CREATED" IS 
                  'The date/time the event was created';
                  
DROP TABLE IF EXISTS public."HPC_EVENT_HISTORY";
CREATE TABLE public."HPC_EVENT_HISTORY"
(
  "ID" integer PRIMARY KEY,
  "USER_IDS" text NOT NULL,
  "TYPE" text NOT NULL,
  "PAYLOAD" bytea,
  "CREATED" timestamp NOT NULL
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_EVENT_HISTORY" IS 
                 'Event history - i.e. events that processed';
COMMENT ON COLUMN public."HPC_EVENT_HISTORY"."ID" IS 
                  'The event ID';
COMMENT ON COLUMN public."HPC_EVENT_HISTORY"."USER_IDS" IS 
                  'A list of user ID that are subscribed to be notified for this event';
COMMENT ON COLUMN public."HPC_EVENT_HISTORY"."TYPE" IS 
                  'The event type';
COMMENT ON COLUMN public."HPC_EVENT_HISTORY"."PAYLOAD" IS 
                  'The event payload';
COMMENT ON COLUMN public."HPC_EVENT_HISTORY"."CREATED" IS 
                  'The date/time the event was created';
                  
DROP TABLE IF EXISTS public."HPC_NOTIFICATION_DELIVERY_RECEIPT";
CREATE TABLE public."HPC_NOTIFICATION_DELIVERY_RECEIPT"
(
  "EVENT_ID" integer NOT NULL,
  "USER_ID" text NOT NULL,
  "NOTIFICATION_DELIVERY_METHOD" text NOT NULL,
  "DELIVERY_STATUS" boolean NOT NULL,
  "DELIVERED" timestamp NOT NULL,
  CONSTRAINT "HPC_NOTIFICATION_DELIVERY_RECEIPT_pkey" PRIMARY KEY ("EVENT_ID", "USER_ID", "NOTIFICATION_DELIVERY_METHOD")
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE public."HPC_NOTIFICATION_DELIVERY_RECEIPT" IS 
                 'Notification delivery receipts - i.e. a list of all notifications the system sent out';
COMMENT ON COLUMN public."HPC_NOTIFICATION_DELIVERY_RECEIPT"."EVENT_ID" IS 
                  'The event ID that notification was sent for';
COMMENT ON COLUMN public."HPC_NOTIFICATION_DELIVERY_RECEIPT"."USER_ID" IS 
                  'The user ID that was notified';
COMMENT ON COLUMN public."HPC_NOTIFICATION_DELIVERY_RECEIPT"."NOTIFICATION_DELIVERY_METHOD" IS 
                  'The notification delivery method';
COMMENT ON COLUMN public."HPC_NOTIFICATION_DELIVERY_RECEIPT"."DELIVERY_STATUS" IS 
                  'Notification delivery success / failure indicator';
COMMENT ON COLUMN public."HPC_NOTIFICATION_DELIVERY_RECEIPT"."DELIVERED" IS 
                  'The data / time the delivery attempt was performed';
