Feature: Title of your feature
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
      | globus      | bb869ce8-df2a-11eb-8325-45cc1b8ccd4a | download/x2.txt        | success  |
    