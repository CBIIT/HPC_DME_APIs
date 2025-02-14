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
#
@group
Feature: Group Management Feature

  @createGroup
  Scenario Outline: Create a Group
    Given I want to create a group named "<group>" in a "<role>" role
    And I add users to the group
      | users 		|
      | schintal 	|
      | sehgalu2	|
      | menons2		|
      | frostr		|
    And I click create group
		Then I verify the status of "<response>" in group creation
		Examples:
			|group						|	role					| response |
			|test_group_sa		| System Admin	| success  |
			|test_group_ga		| Group Admin		|	success  |
			|test_group_user	| User					|	failure unauthorized access|

  @group2
  Scenario Outline: Update a Group
    Given I want to update a group named "<group>" in a "<role>" role
    And I delete users from the group
      | users 		|
      | sehgalu2	|
      | menon2		|
    And I add users to the group
      | users 		|
      | frostr	|   
    And I click update group
		Then I verify the status of "<response>" of updating a group
		Examples:
			|group						|	role					| response |
			|test_group_sa		| System Admin	| success  |
			|test_group_ga		| Group Admin		|	success  |
			|test_group_user	| User					|	failure unauthorized access|

  @group3
  Scenario: Search a Group
		Given I login as a System Admin
    And I want to search a group named "%test%"
    Then I verify the status of success in searching the group

  @group4
  Scenario: Get a Group
		Given I login as a System Admin
    And I want to get a group named "test_group1"
    Then I verify the status of success in getting the group and its users

  @group5
  Scenario: Delete a Group
    Given I want to delete a group named "<group>" in a "<role>" role
    Then I verify the status of <response> in group deletion
		Examples:
			|group						|	role					| response |
			|test_group_sa		| System Admin	| success  |
			|test_group_ga		| Group Admin		|	success  |
			|test_group_user	| User					|	failure unauthorized access|

  #@group6
  #Scenario: User Role should not be able to Create/Update/Delete groups
  #  Given I want to <action> a group named "test_group1" in a User role
  #  Then I verify the status of failure of actions in a User role
	#	Examples:
	#		|	action	|
	#		| create	|
