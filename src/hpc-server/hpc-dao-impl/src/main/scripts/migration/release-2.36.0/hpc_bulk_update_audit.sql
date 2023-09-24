
create table HPC_BULK_UPDATE_AUDIT
(
    USER_ID              VARCHAR2(50)   not null,
    QUERY                CLOB           not null,
    QUERY_TYPE           VARCHAR2(50)   not null,
    METADATA_NAME        VARCHAR2(2700),
    METADATA_VALUE       VARCHAR2(2700),
    CREATED              TIMESTAMP(6)   not null
)
/

comment on column HPC_BULK_UPDATE_AUDIT.USER_ID is 'The user ID who executed the request'
/

comment on column HPC_BULK_UPDATE_AUDIT.QUERY is 'The query to be executed to find the records to be updated'
/

comment on column HPC_BULK_UPDATE_AUDIT.QUERY_TYPE is 'The type of query collection or data_object'
/

comment on column HPC_BULK_UPDATE_AUDIT.METADATA_NAME  is 'The metadata attribute name to be updated'
/

comment on column HPC_BULK_UPDATE_AUDIT.METADATA_VALUE is 'The metadata attribute value to be updated'
/

comment on column HPC_BULK_UPDATE_AUDIT.CREATED is 'The timestamp the request was made'
/

