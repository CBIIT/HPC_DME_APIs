--
-- hpc_test_archive.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
--

update "HPC_DATA_MANAGEMENT_CONFIGURATION" set "DOC" = 'TEST' where "DOC"='DUMMY';
update "HPC_DATA_MANAGEMENT_CONFIGURATION" set "S3_OBJECT_ID" = 'TEST_Archive' where "DOC"='DUMMY_Archive';

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION"(
            "ID", "BASE_PATH", "DOC", "S3_URL", "S3_VAULT", "S3_OBJECT_ID", 
            "S3_ARCHIVE_TYPE", "DATA_HIERARCHY", "COLLECTION_METADATA_VALIDATION_RULES", 
            "DATA_OBJECT_METADATA_VALIDATION_RULES", "S3_UPLOAD_REQUEST_URL_EXPIRATION", 
            "GLOBUS_URL", "GLOBUS_ARCHIVE_ENDPOINT", "GLOBUS_ARCHIVE_PATH", 
            "GLOBUS_ARCHIVE_DIRECTORY", "GLOBUS_ARCHIVE_TYPE", "GLOBUS_DOWNLOAD_ENDPOINT", 
            "GLOBUS_DOWNLOAD_PATH", "GLOBUS_DOWNLOAD_DIRECTORY")
    VALUES ('63fdccdd-64b8-477f-9e5c-450c4dccf45ftest', '/TEST_FS_Archive', 'TEST', null, null, null, 
            null, null, null, 
            null, null, 
            'https://auth.globus.org/v2/oauth2/token', 'N/A', '/TEST_FS_ARCHIVE', 
            '/mnt/IRODsTest/FNL_SF_Share/TEST_FS_ARCHIVE', 'ARCHIVE', null, 
            null, null);

update "HPC_USER" set "DOC"='TEST' where "DOC"='DUMMY';
                                   
