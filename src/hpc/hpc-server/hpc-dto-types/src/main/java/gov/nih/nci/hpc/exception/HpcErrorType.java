/**
 * HpcErrorType.java.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.exception;

/**
 * <p>  
 * Error types to be included in exceptions to describe the source of the
 * error. 
 * </p>
 * 
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public enum HpcErrorType
{
	// A Spring configuration problem.
	SPRING_CONFIGURATION_ERROR,
	
	// INVALID_INPUT. Invalid input.
	INVALID_INPUT,
    
    // Mongo DB is the source of the error.
    MONGO_ERROR,
    
    // Error related to JAXB.
    JAXB_ERROR
}