--
-- hpc_data_management_configuration_dev.sql
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

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('cd8ffbe3-d2b7-4125-a1ca-acb808fc90f0', '/NO_HIERARCHY', 'NOHIERARCHY', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'NO_HIERARCHY',
       'ARCHIVE', NULL, NULL, NULL, 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('c93e82ba-7c66-4463-8376-1c7cb0b1a598', '/FNL_SF_Archive', 'FNLCR', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'FNL_SF_Archive', 'ARCHIVE',
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
    	}',
       '{
		 "metadataValidationRules": [{
				"attribute": "collection_type",
				"mandatory": true,
				"validValues": [
					"Project",
					"PI_Lab",
					"Flowcell",
					"Sample",
					"Folder"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pi_name",
				"mandatory": true,
				"collectionTypes": [
					"PI_Lab"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "project_id_CSAS",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "project_name",
				"mandatory": false,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "lab_contact",
				"mandatory": false,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "bioinformatics_contact",
				"mandatory": false,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "project_start_date",
				"mandatory": false,
				"defaultValue": "System-Date",
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "grant_funding_agent",
				"mandatory": false,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "flowcell_id",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "run_name",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "run_date",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_platform",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_application_type",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "read_length",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pooling",
				"mandatory": true,
				"collectionTypes": [
					"Flowcell"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sample_id",
				"mandatory": true,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sample_name",
				"mandatory": true,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "initial_sample_concentration_ngul",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "initial_sample_volume_ul",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sfqc_sample_concentration_ngul",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sfqc_sample_size",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "RIN",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "28s18s",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_id",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_lot",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_name",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sfqc_library_concentration_nM",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sfqc_library_size",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_id",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_name",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_organism",
				"mandatory": true,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_provider",
				"mandatory": false,
				"collectionTypes": [
					"Sample"
				],
				"ruleEnabled": true
			}
		 ]
		}',
	   '{
		 "metadataValidationRules": [{
				"attribute": "object_name",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "file_type",
				"mandatory": false,
				"ruleEnabled": true
			},
			{
				"attribute": "reference_genome",
				"mandatory": false,
				"ruleEnabled": true
			},
			{
				"attribute": "reference_annotation",
				"mandatory": false,
				"ruleEnabled": true
			},
			{
				"attribute": "software_tool",
				"mandatory": false,
				"ruleEnabled": true
			},
			{
				"attribute": "md5_checksum",
				"mandatory": false,
				"ruleEnabled": true
			},
			{
				"attribute": "phi_content",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PHI Present",
					"PHI Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pii_content",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PII Present",
					"PII Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_encryption_status",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Encrypted",
					"Not Encrypted",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_compression_status",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Compressed",
					"Not Compressed",
					"Not Specified"
				],
				"ruleEnabled": true
			}
		 ]
	    }', 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('be21cdf5-cdd3-4282-a78f-0d817285394a', '/CCBR_SF_Archive', 'CCBR', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 
       'CCBR_SF_Archive', 'ARCHIVE', NULL, NULL, NULL, 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('63fdccdd-64b8-477f-9e5c-450c4dccf748', '/TEST_Archive', 'DUMMY', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'DUMMY_Archive', 'ARCHIVE',
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
        }',
       '{
		 "metadataValidationRules": [{
				"attribute": "collection_type",
				"mandatory": true,
				"validValues": [
					"Project",
					"Dataset",
					"Run",
					"Folder"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "name",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "description",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "project_type",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"collectionTypes": [
					"Project"
				],
				"validValues": [
					"Unspecified",
					"Umbrella Project",
					"Sequencing",
					"Analysis"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "internal_project_id",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_lab_pi",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "lab_branch",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pi_doc",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"validValues": [
					"FNLCR",
					"DUMMY",
					"CCBR"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "original_date_created",
				"mandatory": true,
				"collectionTypes": [
					"Project"
				],
				"defaultValue": "System-Date",
				"ruleEnabled": true
			},
			{
				"attribute": "name",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "description",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "source_lab_pi",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "lab_branch",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pi_doc",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"validValues": [
					"FNLCR",
					"DUMMY",
					"CCBR"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "original_date_created",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "System-Date",
				"ruleEnabled": true
			},
			{
				"attribute": "phi_content",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PHI Present",
					"PHI Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pii_content",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PII Present",
					"PII Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_encryption_status",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Encrypted",
					"Not Encrypted",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_compression_status",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Compressed",
					"Not Compressed",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "funding_organization",
				"mandatory": false,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"ruleEnabled": true
			},
			{
				"attribute": "flow_cell_id",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "run_id",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "run_date",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_platform",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Illumina-MiSeq",
					"Illumina-NextSeq",
					"Illumina-HiSeq",
					"Illumina-HiSeq X",
					"Ion-PGM",
					"Ion-Proton",
					"PacBio-PacBio RS"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_application_type",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"DNA-Seq (Re-Sequencing)",
					"DNA-Seq (De novo assembly)",
					"SNP Analysis / Rearrangement Detection",
					"Exome",
					"ChiP-Seq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_id",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_name",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_type",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_protocol",
				"mandatory": true,
				"collectionTypes": [
					"Dataset"
				],
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			}
		 ]
	    }',
	   '{
		 "metadataValidationRules": [{
				"attribute": "name",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "description",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "source_lab_pi",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "lab_branch",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "pi_doc",
				"mandatory": true,
				"validValues": [
					"FNLCR",
					"DUMMY",
					"CCBR"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "original_date_created",
				"mandatory": true,
				"defaultValue": "System-Date",
				"ruleEnabled": true
			},
			{
				"attribute": "phi_content",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PHI Present",
					"PHI Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "pii_content",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"PII Present",
					"PII Not Present",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_encryption_status",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Encrypted",
					"Not Encrypted",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "data_compression_status",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Compressed",
					"Not Compressed",
					"Not Specified"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "funding_organization",
				"mandatory": false,
				"defaultValue": "Unspecified",
				"ruleEnabled": true
			},
			{
				"attribute": "flow_cell_id",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "run_id",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "run_date",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_platform",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"Illumina-MiSeq",
					"Illumina-NextSeq",
					"Illumina-HiSeq",
					"Illumina-HiSeq X",
					"Ion-PGM",
					"Ion-Proton",
					"PacBio-PacBio RS"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "sequencing_application_type",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"DNA-Seq (Re-Sequencing)",
					"DNA-Seq (De novo assembly)",
					"SNP Analysis / Rearrangement Detection",
					"Exome",
					"ChiP-Seq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_id",
				"mandatory": true,
				"ruleEnabled": true
			},
			{
				"attribute": "library_name",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_type",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			},
			{
				"attribute": "library_protocol",
				"mandatory": true,
				"defaultValue": "Unspecified",
				"validValues": [
					"Unspecified",
					"TruSeq ChIP-Seq",
					"Illumina TrueSeq"
				],
				"ruleEnabled": true
			}
		 ]
	    }', 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('963f55b4-5910-42d2-9cea-c2834ddd0a51', '/CCR_LEEMAX_Archive', 'CCR-LEEMAX', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'CCR_LEEMAX_Archive',
       'ARCHIVE', NULL, NULL, NULL, 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('50a8d63b-2eef-47bb-af96-0e333a80eda5', '/HiTIF_Archive', 'HiTIF', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'HiTIF_Archive',
       'ARCHIVE', NULL, NULL, NULL, 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');
                                                  
INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('0812a506-6e2f-4dcc-b11a-8a14cff00819', '/TEST_NO_HIER_Archive', 'DUMMY_NO_HIER', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'NO_HIER_Archive',
       'ARCHIVE', NULL, NULL, NULL, 24, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/ServiceFolderFor2Hop', '/mnt/IRODsTest/FNL_SF_Share/ServiceFolderFor2Hop', 'TEMPORARY_ARCHIVE', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FNL_SF_S3_Download', '/mnt/IRODsTest/FNL_SF_Share/FNL_SF_S3_Download');
       
       
INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('41ff9b3a-dfb1-11e7-80c1-9a214cf093ae', '/FS_ARCHIVE', 'FS_ARCHIVE', NULL, NULL, NULL,
       NULL, NULL, NULL, NULL, NULL, 'https://auth.globus.org/v2/oauth2/token', 'c6790626-aab4-11e7-aef3-22000a92523b', '/FS_ARCHIVE', '/mnt/IRODsTest/FNL_SF_Share/FS_ARCHIVE', 'ARCHIVE', NULL,  NULL, NULL);


