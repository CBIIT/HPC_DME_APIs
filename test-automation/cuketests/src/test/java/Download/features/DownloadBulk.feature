Feature: Title of your feature
  I want to use this template for my feature file

  @download1
  Scenario: Download file from DME to googleCloud
    Given I have a data source "googleCloud" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/dataobject2.txt"
    When I click Download
    Then I get a response of success for the Download

  @download11
  Scenario: Download file from DME to googleCloud
    Given I have a data source "googleCloud" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/S_VanDyke_coverletter.pdf"
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/x.pdf"
    When I click Download
    Then I get a response of success for the Download


  @download2
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section 5.48)
    Given I have a data source "<source>" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "<response>" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | aws         | dme-test-bucket                      | a.json            | success  |
      | googleCloud | dme-download-bucket                  | data_object_lis1/ | success  |
      | googleDrive | MyDrive                              | b.json            | success  |
      | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | download/         | success  |

  @download3
  Scenario Outline: Download Data Object List from DME to AWS (API Spec doc, section 5.53)
    Given I have a data source "googleCloud" for download
    And I have multiple download dataObject paths as
      | /TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/S_VanDyke_coverletter.pdf      |
      | /TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt |
      | /TEST_NO_HIER_Archive/Sunita-Test-Collection/version.txt |
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/"
    And I set appendPathToDownloadDestination as "true"
    When I click Download
    Then I get a response of success for the Download

  @download4
  Scenario Outline: Download Collection List from DME to AWS (API Spec doc, section 5.54)
    Given I have a data source "googleCloud" for download
    And I have multiple download collection paths as
      | /TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/ |
      | /TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/                |
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/"
    And I set appendPathToDownloadDestination as "true"
    When I click Download
    Then I get a response of success for the Download
