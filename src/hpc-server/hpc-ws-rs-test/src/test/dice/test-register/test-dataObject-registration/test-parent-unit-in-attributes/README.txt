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

Get a preregistered dataObject.
During a get request, the object medatadata is generated from iRods and the
parent information is generated from the materialized view. Therefore the unit
attributes  should be part of the materialized view. 

The project registered using makeme has a unit in one of its metadata. It
should be returned when a get request is executed on the ojbect.
