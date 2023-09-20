Feature: User can add or delete Bookmarks
  Description: This feature file contains Bookmark related scenarios

  @bookmark1
  Scenario: I want add a new bookmark
    Given I have a path of "/GTL_RAS_Archive/TEST"
    And userId of "schintal"
    And permission of "WRITE"
    And bookmark name of "bookmark_GTL1"
    When I add the bookmark
    Then I verify the status of "SUCCESS"

  @bookmark2
  Scenario: I want update an existing bookmark
    Given I have a path of "/TEST_NO_HIER_Archive/PI_testdirectory2"
    And userId of "schintal"
    And permission of "WRITE"
    And bookmark name of "bookmark_GTL1"
    When I update the bookmark
    Then I verify the status of "SUCCESS"

  @bookmark3
  Scenario: I want add delete an existing bookmark
    Given I have a path of "/TEST_NO_HIER_Archive/PI_testdirectory2"
    And userId of "schintal"
    And permission of "WRITE"
    And bookmark name of "bookmark_GTL1"
    When I delete the bookmark
    Then I verify the status of "SUCCESS"
