--
-- hpc_data_management_configuration_restore_uat.sql
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

update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = 'c93e82ba-7c66-4463-8376-1c7cb0b1a598' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'FNLCR';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = 'be21cdf5-cdd3-4282-a78f-0d817285394a' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'CCBR';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '63fdccdd-64b8-477f-9e5c-450c4dccf748' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'DUMMY';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '963f55b4-5910-42d2-9cea-c2834ddd0a51' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'CCR-LEEMAX';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '50a8d63b-2eef-47bb-af96-0e333a80eda5' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'HiTIF';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '0812a506-6e2f-4dcc-b11a-8a14cff00819' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'DUMMY_NO_HIER';

update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = 'a4574a94-7bb0-4540-a0d2-a4c1b17e60f3' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'JDACS4C';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = 'e58318ab-6a61-4478-9d72-f43f4a8257d9' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'ATOM';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '3ea3cacb-567c-46fe-bd84-db01e950991c' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'MoCha';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '4864e232-2fcb-4b9d-9144-d30473457575' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'RAS_INF';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = 'd4fdb3ca-f407-4d3e-9fc7-99fdb81431b6' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'BIOWULF';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '1e088147-4fcd-4282-975c-ee7229297a54' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'DSITP_FNL';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '7a4cd4a4-c900-4637-9b99-414a95bd246d' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'CCR_SBL';
update r_meta_main set meta_attr_name = 'configuration_id', meta_attr_value = '942b43f9-343c-4d96-b7b0-34f41571fb23' where meta_attr_name = 'registered_by_doc' and meta_attr_value = 'NIEHS';

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('a4574a94-7bb0-4540-a0d2-a4c1b17e60f3', '/JDACS4C_Archive', 'JDACS4C', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'JDACS4C_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('e58318ab-6a61-4478-9d72-f43f4a8257d9', '/ATOM_Archive', 'ATOM', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'ATOM_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('3ea3cacb-567c-46fe-bd84-db01e950991c', '/MoCha_Archive', 'MoCha', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'MoCha_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('4864e232-2fcb-4b9d-9144-d30473457575', '/RAS_INF_Archive', 'RAS_INF', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'RAS_INF_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('d4fdb3ca-f407-4d3e-9fc7-99fdb81431b6', '/BIOWULF_Archive', 'BIOWULF', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'BIOWULF_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('1e088147-4fcd-4282-975c-ee7229297a54', '/DSITP_FNL_Archive', 'DSITP_FNL', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'DSITP_FNL_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('b0e00125-6385-4d02-8e6c-9a67b02d3b0b', '/CCR_SBL_WangLab_Archive', 'CCR_SBL', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'CCR_SBL_WangLab_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('b0e00125-6385-4d02-8e6c-9a67b02d32cb', '/CCR_SBL_WaltersLab_Archive', 'CCR_SBL', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'CCR_SBL_WaltersLab_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('327c87f0-6623-4e1a-8175-9f337f69374c', '/CCR_SBL_ByrdLab_Archive', 'CCR_SBL', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'CCR_SBL_ByrdLab_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('7a4cd4a4-c900-4637-9b99-414a95bd246d', '/CCR_SBL_Admin_Archive', 'CCR_SBL', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'CCR_SBL_Admin_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('942b43f9-343c-4d96-b7b0-34f41571fb23', '/NIEHS_Archive', 'NIEHS', 'fr-s-clvrsf-01', 'DSE-TestVault2', 'NIEHS_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('c93e82ba-7c66-4463-8376-1c7cb0b1a598', '/FNL_SF_Archive', 'FNLCR', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'FNL_SF_Archive', 'ARCHIVE',
       '{
       "collectionType": "PI_Lab",
       "isDataObjectContainer": false,
       "subCollections": [
        {
           "collectionType": "Project",
           "isDataObjectContainer": true,
           "subCollections": [
            {
               "collectionType": "Flowcell",
               "isDataObjectContainer": true,
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
 "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "Project",
               "PI_Lab",
               "Flowcell",
               "Sample", 
               "Folder"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_id_CSAS_NAS",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_name",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"lab_contact",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"bioinformatics_contact",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_start_date",
            "mandatory":false,
            "defaultValue":"System-Date",
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"grant_funding_agent",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"flowcell_id",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"run_name",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sequencing_platform",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sequencing_application_type",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"read_length",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pooling",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sample_id",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sample_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"initial_sample_concentration_ngul",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"initial_sample_volume_ul",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sfqc_sample_concentration_ngul",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sfqc_sample_size",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"RIN",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"28s18s",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"library_id",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"library_lot",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"library_name",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sfqc_library_concentration_nM",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sfqc_library_size",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"source_id",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"source_name",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"source_organism",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"source_provider",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         }
      ]
	}',
	   '{
 "metadataValidationRules": [  
         {  
            "attribute":"object_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"file_type",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"reference_genome",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"reference_annotation",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"software_tool",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"md5_checksum",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"phi_content",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "defaultValue":"Unspecified",
            "validValues":[  
               "Unspecified",
               "PHI Present",
               "PHI Not Present",
               "Not Specified"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pii_content",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "defaultValue":"Unspecified",
            "validValues":[  
               "Unspecified",
               "PII Present",
               "PII Not Present",
               "Not Specified"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"data_encryption_status",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "defaultValue":"Unspecified",
            "validValues":[  
               "Unspecified",
               "Encrypted",
               "Not Encrypted",
               "Not Specified"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"data_compression_status",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "defaultValue":"Unspecified",
            "validValues":[  
               "Unspecified",
               "Compressed",
               "Not Compressed",
               "Not Specified"
            ],
            "ruleEnabled":true
         }
      ]
    }');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('be21cdf5-cdd3-4282-a78f-0d817285394a', '/CCBR_Archive', 'CCBR', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 
       'CCBR_Archive', 'ARCHIVE', NULL, NULL, NULL);

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
        	},
        	{
          	 "collectionType": "No_metadata_folder",
          	 "isDataObjectContainer": true
        	}
      	 ]
        }',
       '{
		 "metadataValidationRules": [
        {
            "attribute": "collection_type",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Project",
                "Dataset",
                "Run",
                "No_metadata_folder",
                "Folder"
            ]
        },
        {
            "attribute": "name",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "description",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "project_type",
            "collectionTypes": [
                "Project"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Umbrella Project",
                "Sequencing",
                "Analysis"
            ]
        },
        {
            "attribute": "internal_project_id",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "source_lab_pi",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "lab_branch",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "pi_doc",
            "collectionTypes": [
                "Project"
            ],
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "FNLCR",
                "DUMMY",
                "CCBR"
            ]
        },
        {
            "attribute": "original_date_created",
            "collectionTypes": [
                "Project"
            ],
            "defaultValue": "System-Date",
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "name",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "description",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "phi_content",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "PHI Present",
                "PHI Not Present",
                "Not Specified"
            ]
        },
        {
            "attribute": "pii_content",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "PII Present",
                "PII Not Present",
                "Not Specified"
            ]
        },
        {
            "attribute": "data_encryption_status",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Encrypted",
                "Not Encrypted",
                "Not Specified"
            ]
        },
        {
            "attribute": "data_compression_status",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Compressed",
                "Not Compressed",
                "Not Specified"
            ]
        },
        {
            "attribute": "funding_organization",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": false,
            "ruleEnabled": true
        },
        {
            "attribute": "flow_cell_id",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "run_id",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "run_date",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "sequencing_platform",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Illumina-MiSeq",
                "Illumina-NextSeq",
                "Illumina-HiSeq",
                "Illumina-HiSeq X",
                "Ion-PGM",
                "Ion-Proton",
                "PacBio-PacBio RS"
            ]
        },
        {
            "attribute": "sequencing_application_type",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "DNA-Seq (Re-Sequencing)",
                "DNA-Seq (De novo assembly)",
                "SNP Analysis / Rearrangement Detection",
                "Exome",
                "ChiP-Seq"
            ]
        },
        {
            "attribute": "library_id",
            "collectionTypes": [
                "Dataset"
            ],
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "library_name",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        },
        {
            "attribute": "library_type",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        },
        {
            "attribute": "library_protocol",
            "collectionTypes": [
                "Dataset"
            ],
            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        }
    ]
	    }',
	   '{
		 "metadataValidationRules": [
        {
            "attribute": "name",
            "mandatory": true,
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
            "ruleEnabled": true
        },
        {
            "attribute": "description",
            "mandatory": true,
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
            "ruleEnabled": true
        },
        {
            "attribute": "source_lab_pi",
            "mandatory": true,
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],

            "ruleEnabled": true
        },
        {
            "attribute": "lab_branch",
            "mandatory": true,
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],

            "ruleEnabled": true
        },
        {
            "attribute": "pi_doc",
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],

            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "FNLCR",
                "DUMMY",
                "CCBR"
            ]
        },
        {
            "attribute": "original_date_created",
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
            "defaultValue": "System-Date",
            "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "phi_content",
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],


            "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "PHI Present",
                "PHI Not Present",
                "Not Specified"
            ]
        },
        {
            "attribute": "pii_content",
            "defaultValue": "Unspecified",
            "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],

            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "PII Present",
                "PII Not Present",
                "Not Specified"
            ]
        },
        {
            "attribute": "data_encryption_status",
            "defaultValue": "Unspecified",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Encrypted",
                "Not Encrypted",
                "Not Specified"
            ]
        },
        {
            "attribute": "data_compression_status",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
           "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Compressed",
                "Not Compressed",
                "Not Specified"
            ]
        },
        {
            "attribute": "funding_organization",
              "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
          "defaultValue": "Unspecified",
            "mandatory": false,
            "ruleEnabled": true
        },
        {
            "attribute": "flow_cell_id",
               "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
         "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "run_id",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "run_date",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "sequencing_platform",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "Illumina-MiSeq",
                "Illumina-NextSeq",
                "Illumina-HiSeq",
                "Illumina-HiSeq X",
                "Ion-PGM",
                "Ion-Proton",
                "PacBio-PacBio RS"
            ]
        },
        {
            "attribute": "sequencing_application_type",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "DNA-Seq (Re-Sequencing)",
                "DNA-Seq (De novo assembly)",
                "SNP Analysis / Rearrangement Detection",
                "Exome",
                "ChiP-Seq"
            ]
        },
        {
            "attribute": "library_id",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "mandatory": true,
            "ruleEnabled": true
        },
        {
            "attribute": "library_name",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        },
        {
            "attribute": "library_type",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        },
        {
            "attribute": "library_protocol",
             "collectionTypes": [
                "Project",
                "Dataset", 
                "Folder"
            ],
           "defaultValue": "Unspecified",
            "mandatory": true,
            "ruleEnabled": true,
            "validValues": [
                "Unspecified",
                "TruSeq ChIP-Seq",
                "Illumina TrueSeq"
            ]
        }
    ]
	    }');

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('963f55b4-5910-42d2-9cea-c2834ddd0a51', '/CCR_LEEMAX_Archive', 'CCR-LEEMAX', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'CCR_LEEMAX_Archive',
       'ARCHIVE', NULL, NULL, NULL);

INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('50a8d63b-2eef-47bb-af96-0e333a80eda5', '/HiTIF_Archive', 'HiTIF', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'HiTIF_Archive',
       'ARCHIVE', NULL, NULL, NULL);
                                                  
INSERT INTO public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
VALUES('0812a506-6e2f-4dcc-b11a-8a14cff00819', '/TEST_NO_HIER_Archive', 'DUMMY_NO_HIER', 'http://fr-s-clvrsf-01.ncifcrf.gov', 'DSE-TestVault1', 'NO_HIER_Archive',
       'ARCHIVE', NULL, NULL, NULL);