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

Delete a dataObject that I don't have permission to

makeme:
Register the dataObject synchronously using dice_user_sys_admin. 

runme:
Let dice_user_group_admin delete the dataObject.
Make sure the request generated the appropriate error. 
