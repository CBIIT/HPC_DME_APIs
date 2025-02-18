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
  Scenario Outline: Create a Group test using different roles(System Admin, Group Admin and User)
    Given I want to create a group named "<group>" in a "<role>" role
    And I add users to the group
      | users 		|
      | schintal 	|
      | sehgalu2	|
      | menons2		|
      | frostr		|
    When I click create group
		Then I verify the status of "<response>" in group creation
		Examples:
			|group								|	role					| response |
			|test_group_sa				| System Admin	| success  |
			|test_group_ga				| Group Admin		|	success  |
			|test_group_user			| User					|	failure unauthorized access|
			|test_group_delete_me	| System Admin	|	success|

  @updateGroup
  Scenario Outline: Update a Group using different roles(System Admin, Group Admin and User)
    Given I want to update a group named "<group>" in a "<role>" role
    And I delete users from the group
      | users 		|
      | sehgalu2	|
      | menon2		|
    And I add users to the group
      | users 		|
      | frostr	|   
    When I click update group
		Then I verify the status of "<response>" of updating a group
		Examples:
			|group						|	role					| response |
			|test_group_sa		| System Admin	| success  |
			|test_group_ga		| Group Admin		|	success  |
			|test_group_user	| User					|	failure unauthorized access|

  @searchGroup
  Scenario: Search a Group using different roles(System Admin,Group Admin and User)
    Given I want to search a group named "<group>" in a "<role>" role
    When I click search group
    Then I verify the status of "<response>" in searching the group
		Examples:
			|group		|	role					| response |
			|%test%		| System Admin	| success  |
			|test		| Group Admin		|	success  |
			|test		| User					|	success	 |

  @getGroup
  Scenario: Get a Group  using different roles(System Admin,Group Admin and User)
    Given I want to get a group named "<group>" in a "<role>" role
    When I click get group
    Then I verify the status of "<response>" in getting the group and its users
		Examples:
			|group						|	role					| response |
			|test_group_sa		| System Admin	| success  |
			|test_group_ga		| Group Admin		|	success  |
			|test_group_ga		| User					|	failure unauthorized access  |

  @deleteGroup
  Scenario: Delete a Group using different roles(System Admin,Group Admin and User)
    Given I want to delete a group named "<group>" in a "<role>" role
		When I click delete group
    Then I verify the status of "<response>" in group deletion
		Examples:
			|group									|	role					| response |
			|test_group_sa					| System Admin	| success  |
			|test_group_ga					| Group Admin		|	success  |
			|test_group_delete_me		| User					|	failure unauthorized access  |
			!test_group_delete_me		| System Admin	|	success  |

