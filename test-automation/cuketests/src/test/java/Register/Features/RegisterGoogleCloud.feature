#Author: your.email@your.domain.com

Feature: Register Asynchronous data transfer from Google Drive.
  Description: This feature file contains registering from file system related scenarios

  Scenario:  Register Asynchronous data transfer from Google Drive.
    Given I am a valid gc_user with token
    And I add gc_base_path as "x"
    And I add gc_collection_type as "PI_Lab"
    And I add gc_checksum of ""
    And I add gc_data_file_path as "/Users/schintal/Downloads/pom.xml"
    And I add gc_metadataEntries as
	    |attribute| value   |
	    |name     | Set100  |
    And I add gc_defaultCollectionMetadataEntries as
	    |attribute        | value  |
	    |collection_type  | Folder |
    When I click Register for the Google Cloud Upload
    Then I get a response of success for the Google Cloud Upload