--
-- hpc_user_query.sql
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

ALTER TABLE HPC_USER_QUERY add (DESELECTED_COLUMNS CLOB);
COMMENT ON COLUMN HPC_USER_QUERY.DESELECTED_COLUMNS IS
                 'A list of user deselected columns to hide from search results';
