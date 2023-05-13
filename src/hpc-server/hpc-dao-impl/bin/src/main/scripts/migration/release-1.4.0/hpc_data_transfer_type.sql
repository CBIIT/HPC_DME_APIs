--
-- hpc_data_transfer_type.sql
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

UPDATE public."HPC_DATA_OBJECT_DOWNLOAD_TASK"
   SET  
       "DATA_TRANSFER_TYPE"='S_3'
 WHERE "DATA_TRANSFER_TYPE"='S3';

UPDATE public."HPC_DATA_TRANSFER_UPLOAD_QUEUE"
   SET "DATA_TRANSFER_TYPE"='S_3'
 WHERE "DATA_TRANSFER_TYPE"='S3';

UPDATE public."HPC_DOWNLOAD_TASK_RESULT"
   SET  
       "DATA_TRANSFER_TYPE"='S_3'
 WHERE "DATA_TRANSFER_TYPE"='S3';

update r_meta_main 
	set  meta_attr_value='S_3' 
where meta_attr_name='data_transfer_type' and meta_attr_value='S3';

UPDATE public."HPC_SYSTEM_ACCOUNT"
   SET "DATA_TRANSFER_TYPE"='S_3'
 WHERE "DATA_TRANSFER_TYPE"='S3';