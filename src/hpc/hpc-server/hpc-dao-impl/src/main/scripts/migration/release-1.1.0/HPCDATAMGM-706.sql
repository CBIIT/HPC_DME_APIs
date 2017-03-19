--
-- HPCDATAMGM-706.sql
--
-- Copyright SVG, Inc.
-- Copyright Leidos Biomedical Research, Inc
-- 
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
--
-- @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
-- @version $Id$
--

ALTER TABLE public."HPC_USER" DROP COLUMN "IRODS_USERNAME";
ALTER TABLE public."HPC_USER" DROP COLUMN "IRODS_PASSWORD";
