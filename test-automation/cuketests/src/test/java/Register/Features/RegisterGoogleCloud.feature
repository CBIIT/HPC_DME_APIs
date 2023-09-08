#Author: your.email@your.domain.com

Feature: Register Asynchronous data transfer from Google Cloud and Google Drive
  Description: This feature file contains registering from  related scenarios

	@smoke
  Scenario Outline:  Register Asynchronous data file transfer from Google Cloud
  	Given I have a data source "<source>"
    Given I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
	  And I add google cloud metadataEntries as 
        |attribute	      | value   |
		    |sample_id 				| 3f233 |
		    |source_organism 	| Mouse |
		    |lab_branch   		| file-Lab / Branch Name |
		    |pi_doc						| FNLCR	|
    And I have a refresh token
    When I click Register for the Google Cloud Upload
    Then I get a response of <response> for the Google Cloud Upload
      Examples:
      |source | bucket              | googleCloudLocation | isFile |  response |
      |	googleCloud	| dme-download-bucket | xyz.out             | true  | success |
      | googleCloud	|	dme-upload-bucket   | spreadsheet.csv     | true  | success |


  Scenario Outline:  Register Asynchronous data file transfer from AWS
    Given I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
  	Given I have a data source "<source>"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
	  And I add google cloud metadataEntries as 
        |attribute	      | value   |
		    |sample_id 				| 3f233 |
		    |source_organism 	| Mouse |
		    |lab_branch   		| file-Lab / Branch Name |
		    |pi_doc						| FNLCR	|
    And I have a refresh token
    When I click Register for the AWS Upload
    Then I get a response of <response> for the Google Cloud Upload
      Examples:
      |source | bucket              | googleCloudLocation | isFile |  response |
      |	aws	| dme-test-bucket | a.json             | true  | success |
      | aws	|	dme-test-bucket   | b.json     | true  | success |

 Scenario Outline:  Register Asynchronous data file transfer from Google Cloud with Parent Metadata
    Given I have a data source "<source>"
    Given I have registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "<googleCloudLocation>"
    And I choose file or directory as "<isFile>"
	  And I add google cloud metadataEntries as
        |attribute	      | value   |
		    |sample_id 				| 3f233 |
		    |source_organism 	| Mouse |
		    |lab_branch   		| file-Lab / Branch Name |
		    |pi_doc						| FNLCR	|
    And I have a refresh token
    And I add a path as "/xyz" with pathMetadata as
        |attribute	      | value   |
		    |abc		| x |
		    |abc1 	| y |
		    |abc2  	| z |
    And I add a path as "/sarada" with pathMetadata as
        |attribute	      | value   |
		    |abc		| quick |
		    |abc1 	| brown |
		    |abc2  	| fox |
	  And I add default metadataEntries as
        |attribute	      | value   |
		    |sample_id 				| default_1 |
		    |source_organism 	| default_2 |
		    |lab_branch   		| default_3 |
		    |pi_doc						| FNLCR	|
    When I click Register for the Google Cloud Upload
    Then I get a response of <response> for the Google Cloud Upload
      Examples:
      |source | bucket              | googleCloudLocation | isFile |  response |
      |	googleCloud	| dme-download-bucket | xyz.out             | true  | success |
      | googleCloud	|	dme-upload-bucket   | spreadsheet.csv     | true  | success |

  Scenario Outline:  Register Asynchronous data file transfer from Globus
		Given I have registration path as "/TEST_Archive/PI_testdirectory_Sehgal_3/Project_test/test-file-delete-me-aws.txt"
		Given I have a data source "globus"
		And I add source cloud bucket as "16572124-19cb-11e9-934d-0e3d676669f4"
		And I add source cloud location as "/~/test-file-delete-me-aws.txt"
		And I choose file or directory as "true"
		When I click Register for the Globus Upload
		Then I get a response of success for the Google Cloud Upload

	Scenario Outline:  Register Asynchronous directory transfer from Google Cloud
		Given I have a data source "googleCloud"
    Given I have registration path as "/FNL_SF_Archive/Auto_PI_Lab_CCRSF/Project_staudt_103316_17202_ORF_Xeno_1/Flowcell_00000000-ADF6Y/Sample_ORF3_xeno_A_SC1"
    And I add source cloud bucket as "<bucket>"
    And I add source cloud location as "dme/"
    And I have a refresh token
    When I click Register for the directory Upload
    Then I get a response of <response> for the Google Cloud Upload
      Examples:
      |source | bucket              | response |
      | googleCloud	|	dme-upload-bucket  	| success |

	Scenario Outline:  Register Asynchronous directory transfer from AWS
		Given I have a data source "aws"
		Given I have registration path as "/FNL_SF_Archive/Auto_PI_Lab_CCRSF/Project_staudt_103316_17202_ORF_Xeno_1/Flowcell_00000000-ADF6Y/Sample_ORF3_xeno_A_SC1"
		And I add source cloud bucket as "dme-test-bucket"
		And I add source cloud location as "backup/"
		And I have a refresh token
		When I click Register for the directory Upload
		Then I get a response of success for the Google Cloud Upload
   