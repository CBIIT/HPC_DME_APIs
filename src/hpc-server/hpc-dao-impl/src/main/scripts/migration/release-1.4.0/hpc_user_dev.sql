--
-- hpc_user_dev.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
--

ALTER TABLE public."HPC_USER" ADD COLUMN "DEFAULT_CONFIGURATION_ID" text;

update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'cd8ffbe3-d2b7-4125-a1ca-acb808fc90f0' where "DOC" = 'NOHIERARCHY';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'c93e82ba-7c66-4463-8376-1c7cb0b1a598' where "DOC" = 'FNLCR';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'be21cdf5-cdd3-4282-a78f-0d817285394a' where "DOC" = 'CCBR';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '63fdccdd-64b8-477f-9e5c-450c4dccf748' where "DOC" = 'DUMMY';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '963f55b4-5910-42d2-9cea-c2834ddd0a51' where "DOC" = 'CCR-LEEMAX';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '50a8d63b-2eef-47bb-af96-0e333a80eda5' where "DOC" = 'HiTIF';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '0812a506-6e2f-4dcc-b11a-8a14cff00819' where "DOC" = 'DUMMY_NO_HIER';

--ALTER TABLE public."HPC_USER" DROP COLUMN "DOC";

