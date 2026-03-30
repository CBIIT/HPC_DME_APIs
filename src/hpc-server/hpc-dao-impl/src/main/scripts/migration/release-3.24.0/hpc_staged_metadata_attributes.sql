--
-- hpc_staged_metadata_attributes.sql
--
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
-- @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
-- @version $Id$
--

create table irods.hpc_staged_metadata_attributes
(
    PATH     VARCHAR2(2700),
    META_ATTR_NAME  VARCHAR2(2700)         not null,
    META_ATTR_VALUE VARCHAR2(2700)         not null,
    IN_PROCESS      NUMBER(1)  default 0   not null
);

create table irods.hpc_migrated_metadata_attributes
(
    PATH     VARCHAR2(2700),
    META_ATTR_NAME  VARCHAR2(2700)         not null,
    META_ATTR_VALUE VARCHAR2(2700)         not null,
    COMPLETED TIMESTAMP(6)
);

