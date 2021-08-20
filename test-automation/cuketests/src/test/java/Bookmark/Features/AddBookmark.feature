Feature: User can add or delete Bookmarks 
  Description: This feature file contains Bookmark related scenarios

  Scenario Outline: I want add a new bookmark
    Given I have a path of "/FNL_SF_Archive"
    And userId of "schintal"
    And permission of "WRITE"
    And bookmarkName of "test_bookmark101"
    When I add the bookmark
    Then I verify the status of "SUCCESS"