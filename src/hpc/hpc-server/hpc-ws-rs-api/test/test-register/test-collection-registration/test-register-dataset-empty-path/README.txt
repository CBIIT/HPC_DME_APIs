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

Register a dataset using an empty path. 
The input.json file follows the policies file. However the provided path is invalide. The HTTP retruned code should equal 400.
The expected error is: 
500
DATA_MANAGEMENT_ERROR
Failed to update collection metadata: No access to item in catalog

