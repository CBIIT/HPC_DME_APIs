#Author: your.email@your.domain.com

Feature: Register Asynchronous data transfer from Google Cloud and Google Drive
  Description: This feature file contains registering from  related scenarios

  Scenario:  Register Asynchronous data transfer from Google Cloud
    Given I am a valid gc_user with token
    And I add gc_base_path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/RUN_XYZ_12345678"
    And I add a google cloud bucket as "dme_download_bucket"
    And I add a google cloud location as "xyzfile.txt"
    And I have a refresh token
    When I click Register for the Google Cloud Upload
    Then I get a response of success for the Google Cloud Upload