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

Unit tests root directory for the hpc client tool.

These unit tests are organized according to the DICE cross platform unit testing framework. 

To download and install DICE, please follow the documentation at:
http://www.ece.umd.edu/DSPCAD/projects/dice/dice.htm

-Configuration:
  I-Download the hpc client folder and make sure the directory structure looks:
    ./
    ./utils
    ./utils/hpc-client/
    ./utils/hpc-client/login.txt
    ./utils/hpc-client/hpc.properties  
    ...
     
  II- In the directory ./utils/hpc-client/, edit the 2 files "login.txt" and "hpc.properties" as instructed in the HPC_User_Guie document.

  III- Define the environment variable HPC_DM_CLIENT and let it point to the full path where the hpc-cli-<version>.jar file is located.
        For example:
           $export HPC_DM_CLIENT=/path/to/hpc-cli-<version>.jar

-Execution:
  Every leaf directory includes two scripts: makeme and runme.
  To run the unit tests:
  - For an indiviual leaf Individual Test Subdirectory (ITS) in a leaf folder, run the makeme followed by the runme scripts from the leaf directory.
    The stdout should match the contents of the "correct-output.txt" file, and the stderr should match the contents of the "expected-errors.txt" file.
    Note that these two files can be empty which is still a correct behavior.
  - Use the DICE "dxtest" command from any diretory in the test hierarchy to run all the leaves ITSs that are under this directory. 
    This will give a summary for the test resutls.

For more information about the ITS, check the DICE documentation.
