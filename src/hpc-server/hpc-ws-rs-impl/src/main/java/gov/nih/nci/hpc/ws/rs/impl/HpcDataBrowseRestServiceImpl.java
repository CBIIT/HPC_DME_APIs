/**
 * HpcDataBrowseRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataBrowseBusService;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataBrowseRestService;

/**
 * <p>
 * HPC Data Browse REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataBrowseRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataBrowseRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Browse Business Service instance.
	@Autowired
    private HpcDataBrowseBusService dataBrowseBusService = null;
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataBrowseRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataBrowseRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response addBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
    {
		try {
			 dataBrowseBusService.addBookmark(bookmarkName, bookmarkRequest);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return createdResponse(null);
    }
    
    @Override
    public Response updateBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
    {
		try {
			 dataBrowseBusService.updateBookmark(bookmarkName, bookmarkRequest);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(null, false);
    }
    
    @Override
    public Response deleteBookmark(String bookmarkName)
    {
		try {
			 dataBrowseBusService.deleteBookmark(bookmarkName);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(null, false);
    }
    
    @Override
    public Response getBookmark(String bookmarkName)
    {
    	HpcBookmarkDTO bookmark = null;
		try {
			 bookmark = dataBrowseBusService.getBookmark(bookmarkName);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(bookmark.getBookmark() != null ? bookmark : null, true);
    }    

    @Override
    public Response getBookmarks()
    {
    	HpcBookmarkListDTO bookmarks = null;
		try {
			 bookmarks = dataBrowseBusService.getBookmarks();
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(!bookmarks.getBookmarks().isEmpty() ? bookmarks : null, true);
    }
}

 
