--
-- hpc_s3_lifecycle.sql
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

CREATE TABLE "IRODS"."HPC_S3_LIFECYCLE_RULE"
(
   USER_ID VARCHAR2(50) NOT NULL,
   REQUEST_TYPE VARCHAR2(50) NOT NULL,
   S3_ARCHIVE_CONFIGURATION_ID VARCHAR2(50) NOT NULL,
   FILTER_PREFIX VARCHAR2(2700) NOT NULL,
   COMPLETED timestamp NOT NULL
)
;
comment on table IRODS."HPC_S3_LIFECYCLE_RULE" is 'S3 lifecycle policy rules applied to a bucket';

comment on column IRODS."HPC_S3_LIFECYCLE_RULE"."USER_ID" is 'The user ID who added the rule';

comment on column IRODS."HPC_S3_LIFECYCLE_RULE"."REQUEST_TYPE" is 'The request type of lifecycle rule';

comment on column IRODS."HPC_S3_LIFECYCLE_RULE"."S3_ARCHIVE_CONFIGURATION_ID" is 'The S3 archive configuration ID';

comment on column IRODS."HPC_S3_LIFECYCLE_RULE"."FILTER_PREFIX" IS 'The lifecycle policy filter prefix that was applied';

comment on column IRODS."HPC_S3_LIFECYCLE_RULE"."COMPLETED" is 'The date/time the lifecycle rule was added';
