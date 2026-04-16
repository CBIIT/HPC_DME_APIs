--
-- hpc_collection_download_task.sql
--
-- Distributed under the OSI-approved BSD 3-Clause License.
-- See http://ncip.github.com/HPC/LICENSE.txt for details.
--
-- @author <a href="mailto:sunita.menon@nih.gov@nih.gov">Sunita Menon</a>
--

ALTER TABLE HPC_COLLECTION_DOWNLOAD_TASK
    add DATA_SIZE NUMBER(24)
COMMENT ON COLUMN DATA_SIZE IS 'The requested download size'
