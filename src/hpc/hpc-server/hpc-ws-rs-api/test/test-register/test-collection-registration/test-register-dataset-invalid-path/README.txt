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

Register a dataset using an invalid path 
The input.json file follows the policies file. However the provided path is invalide. The HTTP retruned code should equal 400.
The error should be (message might be different):
400
INVALID_REQUEST_INPUT
Failed to create directory (possibly insufficient permission on path): /tempZone/home/xyz
