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

Check group permission 

makeme:
- Register a new project 
- Register a new-group
- Give READ permission to the new-group
- Add dice_user to the new-group 

runme:
- Verify that dice_user has READ permission
- Remove the dice_user from the new-group
- Verify that dice_user does not have READ permission

Search for the collection using the "EQUAL" comparator using an attribute from the new project 
