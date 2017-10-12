/**
 * HpcPagination.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Pagination Support.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcPagination 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The page size.
	private int pageSize = 0;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor disabled.
     * 
     * @throws HpcException Constructor disabled.
     *
     */
    private HpcPagination() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
    
    /**
     * Constructor for Spring Dependency Injection.
     *
     * @param pageSize The page size
     */
    private HpcPagination(int pageSize) 
    {
    	this.pageSize = pageSize;
    }

    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

    /**
     * Get the page size
     *
     * @param page The requested page.
     * @return The calculated offset
     * @throws HpcException if the page is invalid.
     * 
     */
    public int getPageSize()
    {
    	return pageSize;
    }

    /**
     * Calculate search offset by requested page.
     *
     * @param page The requested page.
     * @return The calculated offset
     * @throws HpcException if the page is invalid.
     * 
     */
    public int getOffset(int page) throws HpcException
    {
    	if(page < 1) {
    	   throw new HpcException("Invalid page: " + page,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return (page - 1) * pageSize;
    }
}

