
create table HPC_INVESTIGATOR
(
    NED_ID                    VARCHAR2(10)   not null,
    FIRST_NAME                VARCHAR2(50)   not null,
    LAST_NAME                 VARCHAR2(50)   not null,
    IC                        VARCHAR2(50)   not null,
    DIVISION                  VARCHAR2(50)   not null,
    LAB_BRANCH                VARCHAR2(50)   not null,
    AD_FIRST_NAME             VARCHAR2(50),
    AD_LAST_NAME              VARCHAR2(50),
    AD_NIH_SAC                VARCHAR2(50)
)
/

comment on table HPC_INVESTIGATOR is 'Lists all Z01 award recepients'
/

comment on column HPC_INVESTIGATOR.NED_ID is 'The NED ID of the PI'
/

comment on column HPC_INVESTIGATOR.FIRST_NAME is 'The first name of the PI'
/

comment on column HPC_INVESTIGATOR.LAST_NAME is 'The last name of the PI'
/

comment on column HPC_INVESTIGATOR.IC is 'The IC of the PI'
/

comment on column HPC_INVESTIGATOR.DIVISION is 'The Division of the PI'
/

comment on column HPC_INVESTIGATOR.LAB_BRANCH is 'The lab/branch of the PI'
/

comment on column HPC_INVESTIGATOR.AD_FIRST_NAME is 'The AD first name of the PI'
/

comment on column HPC_INVESTIGATOR.AD_LAST_NAME is 'The AD last name of the PI'
/

comment on column HPC_INVESTIGATOR.AD_NIH_SAC is 'The AD NIH SAC of the PI'
/



insert into HPC_INVESTIGATOR ('NED_ID','FIRST_NAME','LAST_NAME','IC','DIVISION','LAB_BRANCH') values ('ABNET, CHRISTIAN','ABNET',' CHRISTIAN','DCEG','METABOLIC EPIDEMIOLOGY BRANCH');



-- Script to find out the data owners that are not formatted as Last, First

select data_owner, data_owner_last_name, data_owner_first_name, doc from (
select data_owner,
       CASE WHEN data_owner LIKE '%,%' THEN trim(substr(data_owner, instr(data_owner,',') + 1))
            WHEN data_owner LIKE '% %' THEN trim(substr(data_owner, 1,instr(data_owner,' ') - 1))
            WHEN data_owner LIKE '%_%' and trim(substr(data_owner, 1,instr(data_owner,'_') - 1)) is not null THEN trim(substr(data_owner, 1,instr(data_owner,'_') - 1))
            ELSE data_owner
            END data_owner_first_name,
       CASE WHEN data_owner LIKE '%,%' THEN trim(substr(data_owner, 1, instr(data_owner,',') - 1))
            WHEN data_owner LIKE '% %' THEN trim(substr(data_owner, instr(data_owner,' ') + 1))
            WHEN data_owner LIKE '%_%' and trim(substr(data_owner, instr(data_owner,'_') + 1)) is not null THEN trim(substr(data_owner, instr(data_owner,'_') + 1))
            ELSE data_owner
           END data_owner_last_name,
doc
from r_coll_hierarchy_data_owner) data_owner where doc <> 'NCEF' and data_owner not like '%,%';

-- Script to find out the data owners that are not in the Z01 list

select data_owner, data_owner_last_name, data_owner_first_name, dme_data_owner.doc
from (
select data_owner,
       CASE WHEN data_owner LIKE '%,%' THEN trim(substr(data_owner, instr(data_owner,',') + 1))
            WHEN data_owner LIKE '% %' THEN trim(substr(data_owner, 1,instr(data_owner,' ') - 1))
            WHEN data_owner LIKE '%_%' and trim(substr(data_owner, 1,instr(data_owner,'_') - 1)) is not null THEN trim(substr(data_owner, 1,instr(data_owner,'_') - 1))
            ELSE data_owner
            END data_owner_first_name,
       CASE WHEN data_owner LIKE '%,%' THEN trim(substr(data_owner, 1, instr(data_owner,',') - 1))
            WHEN data_owner LIKE '% %' THEN trim(substr(data_owner, instr(data_owner,' ') + 1))
            WHEN data_owner LIKE '%_%' and trim(substr(data_owner, instr(data_owner,'_') + 1)) is not null THEN trim(substr(data_owner, instr(data_owner,'_') + 1))
            ELSE data_owner
           END data_owner_last_name,
doc
from r_coll_hierarchy_data_owner) dme_data_owner
where doc <> 'NCEF' and not exists (select 1 from HPC_INVESTIGATOR investigator where
      lower(trim(investigator.LAST_NAME)) like '%' || lower(dme_data_owner.data_owner_last_name) || '%'
      and lower(trim(investigator.FIRST_NAME)) like '%' || lower(dme_data_owner.data_owner_first_name) || '%')
group by data_owner, data_owner_last_name, data_owner_first_name, dme_data_owner.doc
order by data_owner_last_name, data_owner_first_name;