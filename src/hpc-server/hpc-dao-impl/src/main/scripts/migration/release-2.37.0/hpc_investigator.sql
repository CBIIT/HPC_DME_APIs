
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
