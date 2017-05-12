--
-- hpc_doc_configuration_dev.sql
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

INSERT INTO public."HPC_DOC_CONFIGURATION" VALUES('NOHIERARCHY', '/NO_HIERARCHY', NULL);

INSERT INTO public."HPC_DOC_CONFIGURATION" 
VALUES('FNLCR', '/FNL_SF_Archive',
       '{
      	 "collectionType": "PI_Lab",
      	 "isDataObjectContainer": false,
      	 "subCollections": [
        	{
          	 "collectionType": "Project",
          	 "isDataObjectContainer": false,
          	 "subCollections": [
            	{
              	 "collectionType": "Flowcell",
              	 "isDataObjectContainer": false,
              	 "subCollections": [
                	{
                     "collectionType": "Sample",
                     "isDataObjectContainer": true
                	}
              	 ]
               }
          	 ]
        	}
      	 ]
    	}');

INSERT INTO public."HPC_DOC_CONFIGURATION" VALUES('CCBR', '/CCBR_SF_Archive', NULL);

INSERT INTO public."HPC_DOC_CONFIGURATION" 
VALUES('DUMMY', '/TEST_Archive',
       '{
      	 "collectionType": "Project",
      	 "isDataObjectContainer": true,
      	 "subCollections": [
        	{
          	 "collectionType": "Dataset",
          	 "isDataObjectContainer": true
        	},
        	{
          	 "collectionType": "Run",
          	 "isDataObjectContainer": false
        	},
        	{
          	 "collectionType": "Folder",
          	 "isDataObjectContainer": true
        	}
      	 ]
        }');

INSERT INTO public."HPC_DOC_CONFIGURATION" VALUES('CCR-LEEMAX', '/CCR_LEEMAX_Archive', NULL);

INSERT INTO public."HPC_DOC_CONFIGURATION" VALUES('HiTIF', '/HiTIF_Archive', NULL);

