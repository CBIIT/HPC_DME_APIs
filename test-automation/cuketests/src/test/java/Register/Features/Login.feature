#Author: your.email@your.domain.com

Feature: Login to DME
  Description: This feature file contains registering from file system related scenarios

  Background: Logging in to DME is working
    Given Browser is open
    When user enter in username box as "schintal"
    When user enters text on password box
    Then user is on Bulk page

  Scenario: AWS
    Given user chooses AWS
    And user enters <bucketName> as "dme-upload-bucket"
    And user enters <s3bucket> as "google-com.pem"
    And user enters <s3File> as "on"
    And user enters AWS Access Key
    And user enters AWS Secret Key
    And user enters <region> as "us-east-1"
    Then user is clicks Register AWS