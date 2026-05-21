#Author: sarada.chintala@nih.gov
Feature: Download for External Archives tests
  Download for External Archives

  @download45 @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "<path>"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source    | bucket                               | path                                               |location           | response |
      | globus    | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file    | results/         | success  |
      | globus    | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file     | sigma/         | success  |
      | globus    | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file    | download/         | success  |
      | globus    | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file     | test1gb/         | success  |
      
@download46 @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "<path>"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source    | bucket           | path                                               |location           | response |
      | aws    | dme-option2-bucket  | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file    | results/t1.txt         | success  |
      | aws    | dme-option2-bucket  | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file     | results/t2.txt         | success  |
      | aws    | dme-option2-bucket  | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file    | results/t3.txt        | success  |
      | aws    | dme-option2-bucket  | /mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file     | results/t4.txt         | success  |
 
  @download48  @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "/mnt/DMETemp/test_auto_tiering/tars/test77.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | download/         | success  |

@download49  @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "/mnt/DMETemp/test_auto_tiering/tars/filename_100mb.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | aws         | dme-option2-bucket                   |  results/t.txt       | success  |  

@download491  @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "/mnt/DMETemp/test_auto_tiering/tars/filename_100mb.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | aws         | dme-option2-bucket2                   |  results/t.txt       | success  |            


@download50  @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "/mnt/DMETemp/test_auto_tiering/tars/temp_1GB_file"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | aws         | dme-option2-bucket                   |  results/t.txt       | success  |      

@download51  @extdownload
  Scenario Outline: Download file from DME to various data sources (API Spec doc, section )
    Given I have a data source "<source>" for download from external archive
    And I have a download dataObject path as "/mnt/DMETemp/test_auto_tiering/tars/test77.txt"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source      | bucket                               | location          | response |
      | box         | MyBox                                |  /             | success  |      
