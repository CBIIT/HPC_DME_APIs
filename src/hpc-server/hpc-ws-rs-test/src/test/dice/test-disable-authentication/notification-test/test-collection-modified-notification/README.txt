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

Test that the notification is delivered after updating a collection 

makeme:
Subscribe to all notification including updates on basepath as dice_user_sys_admin.
Register a new dataObject as dice_user_group_admin

runme:
get the generated notifications as dice_user_sys_admin and make sure the new dataObject is returned. 
