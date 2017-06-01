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

Register the dataObject and the collection before trying to download it.
The file being registered takes around a minute, therefore it's state will move from IN_TRANSIT to ARCHIVED. 
The download should fail when the state is IN_TRANSIT.
