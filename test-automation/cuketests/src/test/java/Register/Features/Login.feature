#Author: your.email@your.domain.com

Feature: Login to DME
  Description: This feature file contains registering from file system related scenarios

  Scenario: Logging in to DME is working
    Given Browser is open
    And user is on google search page
    When user enters text on google search box
    And hits Enter
    When user enters text on password box
    And hits Enter
    Then user is navigated to the Search Results page
