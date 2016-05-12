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

Register a dataObject with valid inputs with no datasource nor multipart file upload 
The test should return 400 with:    
"errorType": "INVALID_REQUEST_INPUT",
"message": "No data transfer source or data attachment provided"

