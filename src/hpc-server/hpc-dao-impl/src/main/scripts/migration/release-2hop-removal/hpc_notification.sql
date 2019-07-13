--
-- hpc_notification.sql
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

DELETE FROM public."HPC_NOTIFICATION_SUBSCRIPTION" WHERE "EVENT_TYPE" = 'DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE';
