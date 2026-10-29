#Author: sarada.chintala@nih.gov
Feature: Download Collections from External Archive tests
  Download Collection from External Archives

  @download201 @extdownload
  Scenario Outline: Download collection from DME to various data sources
    Given I have a data source "<source>" for download from external archive
    And I have a download collection path as "/mnt/DMETemp/DME_TEAM/dmeDNA"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
    | source      | bucket                                  | location          | response |
    | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a    |  /                | success  |      


@download202 @extdownload
  Scenario Outline: Download collection from DME to various data sources
    Given I have a data source "<source>" for download from external archive
    And I have a download collection path as "<path>"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source    | bucket                    | path                                               |location                   | response |
      | aws    | dme-option2-bucket           | /mnt/DMETemp/DME_TEAM/dmeTesting                   | results/dmeTesting        | success  |
      | aws    | dme-option2-bucket           | /mnt/DMETemp/DME_TEAM/dmeDNA                       | results/dmeDNA        | success  |
  

@download203 @extdownload
  Scenario Outline: Download collection from DME to various data sources
    Given I have a data source "<source>" for download from external archive
    And I have a download collection path as "<path>"
    And I add download cloud bucket as "<bucket>"
    And I add download cloud location as "<location>"
    When I click Download
    Then I get a response of "success" for the Download

    Examples: 
      | source    | bucket                             | path                                               |location                   | response |
      | globus | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a  | /mnt/DMETemp/DME_TEAM/dmeDNA                       | results/dmeDNA      | success  |
      | globus | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a  | /mnt/DMETemp/DME_TEAM/dmeTesting                   | results/dmeTesting      | success  |


