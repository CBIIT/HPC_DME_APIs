#Author: sarada.chintala@nih.gov
#Keywords Summary :
#Feature: List of scenarios.
#Scenario: Business rule through list of steps with arguments.
#Given: Some precondition step
#When: Some key actions
#Then: To observe outcomes or validation
#And,But: To enumerate more Given,When,Then steps
#Scenario Outline: List of steps for data-driven as an Examples and <placeholder>
#Examples: Container for s table
#Background: List of steps run before each of the scenarios
#""" (Doc Strings)
#| (Data Tables)
#@ (Tags/Labels):To group Scenarios
#<> (placeholder)
#""
## (Comments)
#Sample Feature Definition Template
@allUserTests
Feature: User Management Feature

  @createuser
  Scenario: Create a User
    Given I want to create a user named "dmetestuser"
    And I want to assign a role of "USER"
    When I create the user
    Then I verify the status of success in user creation

  @creategroupadmin
  Scenario: Create a Group Admin User
    Given I want to create a user named "dmetestgroupadmin"
    And I want to assign a role of "GROUP_ADMIN"
    When I create the user
    Then I verify the status of success in user creation

  @createsystemadmin
  Scenario: Create a System Admin User
    Given I want to create a user named "dmetestsystemadmin"
    And I want to assign a role of "SYSTEM_ADMIN"
    When I create the user
    Then I verify the status of success in user creation
    