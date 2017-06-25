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

Try to download a file while it is in IN_TRASIENT mode.
The request should generate an error.
- Register the a large  dataObject with 10GB. (To make sure it stays in the
  transient mode while the test is running)
- This test will require the Globus shared endpoint to contain a file with the path  "/test10GB"
- Verify the dataObject is in transient. 
- Try to download the dataObject.
- Verify that the download request is rejected.
