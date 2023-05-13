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

update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'a4574a94-7bb0-4540-a0d2-a4c1b17e60f3' where "DOC" = 'JDACS4C';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'e58318ab-6a61-4478-9d72-f43f4a8257d9' where "DOC" = 'ATOM';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '3ea3cacb-567c-46fe-bd84-db01e950991c' where "DOC" = 'MoCha';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '4864e232-2fcb-4b9d-9144-d30473457575' where "DOC" = 'RAS_INF';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = 'd4fdb3ca-f407-4d3e-9fc7-99fdb81431b6' where "DOC" = 'BIOWULF';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '1e088147-4fcd-4282-975c-ee7229297a54' where "DOC" = 'DSITP_FNL';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '7a4cd4a4-c900-4637-9b99-414a95bd246d' where "DOC" = 'CCR_SBL';
update public."HPC_USER" set "DEFAULT_CONFIGURATION_ID" = '942b43f9-343c-4d96-b7b0-34f41571fb23' where "DOC" = 'NIEHS';
