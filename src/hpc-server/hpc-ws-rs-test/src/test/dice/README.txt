/******************************
@ddblock_begin copyright
/**
 * Readme.txt
 * @author: George Zaki 
 *
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

@ddblock_end copyright
******************************/

Unit tests root directory for the High Performance Conputing Data Management HPCDM REST APIs.

These unit tests are organized according to the DICE cross platform unit testing framework. 

To download and install DICE, please follow the documentation at:
http://www.ece.umd.edu/DSPCAD/projects/dice/dice.htm

-Configuration:
  I- General configuration:
     Edit the configuration file:  ./utils/test-configuration:
        1- Edit your NCI "username"
        2- Edit the "base-folder" for your test DOC
        3- Edit the "globus-shared-endpoint-uid" as mentioned in step II


  II- Globus configuraton:
      1- Create a globus shared endpoint (e.g., HPC_DM_TEST) and share it with the service account. 
         Review the user guide document to get the name of the service account. 
      2- Add a test.txt empty file to this Globus endpoint:
        "endpoint": "HPC_DM_TEST",
        "path": "/test.txt"
      3- Add a download empty folder to your shared Globus endpoint
        "endpoint": "HPC_DM_TEST",
        "path": "/download/"
      4- Edigt your test-configuration file with the shared endpoint UUID.
      5- Install the globus Command Line Client: 
         https://docs.globus.org/cli/installation/
         Then login with your account:
         globus login
         

  III- Server configuration:
        Edit the  ./utils/server to add the server name and port number for the
        server to be tested.

 
  IV- Scripts configuration: 
        Define the environment variable HPC_DM_TEST and let it point to the
        directory where this README.txt is located.  
        For example, in your ~/.bashrc file add: 
            export HPC_DM_TEST=/path/to/this/README.txt/
       
  V- Token generation 
        Before you can run the test, you need to generate an HPC DM API token.
        Run:
        $HPC_DM_TEST/utils/generate-token.sh

  VI- To run test-hpc-client units tests, please follow the setup steps shown in ./test-hpc-client/README.txt 

  VII- The user who will run the test should be part of a dummy DOC with the hierarchy specified in the appendix.

-Execution:
  Every leaf directory includes two scripts: makeme and runme.
  To run the unit tests:
  - For an indiviual leaf Individual Test Subdirectory (ITS) in a leaf folder, run the makeme followed by the runme scripts from the leaf directory.
    The stdout should match the contents of the "correct-output.txt" file, and the stderr should match the contents of the "expected-errors.txt" file.
    Note that these two files can be empty which is still a correct behavior.
  - Use the DICE "dxtest" command from any diretory in the test hierarchy to run all the leaves ITSs that are under this directory. 
    This will give a summary for the test resutls.

For more information about the ITS, check the DICE documentation.


Appendix:
Dummy DOC policies:
{
      "DOC": [
        "DUMMY"
      ],
      "collectionType": "Project",
      "isDataObjectContainer": true,
      "subCollections": [
        {
          "collectionType": "Dataset",
          "isDataObjectContainer": true
        },
        {
          "collectionType": "Run",
          "isDataObjectContainer": false
        },
        {
          "collectionType": "Folder",
          "isDataObjectContainer": true
        }
       
      ]
}
