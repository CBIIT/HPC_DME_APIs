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
  I-Edit two configuration files in the ./utils subdirectory:
    1- ./utils/config
        Add you username after the option "-u <username>[:password]"
        Note: Do not check in the config file in the repository with your NCI password.
        For that, the config file is checked in as a dynamic link.
    2. ./utils/server
        Edit the server name and port number for the server to be tested.

  II- Add a test.txt empty file to this Globus endpoint:
        "endpoint": "nihnci#NIH-NCI-TRANSFER1",
        "path": "/GridFTP/GridFTP_t3/<nciID>/dice-tests/test.txt"

  III- Define the environment variable HPC_DM_TEST and let it point to the directory where this README.txt is located.
        For example:
           $export HPC_DM_TEST=/path/to/README.txt


-Execution:
  Every leaf directory includes two scripts: makeme and runme.
  To run the unit tests:
  - For an indiviual leaf Individual Test Subdirectory (ITS) in a leaf folder, run the makeme followed by the runme scripts from the leaf directory.
    The stdout should match the contents of the "correct-output.txt" file, and the stderr should match the contents of the "expected-errors.txt" file.
    Note that these two files can be empty which is still a correct behavior.
  - Use the DICE "dxtest" command from any diretory in the test hierarchy to run all the leaves ITSs that are under this directory. 
    This will give a summary for the test resutls.

For more information about the ITS, check the DICE documentation.
