UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "Project",
               "PI_Lab",
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"affiliation",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"Organization to which the PI is affiliated"
         },
         {  
            "attribute":"project_id",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_title",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Title of the project"
         },
         {  
            "attribute":"project_description",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Similar to an abstract; include details such as type of patients, trial phase, target genes under study, drugs being tested"
         },
         {  
            "attribute":"method",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
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
            "attribute":"start_date",
            "mandatory":true,
            "defaultValue":"System-Date",
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Start date of the project"
         },
         {  
            "attribute":"collaborator",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"collaborator_affiliation",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"publications",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"comments",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sample_id",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Sample identifier, Same as BioSample in dbGap"
         },
         {  
            "attribute":"sample_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the sample"
         },
         {  
            "attribute":"sequencing_application_type",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Same as Assay Type in dbGap e.g RNA-Seq, DNA-Seq"
         },
         {  
            "attribute":"sample_type",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Same as Analyte Type in dbGap e.g. RNA, DNA"
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
            "ruleEnabled":true,
            "description":"Source organism e.g. Homon Sapiens (Human)"
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
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"file_type",
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
         }
      ]
    }' 
WHERE "BASE_PATH"='/CCBR_EXT_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
     {  
         "attribute":"collection_type",
         "mandatory":true,
         "validValues":[  
         "PI_Lab",
         "Project",
         "Summary",
         "Report",
         "Raw_Data",
         "Analysis",
         "Sample",
         "Supplement",
         "QC",
         "Dataset",
         "Output",
         "Setup"
        ],
        "ruleEnabled":true
      },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"affiliation",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"Organization to which the PI is affiliated"
         },
         {  
            "attribute":"project_id",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_title",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Title of the project"
         },
         {  
            "attribute":"project_description",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Similar to an abstract; include details such as type of patients, trial phase, target genes under study, drugs being tested"
         },
         {
            "attribute":"method",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
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
            "attribute":"start_date",
            "mandatory":true,
            "defaultValue":"System-Date",
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Start date of the project"
         },
         {  
            "attribute":"collaborator",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"collaborator_affiliation",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"publications",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"comments",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"software",
            "mandatory":true,
            "collectionTypes":[  
               "Analysis",
               "QC"
            ],
            "ruleEnabled":true,
            "description":"Software used"
         },
         {  
            "attribute":"assay_type",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"dataset_id",
            "mandatory":false,
            "collectionTypes":[  
               "Dataset"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"data_type",
            "mandatory":true,
            "collectionTypes":[  
               "Raw_Data"
            ],
            "ruleEnabled":true,
            "description":"Type of data stored"
         }   
      ]
}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"file_type",
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
         }
      ]
    }' 
WHERE "BASE_PATH"='/SRP_SEER_EXT_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
   "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "Project",
               "PI_Lab",
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"affiliation",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"Organization to which the PI is affiliated"
         },
         {  
            "attribute":"project_title",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Title of the project"
         },
         {  
            "attribute":"project_description",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Similar to an abstract; include details such as type of patients, trial phase, target genes under study, drugs being tested"
         },
         {
            "attribute":"origin",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Origin of the project"
         },
         {
            "attribute":"method",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
         },
         {
            "attribute":"start_date",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Start date of the project"
         },
         {
            "attribute":"access",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "validValues":[  
               "Closed Access",
               "Controlled Access",
               "Open Access"
            ],
            "ruleEnabled":true,
            "description":"<b>Closed access</b>:  data catalog entry will not be exposed.<br/>
<b>Controlled access</b>: data catalog entry will be exposed; access to full data sets is by permission only.<br/>
<b>Open access</b>: data catalog entry will be exposed; access to full data does not require permission."
         },
          {  
            "attribute":"project_affiliation",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_poc",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"publication_status",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"origin_repository",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"origin_accession_id",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"study_id",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"study_design",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"study_disease",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"assembly_name",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"cell_line_name",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"number_of_cases",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"summary_of_samples",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Summarize details about the samples represented in this dataset. For example, are these cell lines or clinical samples?  How many samples and of what types per case/individual?  What is the tissue type, normal, tumor, metastasis?  What fixative methods? etc. "
         },
         {
            "attribute":"organisms",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {
            "attribute":"collaborators",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sample_id",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Sample identifier, Same as BioSample in dbGap"
         },
         {  
            "attribute":"sample_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the sample"
         },
         {  
            "attribute":"sequencing_application_type",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Same as Assay Type in dbGap e.g RNA-Seq, DNA-Seq"
         },
         {  
            "attribute":"sample_type",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Same as Analyte Type in dbGap e.g. RNA, DNA"
         },
         {  
            "attribute":"source_organism",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Source organism e.g. Homon Sapiens (Human)"
         },
         {  
            "attribute":"origin_repository_id",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"consent",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"experiment",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"insert_size",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"instrument",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"library_selection",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Library selection e.g. PolyA"
         },
         {  
            "attribute":"library_source",
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
            "attribute":"average_spot_length",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"load_date",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"mbases",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"m_bytes",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"release_date",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"run",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"body_site",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"histological_type",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"is_tumor",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"submitted_subject_id",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"biospecimen_repository_sample_id",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sex",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"subject_is_affected",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true
         }
      ]
	}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"file_type",
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
         }
      ]
    }' 
WHERE "BASE_PATH"='/CCBR_SRA_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "Project",
               "PI_Lab",
               "Flowcell",
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"project_id_CSAS_NAS",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Project identifier"
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
            "ruleEnabled":true,
            "description":"Flowcell identification string"
         },
         {  
            "attribute":"run_name",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Name of the run"
         },
         {  
            "attribute":"run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Date of the run"
         },
         {  
            "attribute":"sequencing_platform",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Sequencing platform e.g. Illumina"
         },
         {  
            "attribute":"sequencing_application_type",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Same as Assay Type in dbGap e.g RNA-Seq, DNA-Seq"
         },
         {  
            "attribute":"read_length",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Read length"
         },
         {  
            "attribute":"pooling",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Pooling"
         },
         {  
            "attribute":"sample_id",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Sample identifier, Same as BioSample in dbGap"
         },
         {  
            "attribute":"sample_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the sample"
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
            "ruleEnabled":true,
            "description":"Source organism e.g. Homon Sapiens (Human)"
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
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"object_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the stored object"
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
         }
      ]
    }' 
WHERE "BASE_PATH"='/TEST_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
    {          
        "attribute":"collection_type",
        "mandatory":true,
        "validValues":[  
            "Project",
            "PI_Lab",
            "Sample"   
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"pi_name",
        "mandatory":true,
        "collectionTypes":[  
               "PI_Lab"
        ],
        "ruleEnabled":true,
        "description":"First and Last name of the PI"
    },
    {
        "attribute":"affiliation",
        "mandatory":true,
        "collectionTypes":[  
               "PI_Lab"
        ],
        "ruleEnabled":true,
        "description":"Organization to which the PI is affiliated"
    },
    {  
        "attribute":"project_id",
        "mandatory":false,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"project_name",
        "mandatory":true,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true,
        "description":"Name of the project"
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
        "attribute":"start_date",
        "mandatory":true,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true,
        "description":"Start date of the project"
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
        "attribute":"description",
        "mandatory":true,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"Description of the project"
    },
    {  
        "attribute":"method",
        "mandatory":true,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
    },
    {  
        "attribute":"collaborator",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"collaborator_affiliation",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"Organization to which the collaborator is affiliated"
    },
    {
        "attribute":"publications",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {
        "attribute":"comments",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"sample_id",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Sample identifier, Same as BioSample in dbGap"
    },
    {  
        "attribute":"sample_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Name of the sample"
    },
    {  
        "attribute":"machine_type",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
         ],
         "ruleEnabled":true,
         "description":"Machine type"
    },
    {  
        "attribute":"application",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
         "description":"Application"
    },
    {  
        "attribute":"read_length",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Read length"
    },
    {  
        "attribute":"sample_type",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Same as Analyte Type in dbGap e.g. RNA, DNA"
    },
    {  
        "attribute":"source_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Source name"
    },
    {  
        "attribute":"source_organism",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Source organism e.g. Homon Sapiens (Human)"
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
        "attribute":"control",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"sample_replica",
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
        "attribute":"sample_id",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Sample identifier, Same as BioSample in dbGap"
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
        "attribute":"library_concentration",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"library_size",
        "mandatory":false,
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
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
    {  
        "attribute":"object_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Name of the stored object"
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
        "ruleEnabled":true,
        "description":"PHI information is present or not"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"PII information is present or not"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"Status of data encryption"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"Status of data compression"
    } 
   ]
}' 
WHERE "BASE_PATH"='/CCR_SCAF_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
  "metadataValidationRules": [
    {
      "attribute": "collection_type",
      "mandatory": true,
      "validValues": [
        "PI_Lab",
        "Project",
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "pi_name",
      "mandatory": true,
      "collectionTypes": [
        "PI_Lab"
      ],
      "ruleEnabled": true,
      "description":"First and Last name of the PI"
    },
    {
      "attribute": "project_name",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Name of the project"
    },
    {
      "attribute": "project_title",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Title of the project"
    },
    {
      "attribute": "start_date",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Start date of the project"
    },
    {
      "attribute": "affiliation",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Organization to which the project is affiliated"
    },
    {
      "attribute": "description",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Description of the project"
    },
    {
      "attribute": "method",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
    },
    {
      "attribute": "collaboration",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "publications",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "sample_name",
      "mandatory": true,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true,
      "description":"Name of the sample"
    },
    {
      "attribute": "sequencing_technology",
      "mandatory": false,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "sequencing_lane",
      "mandatory": false,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "experimental_strategy",
      "mandatory": false,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "experimental_sub_strategy",
      "mandatory": false,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true
    }
  ]
}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{ "metadataValidationRules" : [ 

     { "attribute" : "object_name",
       "mandatory" : true,
       "collectionTypes" : [ "Project", "Sample" ],
       "ruleEnabled" : true,
       "description":"Name of the stored object" },
		
     { "attribute" : "file_type",
       "mandatory" : false,
       "collectionTypes" : [ "Project", "Sample" ],
       "ruleEnabled" : true }
] }' 
WHERE "BASE_PATH"='/CCR_LCBG_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "Project",
               "PI_Lab",
               "Flowcell",
               "Sample"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"project_id_CSAS_NAS",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Project identifier"
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
            "ruleEnabled":true,
            "description":"Flowcell identification string"
         },
         {  
            "attribute":"run_name",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Name of the run"
         },
         {  
            "attribute":"run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Date of the run"
         },
         {  
            "attribute":"sequencing_platform",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Sequencing platform e.g. Illumina"
         },
         {  
            "attribute":"sequencing_application_type",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Same as Assay Type in dbGap e.g RNA-Seq, DNA-Seq"
         },
         {  
            "attribute":"read_length",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Read length"
         },
         {  
            "attribute":"pooling",
            "mandatory":true,
            "collectionTypes":[  
               "Flowcell"
            ],
            "ruleEnabled":true,
            "description":"Pooling"
         },
         {  
            "attribute":"sample_id",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Sample identifier, Same as BioSample in dbGap"
         },
         {  
            "attribute":"sample_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the sample"
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
            "ruleEnabled":true,
            "description":"Source organism e.g. Homon Sapiens (Human)"
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
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"object_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the stored object"
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
         }
      ]
    }' 
WHERE "BASE_PATH"='/FNL_SF_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
 "metadataValidationRules": [  
         {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "PI_Lab",
               "Project",
               "Tissue",
               "Experiment",
               "Run", 
               "Analysis",
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
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
         },
         {  
            "attribute":"affiliation",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"Organization to which the PI is affiliated"
         },
         {  
            "attribute":"project_title",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Title of the project"
         },
         {  
            "attribute":"description",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Description of the project"
         },
         {  
            "attribute":"start_date",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Start date of the project"
         },
         {  
            "attribute":"method",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
         },
         {  
            "attribute":"collaboration_partner",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"project_publications",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"notes",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"tissue_name",
            "mandatory":true,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true,
            "description":"Tissue name"
         },
         {  
            "attribute":"organism",
            "mandatory":true,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true,
            "description":"Organism e.g. Homo Sapien (Human)"
         },
         {  
            "attribute":"tissue_type",
            "mandatory":true,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true,
            "description":"Tissue type"
         },
         {  
            "attribute":"tissue_from",
            "mandatory":false,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"sectioned_by",
            "mandatory":false,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"coverslip_type",
            "mandatory":false,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"h&e_stained_section_with_annotation_available",
            "mandatory":false,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"notes",
            "mandatory":false,
            "collectionTypes":[  
               "Tissue"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"experiment_name",
            "mandatory":true,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true,
            "description":"Name of the experiment"
         },
         {  
            "attribute":"experiment_date",
            "mandatory":true,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true,
            "description":"Date of the experiment"
         },
         {  
            "attribute":"animal_id",
            "mandatory":false,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"tumor_id",
            "mandatory":false,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"slide_id",
            "mandatory":true,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true,
            "description":"Slide identifier"
         },
         {  
            "attribute":"tissue_thickness",
            "mandatory":false,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"number_of_antibodies",
            "mandatory":true,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true,
            "description":"Number of antibodies"
         },
         {  
            "attribute":"antibody_name",
            "mandatory":true,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true,
            "description":"Name of antibody"
         },
         {  
            "attribute":"staiining_protocol",
            "mandatory":false,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"notes",
            "mandatory":false,
            "collectionTypes":[  
               "Experiment"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"run_name",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Name of the run"
         },
         {  
            "attribute":"run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Date of the run"
         },
         {  
            "attribute":"run_by",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"codex_instrument",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"microscope",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"objective",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"cycle_number",
            "mandatory":true,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Cycle number"
         },
         {  
            "attribute":"number_of_regions",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
          {  
            "attribute":"number_of_tiles",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"number_of_z-stacks",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"pitch_number",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"notes",
            "mandatory":false,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"analysis_date",
            "mandatory":true,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true,
            "description":"Date of analysis"
         },
         {  
            "attribute":"analysis_done_on_run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true,
            "description":"Date of the run"
         },
         {  
            "attribute":"analysis_type",
            "mandatory":true,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true,
            "description":"Type of analysis"
         },
         {  
            "attribute":"best_focus_reference_cycle",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"cell_size_cutoff_factor",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"deconvolution_used",
            "mandatory":true,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true,
            "description":"Deconvolution used"
         },
         {  
            "attribute":"drift_compensation_reference_cycle",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"generated_by",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"membrane_stain_channel",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"membrane_stain_cycle",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         }, 
         {  
            "attribute":"name_or_version_of_clustering_software_used",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
          {  
            "attribute":"name_or_version_of_clustering_software_used",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"name_or_version_of_segmentation_software_used",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"notes",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"nuclear_stain_channel",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"nuclear_stain_cycle",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"relative_cutoff",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"segmentation_max_cutoff",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"segmentation_min_cutoff",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         },
         {  
            "attribute":"segmentation_radius",
            "mandatory":false,
            "collectionTypes":[  
               "Analysis"
            ],
            "ruleEnabled":true
         }
      ]
	}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
        {  
            "attribute":"object_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the stored object"
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
            "attribute":"md5_checksum",
            "mandatory":false,
            "collectionTypes":[  
               "Sample"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
        }
    ]
}' 
WHERE "BASE_PATH"='/CPTR_CODEX_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
  "metadataValidationRules": [
    {
      "attribute": "collection_type",
      "mandatory": true,
      "validValues": [
        "PI_Lab",
        "Project",
        "Data",
        "Analysis",
        "Results",
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
      "ruleEnabled": true,
      "description":"First and Last name of the PI"
    },
    {
      "attribute": "pi_lab",
      "mandatory": true,
      "collectionTypes": [
        "PI_Lab"
      ],
      "ruleEnabled": true,
      "description":"Lab to which the PI belongs to"
    },
    {
      "attribute": "project_name",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Name of the project"
    },
    {
      "attribute": "project_title",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Title of the project"
    },
    {
      "attribute": "start_date",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Start date of the project"
    },
    {
      "attribute": "description",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Description of the project"
    },
    {
      "attribute": "collaborator",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "collaborator_affiliation",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Organization to which the collaborator is affiliated"
    },
    {
      "attribute": "web_links",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "publications",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "method",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
    },
    {
      "attribute": "comments",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    }
  ]
}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
     {  
         "attribute":"object_name",
         "mandatory":true,
         "collectionTypes":[  
             "Data",
             "Analysis",
             "Results"
         ],
         "ruleEnabled":true,
         "description":"Name of the stored object"
     },
     {  
         "attribute":"phi_content",
         "mandatory":true,
         "collectionTypes":[  
             "Data"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "PHI Present",
             "PHI Not Present",
             "Not Specified"
            ],
            "ruleEnabled":true,
            "description":"PHI information is present or not"
     },
     {  
         "attribute":"pii_content",
         "mandatory":true,
         "collectionTypes":[  
             "Data"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "PII Present",
             "PII Not Present",
             "Not Specified"
         ],
         "ruleEnabled":true,
         "description":"PII information is present or not"
     },
     {  
         "attribute":"data_compression_status",
         "mandatory":true,
         "collectionTypes":[  
             "Data"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "Compressed",
             "Not Compressed",
             "Not Specified"
         ],
         "ruleEnabled":true,
         "description":"Status of data compression"
     },
     {  
         "attribute":"tools_used",
         "mandatory":true,
         "collectionTypes":[  
             "Analysis"
         ],
         "ruleEnabled":true,
         "description":"Tools used"
     },
     {  
         "attribute":"methods_used",
         "mandatory":false,
         "collectionTypes":[  
             "Analysis"
         ],
         "ruleEnabled":true
     },
     {  
         "attribute":"text",
         "mandatory":true,
         "collectionTypes":[  
             "Results"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "Report Present",
             "Report Not Present"
         ],
         "ruleEnabled":true,
         "description":"Report is present or not"
     },
     {  
         "attribute":"graphics",
         "mandatory":true,
         "collectionTypes":[  
             "Results"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "Graphics Present",
             "Graphics Not Present"
         ],
         "ruleEnabled":true,
         "description":"Graphics is present or not"
     },
     {  
         "attribute":"tables",
         "mandatory":true,
         "collectionTypes":[  
             "Results"
         ],
         "defaultValue":"Unspecified",
         "validValues":[  
             "Unspecified",
             "Tabular Data",
             "Tabular Data Not Present"
         ],
         "ruleEnabled":true,
         "description":"Tabular data is present or not"
     }
    ]
}' 
WHERE "BASE_PATH"='/FNL_ABCS_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
        {  
            "attribute":"collection_type",
            "mandatory":true,
            "validValues":[  
               "PI_Lab",
               "Project",
               "Variant",
               "Negative_Stain",
               "CryoEM", 
               "Raw_Data",
               "Run"
            ],
            "ruleEnabled":true
        },
        {  
            "attribute":"pi_name",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"First and Last name of the PI"
        },
        {  
            "attribute":"affiliation",
            "mandatory":true,
            "collectionTypes":[  
               "PI_Lab"
            ],
            "ruleEnabled":true,
            "description":"Organization to which the PI is affiliated"
        },
        {  
            "attribute":"project_number",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
             "description":"Project number"
        },
        {  
            "attribute":"project_name",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Name of the project"
        },
        {  
            "attribute":"description",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Description of the project"
        },
        {  
            "attribute":"method",
            "mandatory":true,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
        },
        {  
            "attribute":"start_date",
            "mandatory":true,
            "defaultValue":"System-Date",
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true,
            "description":"Start date of the project"
        },
        {  
            "attribute":"publications",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
        },
        {  
            "attribute":"comments",
            "mandatory":false,
            "collectionTypes":[  
               "Project"
            ],
            "ruleEnabled":true
        },
        {  
            "attribute":"variant_name",
            "mandatory":false,
            "collectionTypes":[  
               "Variant"
            ],
            "ruleEnabled":true
        },
        {  
            "attribute":"instrument",
            "mandatory":true,
            "collectionTypes":[  
               "Negative_Stain"
            ],
            "ruleEnabled":true,
            "description":"Instrument used e.g. Illumina HiSeq"
        },
        {  
            "attribute":"pipeline_number",
            "mandatory":true,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Pipeline identifier"
        },
        {  
            "attribute":"software",
            "mandatory":true,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Software used"
        },
        {  
            "attribute":"run_date",
            "mandatory":true,
            "collectionTypes":[  
               "Run"
            ],
            "ruleEnabled":true,
            "description":"Date of the run"
        }
    ]
}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
        {  
            "attribute":"object_name",
            "mandatory":true,
            "collectionTypes":[  
               "Sample"
            ],
            "ruleEnabled":true,
            "description":"Name of the stored object"
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
            "ruleEnabled":true,
            "description":"PHI information is present or not"
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
                "PHI Present",
                "PHI Not Present",
                "Not Specified"
            ],
            "ruleEnabled":true,
            "description":"PII information is present or not"
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
                "PHI Present",
                "PHI Not Present",
                "Not Specified"
            ],
            "ruleEnabled":true,
            "description":"Status of data encryption"
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
            "ruleEnabled":true,
            "description":"Status of data compression"
        }
    ]
}' 
WHERE "BASE_PATH"='/CCR_CMM_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
  "metadataValidationRules": [
    {
      "attribute": "collection_type",
      "mandatory": true,
      "validValues": [
        "PI_Section",
        "Researcher",
        "Project",
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "pi_name",
      "mandatory": true,
      "collectionTypes": [
        "PI_Section"
      ],
      "ruleEnabled": true,
      "description":"First and Last name of the PI"
    },
    {
      "attribute": "pi_section",
      "mandatory": true,
      "collectionTypes": [
        "PI_Section"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "affiliation",
      "mandatory": true,
      "collectionTypes": [
        "PI_Section"
      ],
      "ruleEnabled": true,
      "description":"Organization to which the PI is affiliated"
    },
    {
      "attribute": "researcher_name",
      "mandatory": true,
      "collectionTypes": [
        "Researcher"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "project_accession",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "project_title",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Title of the project"
    },
    {
      "attribute": "start_date",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Start date of the project"
    },
    {
      "attribute": "description",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Description of the project"
    },
    {
      "attribute": "collaborator",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "collaborator_affiliation",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Organization to which the collaborator is affiliated"
    },
    {
      "attribute": "publications",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "comments",
      "mandatory": false,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "platform",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "library_strategy",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "library_source",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "library_selection",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true,
      "description":"Library selection e.g. PolyA"
    },
    {
      "attribute": "library_layout",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "design_description",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "instrument_model",
      "mandatory": true,
      "collectionTypes": [
        "Project"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "sample_accession",
      "mandatory": true,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true
    },
    {
      "attribute": "flowcell_id",
      "mandatory": true,
      "collectionTypes": [
        "Sample"
      ],
      "ruleEnabled": true,
      "description":"Flowcell identification string"
    }
  ]
}',
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
  "metadataValidationRules": [
    {
      "attribute": "object_name",
      "mandatory": true,
      "collectionTypes": [
        "Project",
        "Sample"
      ],
      "ruleEnabled": true,
      "description":"Name of the stored object"
    },
    {
      "attribute": "file_type",
      "mandatory": true,
      "collectionTypes": [
        "Project",
        "Sample"
      ],
      "ruleEnabled": true
    }
  ]
}' 
WHERE "BASE_PATH"='/LHC_Genomics_Archive';

UPDATE public."HPC_DATA_MANAGEMENT_CONFIGURATION" 
SET "COLLECTION_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
    {          
        "attribute":"collection_type",
        "mandatory":true,
        "validValues":[  
            "Project",
            "PI_Lab",
            "Sample"   
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"pi_name",
        "mandatory":true,
        "collectionTypes":[  
               "PI_Lab"
        ],
        "ruleEnabled":true,
        "description":"First and Last name of the PI"
    },
    {
        "attribute":"affiliation",
        "mandatory":true,
        "collectionTypes":[  
               "PI_Lab"
        ],
        "ruleEnabled":true,
        "description":"Organization to which the PI is affiliated"
    },
    {  
        "attribute":"project_id",
        "mandatory":false,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true,
        "description":"Project identifier"
    },
    {  
        "attribute":"project_name",
        "mandatory":true,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true,
        "description":"Name of the project"
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
        "attribute":"start_date",
        "mandatory":true,
        "collectionTypes":[  
               "Project"
        ],
        "ruleEnabled":true,
        "description":"Start date of the project"
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
        "attribute":"description",
        "mandatory":true,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"Description of the project"
    },
    {  
        "attribute":"method",
        "mandatory":true,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"method/assay type/data type such as RNA-Seq, Chip-Seq, Exome-Seq, ATAC-Seq, Whole Genome etc"
    },
    {  
        "attribute":"collaborator",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"collaborator_affiliation",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true,
        "description":"Organization to which the collaborator is affiliated"
    },
    {
        "attribute":"publications",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {
        "attribute":"comments",
        "mandatory":false,
        "collectionTypes":[  
            "Project"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"sample_id",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Sample identifier, Same as BioSample in dbGap"
    },
    {  
        "attribute":"sample_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Name of the sample"
    },
    {  
        "attribute":"machine_type",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
         ],
         "ruleEnabled":true,
         "description":"Machine type"
    },
    {  
        "attribute":"application",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Application"
    },
    {  
        "attribute":"read_length",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Read length"
    },
    {  
        "attribute":"sample_type",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Same as Analyte Type in dbGap e.g. RNA, DNA"
    },
    {  
        "attribute":"source_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Source name"
    },
    {  
        "attribute":"source_organism",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Source organism e.g. Homon Sapiens (Human)"
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
        "attribute":"control",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"sample_replica",
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
        "attribute":"sample_id",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Sample identifier, Same as BioSample in dbGap"
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
        "attribute":"library_concentration",
        "mandatory":false,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true
    },
    {  
        "attribute":"library_size",
        "mandatory":false,
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
"DATA_OBJECT_METADATA_VALIDATION_RULES"=
'{
    "metadataValidationRules": [  
    {  
        "attribute":"object_name",
        "mandatory":true,
        "collectionTypes":[  
            "Sample"
        ],
        "ruleEnabled":true,
        "description":"Name of the stored object"
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
        "ruleEnabled":true,
        "description":"PHI information is present or not"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"PII information is present or not"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"Status of data encryption"
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
            "PHI Present",
            "PHI Not Present",
            "Not Specified"
        ],
        "ruleEnabled":true,
        "description":"Status of data compression"
    } 
   ]
}' 
WHERE "BASE_PATH"='/FNL_SF_SCAF_Archive';

