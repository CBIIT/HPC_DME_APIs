#Author: your.email@your.domain.com

Feature: Register Asynchronous data transfer from Google Cloud and Google Drive
  Description: This feature file contains registering from  related scenarios

  Scenario Outline:  Register Asynchronous data file transfer from Google Cloud
  	Given I have a data source "<source>"
    Given I add registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add a google cloud bucket as "<bucket>"
    And I add a google cloud location as "<googleCloudLocation>"
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
    Given I add registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
  	Given I have a data source "<source>"
    Given I add registration path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add a google cloud bucket as "<bucket>"
    And I add a google cloud location as "<googleCloudLocation>"
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
      |	aws	| dme-download-bucket | xyz.out             | true  | success |
      | aws	|	dme-upload-bucket   | spreadsheet.csv     | true  | success |
    