#Author: your.email@your.domain.com
Feature: Download data object to Globus
  Description: This feature file contains scenarious related to downloading data object to Globus

	Scenario Outline: Download to data object to Globus
	  Given a GlobusFileContainerId
    And a fileId
    And a destinationOverwrite field
    When I click Download
    Then I get a response of success