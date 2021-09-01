#Author: your.email@your.domain.com
Feature: Download data object to Globus
  Description: This feature file contains scenarious related to downloading data object to Globus

	Scenario Outline: Download to data object to Globus
	  Given a <globusFileContainerId>
    And a <fileId>
    And a <destinationOverwrite> field
    When I click Download
    Then I get a <status> of success
    | globusFileContainerId |              fileId          | destinationOverwrite | status|
    | 4a3b132a-815f-11e7-8dff-22000b9923ef | test-12-02-18 | true		               success |