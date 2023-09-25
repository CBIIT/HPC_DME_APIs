Feature: Title of your feature
  I want to use this template for my feature file

  @download1
  Scenario Outline: Download file from DME to googleCloud
    Given I have a data source "googleCloud" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/dataobject2.txt"
    When I click Download
    Then I get a response of success for the Download

  @download2
  Scenario Outline: Download file from DME to AWS
    Given I have a data source "googleCloud" for download
    And I have multiple download collection paths as
      | paths                                                                              |
      | /TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/ |
      | /TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/                |
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/"
    And I set appendPathToDownloadDestination as "true"
    When I click Download
    Then I get a response of success for the Download
