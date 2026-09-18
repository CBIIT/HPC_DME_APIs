--
-- hpc_archive_poc.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:dinhys@nih.gov">Yuri Dinh</a>
--

-- HPC_ARCHIVE_POC
CREATE TABLE HPC_ARCHIVE_POC (
    BASE_PATH VARCHAR2(50) NOT NULL,
    DOC       VARCHAR2(50) NOT NULL,
    POC       VARCHAR2(50) NOT NULL
);

comment on column HPC_ARCHIVE_POC.BASE_PATH is 'The base path of the archive';

comment on column HPC_ARCHIVE_POC.DOC is 'The DOC that own this archive';

comment on column HPC_ARCHIVE_POC.POC is 'The POC of this archive';
