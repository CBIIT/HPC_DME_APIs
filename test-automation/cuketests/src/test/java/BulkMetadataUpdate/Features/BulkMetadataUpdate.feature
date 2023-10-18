Feature: Bulk metadata updates
  Description: This feature file tests performing bulk metadata updates

  @blk4
  Scenario Outline: Bulk Metadata update
    Given I have multiple metadata attributes to update
      | attribute       | value |
      | sample_id       | 3f233 |
      | source_organism | Mouse |
    And I have multiple paths to update metadata as
      | Paths                                                        |
      | /DCTD_NExT_Archive/PI_James_Doroshow/Project_NOX/Period_2022 |
    When I submit the bulk metadata update
    Then I get a response of success for the bulk metadata update
