DROP TABLE IF EXISTS public."HPC_GROUP";
create table PUBLIC."HPC_GROUP"
(
    "GROUP_NAME"                  text not null
        constraint "HPC_GROUP_pkey"
            primary key,
    "CREATED"                  timestamp,
    "LAST_UPDATED"             timestamp,
    "DOC"                      text,
    "ACTIVE"                   boolean,
    "ACTIVE_UPDATED_BY"        text
);

comment on table "HPC_GROUP" is 'HPC-DME Groups';

comment on column "HPC_GROUP"."GROUP_NAME" is 'The group name';

comment on column "HPC_GROUP"."CREATED" is 'The date / time the group was created';

comment on column "HPC_GROUP"."LAST_UPDATED" is 'The date / time the group was updated';

comment on column "HPC_GROUP"."DOC" is 'The DOC the group belongs to';

comment on column "HPC_GROUP"."ACTIVE" is 'User active indicator';

comment on column "HPC_GROUP"."ACTIVE_UPDATED_BY" is 'The administrator user ID that activated this group';


alter table "HPC_GROUP"
    owner to irods;


insert into public."HPC_GROUP"("GROUP_NAME", "ACTIVE", "DOC", "CREATED", "LAST_UPDATED") SELECT user_name, true, 'FNLCR', to_timestamp(cast(create_ts as double precision)), to_timestamp(cast(modify_ts as double precision)) from "r_user_main" where user_type_name = 'rodsgroup';

