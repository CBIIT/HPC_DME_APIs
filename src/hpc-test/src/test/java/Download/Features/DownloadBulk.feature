#Author: sarada.chintala@nih.gov
Feature: Download tests
  I want to use this template for my feature file

  @download2  @download
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section 5.48),  (UAT)
    Given I have a data source "<source>" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples:
      | source      | bucket                               | location          | response |
      | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | download/x2456.txt        | success  |

	@download1 @download
	Scenario: Download file from DME to googleCloud (UAT)
    Given I have a data source "googleCloud" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/dataobject2.txt"
    When I click Download
    Then I get a response of "success" for the Download

  @download11  @download
  Scenario: Download file from DME to googleCloud (UAT)
    Given I have a data source "googleCloud" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/S_VanDyke_coverletter.pdf"
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/x.pdf"
    When I click Download
    Then I get a response of "success" for the Download

  @download2  @download
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section 5.48),  (UAT)
    Given I have a data source "<source>" for download
    And I have a download dataObject path as "/TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | aws         | dme-test-bucket                      | a.json            | success  |
      | googleCloud | dme-download-bucket                  | data_object_lis1/ | success  |
      | googleDrive | MyDrive                              | b.json            | success  |
      | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | download/         | success  |

  @download3  @download
  Scenario Outline: Download Data Object List from DME to AWS (API Spec doc, section 5.53),  (UAT)
    Given I have a data source "googleCloud" for download
    And I have multiple download dataObject paths as
      | Paths                                                                                             |
      | /TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/S_VanDyke_coverletter.pdf      |
      | /TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/dataobject2.txt |
      | /TEST_NO_HIER_Archive/Sunita-Test-Collection/version.txt                                          |
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/"
    And I set appendPathToDownloadDestination as "true"
    When I click Download
    Then I get a response of "success" for the Download

  @download4  @download
  Scenario Outline: Download Collection List from DME to AWS (API Spec doc, section 5.54)
    Given I have a data source "googleCloud" for download
    And I have multiple download collection paths as
      | Paths                                                                              |
      | /TEST_NO_HIER_Archive/PI_testdirectory2/Project_test/flowcell_test2/sample_sehgal/ |
      | /TEST_NO_HIER_Archive/Sample_Collection_Yuri/Compass_Test_Archive1/                |
    And I add download cloud bucket as "dme-upload-bucket"
    And I add download cloud location as "Sample1/"
    And I set appendPathToDownloadDestination as "true"
    When I click Download
    Then I get a response of "success" for the Download

  @download5  @download
  Scenario: Download file from DME to Aspera
    Given I have a data source "aspera" for download
    And I have a download dataObject path as "/FNL_SF_Archive/eran-pi-lab/eran-project/eran-flowcell/eran-sample-2/eran-data-object-aws-12-13-20"
    And I add download cloud bucket as "test"
    And I add download cloud location as "filename"
    When I click Download
    Then I get a response of "success" for the Download

  @download6  @download
  Scenario: Download list of DataObject from DME to Aspera
    Given I have a data source "aspera" for download
    And I have multiple download dataObject paths as
      | Paths                                                 |
      | /FS_ARCHIVE/fs-demo/data-object-file-sync-1-18-19-2 |
      | /FS_ARCHIVE/fs-demo/data-object-file-sync-1-18-19-1 |
    And I add download cloud bucket as "test"
    And I add download cloud location as "/"
    When I click Download
    Then I get a response of "success" for the Download

  @download7  @download
  Scenario: Download list of Collections from DME to Aspera
    Given I have a data source "aspera" for download
    And I have multiple download collection paths as
      | Paths                                                 |
      | /FNL_SF_Archive/eran-pi-lab-2/eran-project/eran-flowcell/eran-sample/eran-data-object-sync-10-19-20 |
    And I add download cloud bucket as "test"
    And I add download cloud location as "/"
    When I click Download
    Then I get a response of "success" for the Download

   @download8 @aspera
  Scenario: Download file from DME to Aspera, UAT
    Given I have a data source "aspera" for download
    And I have a download dataObject path as "/FNL_SF_Archive/eran_pi_cli_test/Project_test/flowcell_test2/sample_sehgal/dataobject.txt"
    And I add download cloud bucket as "test"
    And I add download cloud location as "filename"
    When I click Download
    Then I get a response of "success" for the Download

  @download9 @aspera  @download
  Scenario: Download list of DataObjects from DME to Aspera
    Given I have a data source "aspera" for download
    And I have multiple download dataObject paths as
      | Paths                                                 |
      | /FNL_SF_Archive/eran_pi_cli_test/Project_test/flowcell_test2/sample_sehgal/dataobject.txt |
      | /FNL_SF_Archive/eran_pi_cli_test/Project_test/flowcell_test2/sample_sehgalu2/dataobject.txt |
    And I add download cloud bucket as "test"
    And I add download cloud location as "/"
    When I click Download
    Then I get a response of "success" for the Download

  @download10 @aspera  @download
  Scenario: Download list of Collections from DME to Aspera
    Given I have a data source "aspera" for download
    And I have multiple download collection paths as
      | Paths                                                 |
      | /FNL_SF_Archive/eran-pi-lab/eran-project/eran-flowcell/eran-sample-migration/ |
      | /FNL_SF_Archive/eran-pi-lab/eran-project/eran-flowcell/eran-sample-2/ |
    And I add download cloud bucket as "test"
    And I add download cloud location as "/"
    When I click Download
    Then I get a response of "success" for the Download

  @download11 @aspera  @download
  Scenario: Download file from DME to Aspera, UAT
    Given I have a data source "aspera" for download
    And I have a download collection path as "/CCR_GB_MGS_Archive/PI_Paul_Meltzer/Data_BAM/Flowcell_AC9W54ANXX"
    And I add download cloud bucket as "test"
    And I add download cloud location as "filename"
    When I click Download
    Then I get a response of "success" for the Download
    