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

Unit tests root directory for performance tests of very large files. 
These unit tests are organized according to the DICE cross platform unit testing framework. 
Check more information in ../README.txt

Follow the 

-Configuration:
  I-Edit the configuration files in the ./utils subdirectory:
    1- ./utils/globus-data-config
        Add the name of the endpoint and the path to the large data files following this json format.
         { "endpoint":"<Globus-endpoint-name>",  "path":"</path/to/root/folder/>"
        
        Note: Do not check in the globus-data-config file in the repository with your endpoint and path. 
        For that, the globus-data-config file is checked in as a dynamic link.
    2- Add the following large files at the "path" mentioned in step 1.
        
        a. /path/to/root/folder/test10GB

       Note that the file name contains its size too using the format:
       test<size>. To generate such files, you can use the dd command:
           $dd if=/dev/urandom of=<filename> bs=1M count=<size in MB>
       For exmaple, to generate a 10GB file use:
           $dd if=/dev/urandom of=test10GB bs=1M count=10240

