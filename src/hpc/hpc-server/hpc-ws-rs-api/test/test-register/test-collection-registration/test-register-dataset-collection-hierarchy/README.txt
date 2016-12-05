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

Register a Dataset with valid inputs without following the collection. 
At the time this test is checked, the input.json file follows the policies file.  

However, the dataset should follow the hierarchy /project/datatset/dataObject which is not the case

The returned HTTP code should equal 400.
