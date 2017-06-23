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

Make sure a USER can not delete a dataObject even if it owns it. 

makeme:
Register the dataObject synchronously dice_user_sys_admin
give own permission to dice_user

runme:
Let dice_user delete the dataObject.
Make sure the correct error message is generated.
