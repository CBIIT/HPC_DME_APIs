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
      | googleCloud | dme-download-bucket | xyzfile.txt             | true   | success  |
      | googleCloud | dme-upload-bucket   | RSF-2022-03-29.csv  | true   | success  |

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

  @test
  Scenario Outline: Register Asynchronous directory transfer from Google Cloud
    Given I have a data source "googleCloud"
    And I have registration path as "/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project/935-testing-flowcell/Run_SRR479649"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "dme/"
    And I add a path as "/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project" with pathMetadata as
      | attribute       | value   |
      | collection_type | Project |
      | project_name    | E052    |
    And I add a path as "/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project/935-testing-flowcell" with pathMetadata as
      | attribute       | value        |
      | collection_type | Flowcell     |
      | flowcell_id     | Sarada ff123 |
    And I add a path as "/FNL_SF_Archive/PI_Bogus_Investigator/935-testing-project/935-testing-flowcell/Run_SRR479649" with pathMetadata as
      | attribute       | value        |
      | collection_type | Sample       |
      | sample_id       | 43JSampleId  |
      | sample_name     | sample_mouse |
    When I click Register for the directory Upload
    Then I get a response of <response> for the Upload

    Examples: 
      | source      | bucket            | response |
      | googleCloud | dme-upload-bucket | success  |

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
