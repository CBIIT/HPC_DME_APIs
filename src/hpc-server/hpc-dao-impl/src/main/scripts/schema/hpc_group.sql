create table irods."HPC_GROUP"
(
    "GROUP_NAME"                  VARCHAR2(250) not null
        constraint "HPC_GROUP_pkey"
            primary key,
    "CREATED"                  timestamp,
    "LAST_UPDATED"             timestamp,
    "DOC"                      clob,
    "ACTIVE"                   char(1),
    "ACTIVE_UPDATED_BY"        clob
);

comment on table "HPC_GROUP" is 'HPC-DME Groups';

comment on column "HPC_GROUP"."GROUP_NAME" is 'The group name';

comment on column "HPC_GROUP"."CREATED" is 'The date / time the group was created';

comment on column "HPC_GROUP"."LAST_UPDATED" is 'The date / time the group was updated';

comment on column "HPC_GROUP"."DOC" is 'The DOC the group belongs to';

comment on column "HPC_GROUP"."ACTIVE" is 'User active indicator';

comment on column "HPC_GROUP"."ACTIVE_UPDATED_BY" is 'The administrator user ID that activated this group';

