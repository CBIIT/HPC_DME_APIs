Feature: Register from file system synchronously
  Description: This feature file contains registering from file system related scenarios

	Background: User is Logged In
		Given I am a valid user with token

  	Scenario: Register from file system synchronously with success
	    Given I add base_path as "/CCR_CSB_Archive"
	    And I add collection_type as "PI_Lab"
	    And I add a checksum of ""
	    And I add data_file_path as ""
	    And I add source_path as "/Users/schintal/Downloads/saradatest.out"
	    And I add destination_path as "/CCR_CSB_Archive/PI_PI/Project_some_brief_description/Run_Raw_dataset_X/saradatest.out"
	    And I add metadataEntries as
		    |attribute| value   |
		    |object_name     | saradatest1 |
	    And I add defaultCollectionMetadataEntries as
		    |attribute        | value  |
		    |collection_type  | Folder |
	    When I click Register
	    Then I get a response of success