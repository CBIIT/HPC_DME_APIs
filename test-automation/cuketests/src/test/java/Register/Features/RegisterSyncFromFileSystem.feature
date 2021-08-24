Feature: Register from file system synchronously
  Description: This feature file contains registering from file system related scenarios

  Scenario: Register from file system synchronously with success
    Given I am a valid user with token
    And I add base_path as "/CCR_CSB_Archive"
    And I add collection_type as "PI_Lab"
    And I add a checksum of ""
    And I add data_file_path as "/Users/schintal/Downloads/pom.xml"
    And I add metadataEntries as
	    |attribute| value   |
	    |name     | Set100  |
    And I add defaultCollectionMetadataEntries as
	    |attribute        | value  |
	    |collection_type  | Folder |
    When I click Register
    Then I get a response of success