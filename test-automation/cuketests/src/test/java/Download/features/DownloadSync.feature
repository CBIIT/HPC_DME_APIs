#Author: your.email@your.domain.com
Feature: Download to your file system synchronously
  Description: This feature file contains scenarious related to downloading files synchronously to your file system

	Scenario Outline: Download to file system synchronously with success
	  Given I have a compressedArchiveType as "ZIP"
    And I have includePatterns as
    | *.* |
    
   
    When I click Download
   
    Then I get a response of success