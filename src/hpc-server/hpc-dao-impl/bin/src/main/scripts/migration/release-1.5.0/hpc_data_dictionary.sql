--
-- hpc_data_dictionary.sql
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

COMMENT ON TABLE public."HPC_DATA_OBJECT_DOWNLOAD_TASK" IS 
                 'Active data object download tasks';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CONFIGURATION_ID" IS 
                  'The configuration ID to use in the downloading the data object';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (S3 or Globus) request ID that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer (S3 or Globus) that is currently in progress';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DATA_TRANSFER_STATUS" IS 
                  'The data transfer status (S3 or Globus)';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DOWNLOAD_FILE_PATH" IS 
                  'The file path used in the 2-hop download';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ARCHIVE_LOCATION_FILE_CONTAINER_ID" IS 
                  'The archive location container ID of the data object to be downloaded';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."ARCHIVE_LOCATION_FILE_ID" IS 
                  'The archive location file ID of the data object to be downloaded';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event needs to be generated when the task is completed';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
                  
COMMENT ON TABLE public."HPC_COLLECTION_DOWNLOAD_TASK" IS 
                 'Active collection or bulk download tasks';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."TYPE" IS 
                  'The type of the request - collection or bulk (list of data objects)';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."PATH" IS 
                  'The collection path to download';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DATA_OBJECT_PATHS" IS 
                  'The list of data object paths to download';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."STATUS" IS 
                  'The download task status';   
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';            
COMMENT ON COLUMN public."HPC_COLLECTION_DOWNLOAD_TASK"."CREATED" IS 
                  'The date and time the task was created';
                  
COMMENT ON TABLE public."HPC_DOWNLOAD_TASK_RESULT" IS 
                 'Download task (single file, collection, bulk) results';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."ID" IS 
                  'The download task ID';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."USER_ID" IS 
                  'The user ID who submitted the download request';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."TYPE" IS 
                  'The type of the request - data object, collection or bulk (list of data objects)';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."PATH" IS 
                  'The data object or collection path requested';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DATA_TRANSFER_REQUEST_ID" IS 
                  'The data transfer (S3 or Globus) request ID that was last used';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer (S3 or Globus) that was last used';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_CONTAINER_ID" IS 
                  'The download destination container ID';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."DESTINATION_LOCATION_FILE_ID" IS 
                  'The download destination file ID';   
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."RESULT" IS 
                  'The download task success/fail indicator';   
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."MESSAGE" IS 
                  'An error message in case the task failed';                     
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."ITEMS" IS 
                  'The download items included in this collection / bulk download request';  
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DOWNLOAD_TASK"."COMPLETION_EVENT" IS 
                  'An indicator whether a completion event was generated when the task completed';          
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."CREATED" IS 
                  'The date and time the task was created';
COMMENT ON COLUMN public."HPC_DOWNLOAD_TASK_RESULT"."COMPLETED" IS 
                  'The date and time the task was completed';
                  
COMMENT ON TABLE public."HPC_DATA_MANAGEMENT_CONFIGURATION" IS 
                 'The data management configurations supported by HPC-DME';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."ID" IS 
                  'The configuration ID';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."BASE_PATH" IS 
                  'The base path to apply this configuration to';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DOC" IS 
                  'The DOC that own this configuration';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_URL" IS 
                  'The S3 archive (Cleversafe) URL';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_VAULT" IS 
                  'The S3 archive (Cleversafe) vault';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_OBJECT_ID" IS 
                  'The S3 archive (Cleversafe) object id prefix';
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."S3_ARCHIVE_TYPE" IS 
                  'The S3 archive type (Archive / Temp Archive). Note: Temp Archive is currently not used';  
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_HIERARCHY" IS 
                  'The data hierarchy policy';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."COLLECTION_METADATA_VALIDATION_RULES" IS 
                  'The collection metadata validation rules';    
COMMENT ON COLUMN public."HPC_DATA_MANAGEMENT_CONFIGURATION"."DATA_OBJECT_METADATA_VALIDATION_RULES" IS 
                  'The data object metadata validation rules';     
                  
COMMENT ON TABLE public."HPC_DATA_OBJECT_DELETION_HISTORY" IS 
                 'Data object deletion requests';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."PATH" IS 
                  'The data object path';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."METADATA" IS 
                  'The data object metadata at the time of deletion';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_FILE_CONTAINER_ID" IS 
                  'The data object archive container ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_FILE_ID" IS 
                  'The data object archive file ID';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."ARCHIVE_DELETE_STATUS" IS 
                  'Deletion from archive (Cleversafe) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."DATA_MANAGEMENT_DELETE_STATUS" IS 
                  'Deletion from data management (iRODS) success/fail indicator';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."MESSAGE" IS 
                  'Error message if the deletion failed (iRODS or Cleversafe)';
COMMENT ON COLUMN public."HPC_DATA_OBJECT_DELETION_HISTORY"."DELETED" IS 
                  'The date/time the request was submitted';
                  
COMMENT ON TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK" IS 
                 'Bulk data object registration tasks';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."ID" IS 
                  'The bulk registration task ID';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."STATUS" IS 
                  'The bulk registration task status';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."ITEMS" IS 
                  'The list individual data object registrations included in this bulk registration request, in JSON format';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_TASK"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';
                  
COMMENT ON TABLE public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT" IS 
                 'Bulk data object registration task results';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."ID" IS 
                  'The bulk registration task ID';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."USER_ID" IS 
                  'The user ID who submitted the request';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."RESULT" IS 
                  'Task success/failure indicator';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."MESSAGE" IS 
                  'An error message if the task failed';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."ITEMS" IS 
                  'The list individual data object registrations included in this bulk registration request, in JSON format';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."CREATED" IS 
                  'The data/time the bulk registration request was submitted';
COMMENT ON COLUMN public."HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT"."COMPLETED" IS 
                  'The data/time the bulk registration request was completed';
                  
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
                  
COMMENT ON TABLE public."HPC_NOTIFICATION_TRIGGER" IS 
                 'Notification triggers - rules to determine if the notification needs to be sent';
COMMENT ON COLUMN public."HPC_NOTIFICATION_TRIGGER"."NOTIFICATION_SUBSCRIPTION_ID" IS 
                  'The notification subscription ID to apply the trigger rules';
COMMENT ON COLUMN public."HPC_NOTIFICATION_TRIGGER"."NOTIFICATION_TRIGGER" IS 
                  'A list of rules that operate on event payload data to determine if the notification should be sent';
                  
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
                  
COMMENT ON TABLE public."HPC_SYSTEM_ACCOUNT" IS 
                 'System accounts';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."USERNAME" IS 
                  'The user name';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."PASSWORD" IS 
                  'The password';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."SYSTEM" IS 
                  'The system';
COMMENT ON COLUMN public."HPC_SYSTEM_ACCOUNT"."DATA_TRANSFER_TYPE" IS 
                  'The data transfer type (S3 for Cleversafe etc)';
                  
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
                  
COMMENT ON TABLE public."HPC_USER" IS 
                 'HPC-DME Users';
COMMENT ON COLUMN public."HPC_USER"."USER_ID" IS 
                  'The user ID';
COMMENT ON COLUMN public."HPC_USER"."FIRST_NAME" IS 
                  'The user first name';
COMMENT ON COLUMN public."HPC_USER"."LAST_NAME" IS 
                  'The user last name';
COMMENT ON COLUMN public."HPC_USER"."DOC" IS 
                  'The DOC the user belongs to';
COMMENT ON COLUMN public."HPC_USER"."DEFAULT_CONFIGURATION_ID" IS 
                  'The default configuration ID associated with the user';
COMMENT ON COLUMN public."HPC_USER"."ACTIVE" IS 
                  'User active indicator';
COMMENT ON COLUMN public."HPC_USER"."CREATED" IS 
                  'The date / time the user was created';
COMMENT ON COLUMN public."HPC_USER"."LAST_UPDATED" IS 
                  'The date / time the user was updated';
COMMENT ON COLUMN public."HPC_USER"."ACTIVE_UPDATED_BY" IS 
                  'The administrator user ID that activated this user';
                  