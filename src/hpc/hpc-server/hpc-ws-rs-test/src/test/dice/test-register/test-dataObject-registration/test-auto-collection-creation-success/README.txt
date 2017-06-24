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

Register a dataObject with valid inputs. 
The parent collection of the dataObject is not registered and should be automatically registerd by the API.

This is done by setting the attribute "createParentCollections" to "true".

The returned code should be 201

