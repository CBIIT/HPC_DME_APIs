--
-- hpc_globus_transfer_task.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
--

create table HPC_GLOBUS_TRANSFER_TASK
(
    GLOBUS_ACCOUNT 			  VARCHAR2(250)    not null,
    DATA_TRANSFER_REQUEST_ID  VARCHAR2(50)     not null,
    PATH                      VARCHAR2(2700)   not null,
    DOWNLOAD                  CHAR default '0' not null,
    CREATED                   TIMESTAMP(6)     not null
);

comment on table HPC_GLOBUS_TRANSFER_TASK is 'Lists all ongoing Globus transfer requests';
comment on column HPC_GLOBUS_TRANSFER_TASK.GLOBUS_ACCOUNT is 'The globus account used for the transfer';
comment on column HPC_GLOBUS_TRANSFER_TASK.DATA_TRANSFER_REQUEST_ID is 'The globus transfer request ID';
comment on column HPC_GLOBUS_TRANSFER_TASK.PATH is 'The DME path';
comment on column HPC_GLOBUS_TRANSFER_TASK.DOWNLOAD is 'An indicator if the transfer is a download task';
comment on column HPC_GLOBUS_TRANSFER_TASK.CREATED is 'The date and time the request was created';


