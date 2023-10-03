#Author: sarada.chintala@nih.gov
Feature: Register Asynchronous data transfer from Google Cloud, AWS, Globus and Google Drive
  Description: This feature file contains registering from  related scenarios

  @smoke
  Scenario Outline: Register Asynchronous data file transfer from Google Cloud
    Given I have a data source "<source>"
    And I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
    And I add metadataEntries as
      | attribute       | value                  |
      | sample_id       | 3f233                  |
      | source_organism | Mouse                  |
      | lab_branch      | file-Lab / Branch Name |
      | pi_doc          | FNLCR                  |
    When I click Register for the file Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source      | bucket              | googleCloudLocation | isFile | response |
      | googleCloud | dme-download-bucket | test.txt            | true   | success  |
      | googleCloud | dme-upload-bucket   | screenshot.png      | true   | success  |

  Scenario Outline: Register Asynchronous data file transfer from AWS
    Given I have registration path as "/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project/935-testing-flowcell/Run_SRR479649"
    And I have a data source "<source>"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
    And I add metadataEntries as
      | attribute       | value                  |
      | sample_id       | 3f233 aws              |
      | source_organism | Mouse                  |
      | lab_branch      | file-Lab / Branch Name |
      | pi_doc          | FNLCR                  |
    When I click Register for the file Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source | bucket          | googleCloudLocation | isFile | response |
      | aws    | dme-test-bucket | a.json              | true   | success  |
      | aws    | dme-test-bucket | b.json              | true   | success  |

  @test1023 @gc @bulk
  Scenario Outline: Register Asynchronous data file transfer from Google Cloud with Parent Metadata
    Given I have a data source "<source>"
    And I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
    And I add metadataEntries as
      | attribute       | value                  |
      | sample_id       | 3f233                  |
      | source_organism | Mouse                  |
      | lab_branch      | file-Lab / Branch Name |
      | pi_doc          | FNLCR                  |
    And I add a path as "/xyz" with pathMetadata as
      | attribute | value |
      | abc       | x     |
      | abc1      | y     |
      | abc2      | z     |
    And I add a path as "/sarada" with pathMetadata as
      | attribute | value |
      | abc       | quick |
      | abc1      | brown |
      | abc2      | fox   |
    And I add default metadataEntries as
      | attribute       | value     |
      | sample_id       | default_1 |
      | source_organism | default_2 |
      | lab_branch      | default_3 |
      | pi_doc          | FNLCR     |
    When I click Register for the file Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source      | bucket              | googleCloudLocation | isFile | response |
      | googleCloud | dme-download-bucket | xyzfile.txt         | true   | success  |
      | googleCloud | dme-upload-bucket   | RSF-2022-03-29.csv  | true   | success  |

  @test1024 @gc @bulk @dev
  Scenario Outline: Register Asynchronous data file transfer from Google Cloud with Parent Metadat, DEV
    Given I need set up a Bulk Directory Registration
    Given I have a data source "googleCloud"
    And I have registration path as "/DOE_TEST_Archive/Test_Program/Test_Study"
    And I add source cloud bucket as "dme-download-bucket"
    And I add source cloud location as "Demo9_14_2023"
    And I add a path as "/DOE_TEST_Archive/Test_Program/Test_Study/Demo9_14_2023" with pathMetadata as
      | attribute              | value     |
      | collection_type        | Asset     |
      | asset_identifier       | a12       |
      | asset_name             | b12       |
      | access_group           | public    |
      | description            | Asset seq |
      | asset_type             | Dataset   |
      | is_reference_dataset   | No        |
      | applicable_model_paths | Y         |
    And I add a path as "/DOE_TEST_Archive/Test_Program/Test_Study/Demo9_14_2023/Screenshot" with pathMetadata as
      | attribute         | value  |
      | folder_identifier | abc123 |
    Given I need set up a Bulk Data Object Registration
    Given I have a data source "googleCloud"
    And I have registration path as "/DOE_TEST_Archive/Test_Program/Test_Study/Test_Dataset"
    And I add source cloud bucket as "dme-download-bucket"
    And I add source cloud location as "xyzfile.txt"
    And I add a path as "/DOE_TEST_Archive/Test_Program/Test_Study/Test_Dataset/xyzfile.txt" with pathMetadata as
      | attribute   | value  |
      | object_name | obj456 |
      | source_path | xyz    |
    Given I need set up a Bulk Data Object Registration
    Given I have a data source "googleCloud"
    And I have registration path as "/DOE_TEST_Archive/Test_Program/Test_Study/Test_Dataset"
    And I add source cloud bucket as "dme-download-bucket"
    And I add source cloud location as "12_12_2022_1124.txt"
    And I add a path as "/DOE_TEST_Archive/Test_Program/Test_Study/Test_Dataset/12_12_2022_1124.txt" with pathMetadata as
      | attribute   | value  |
      | object_name | obj457 |
      | source_path | xyz2   |
    When I click Register for Bulk Upload
    Then I get a response of success for the Upload

  Scenario Outline: Register Asynchronous data file transfer from Globus
    Given I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I have a data source "globus"
    And I add source cloud bucket as "bb869ce8-df2a-11eb-8325-45cc1b8ccd4a"
    And I add source cloud location as "PI_testdirectory2.metadata.json"
    And I add metadataEntries as
      | attribute        | value   |
      | file_description | Testing |
    And I choose file or directory as "true"
    When I click Register for the file Upload
    Then I get a response of success for the Upload

  Scenario Outline: Register Asynchronous directory transfer from Google Cloud
    Given I have a data source "googleCloud"
    And I have registration path as "/GTL_RAS_Archive/TEST/Project/Flowcell"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "data_object_lis1/"
    And I add a path as "/GTL_RAS_Archive/TEST/Project/Flowcell/data_object_lis1" with pathMetadata as
      | attribute       | value        |
      | collection_type | Sample       |
      | sample_id       | 43JSampleId  |
      | sample_name     | sample_mouse |
    When I click Register for the directory Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source      | bucket              | response |
      | googleCloud | dme-download-bucket | success  |

  @testdev101
  Scenario Outline: Register Asynchronous directory transfer from Google Cloud(DEV)
    Given I have a data source "googleCloud"
    And I have registration path as "/GTL_RAS_Archive/test2"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "Project101/"
    And I add a path as "/GTL_RAS_Archive/test2/Project101" with pathMetadata as
      | attribute           | value                                          |
      | collection_type     | Project                                        |
      | origin              | FAO                                            |
      | project_title       | Finding variants of BRC1                       |
      | project_description | Finding different biomarkers for breast cancer |
      | project_poc         | POC                                            |
      | publication_status  | current                                        |
      | collaborators       | NIH,NCI                                        |
      | comments            | Ongoing for 2 years                            |
    And I add a path as "/GTL_RAS_Archive/test2/Project101/Flowcell201" with pathMetadata as
      | attribute                   | value                       |
      | collection_type             | Flowcell                    |
      | flowcell_id                 | ff2351                      |
      | run_name                    | sar124                      |
      | run_date                    | 01/02/2023                  |
      | sequencing_platform         | NGS Illumina                |
      | sequencing_application_type | On demand Genome Sequencing |
      | study_disease               | Alzheimers                  |
    And I add a path as "/GTL_RAS_Archive/test2/Project101/Flowcell201/Sample_301" with pathMetadata as
      | attribute       | value       |
      | collection_type | Sample      |
      | sample_name     | sample38648 |
      | sample_type     | frz106      |
    And I add a path as "/GTL_RAS_Archive/test2/Project101/Flowcell201/Sample_301/code.txt" with pathMetadata as
      | attribute   | value |
      | object_name | meta  |
      | file_type   | text  |
    When I click Register for the directory Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source      | bucket              | response |
      | googleCloud | dme-download-bucket | success  |

  @test123
  Scenario Outline: Register Asynchronous directory transfer from AWS
    Given I have a data source "googleCloud"
    And I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add source cloud bucket as "dme-upload-bucket"
    And I add source cloud location as "dme"
    And I add metadataEntries as
      | attribute       | value                  |
      | sample_id       | 3f233                  |
      | source_organism | Mouse                  |
      | lab_branch      | file-Lab / Branch Name |
      | pi_doc          | FNLCR                  |
    When I click Register for the directory Upload
    Then I get a response of success for the Upload