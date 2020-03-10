/**
 * HpcDataBrowseBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.bus.HpcDataBrowseBusService;
import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataBrowseService;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Browse Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataBrowseBusServiceImpl implements HpcDataBrowseBusService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Data Browse Application Service instance.
  @Autowired private HpcDataBrowseService dataBrowseService = null;

  // Security Application Service instance.
  @Autowired private HpcSecurityService securityService = null;
  
  //Data Management Application Service instance
  @Autowired private HpcDataManagementService dataManagementService = null;
  
  //Data Management Security Application Service instance
  @Autowired private HpcDataManagementSecurityService dataManagementSecurityService = null;
  
  //Data Management Security Bussiness Service instance
  @Autowired private HpcSecurityBusService hpcSecurityBusService = null;

  //The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataBrowseBusServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcDataBrowseBusService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void addBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(bookmarkName) || bookmarkRequest == null) {
      throw new HpcException(
          "Null or empty bookmark name / bookmark request", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    

    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    String nciUserId = bookmarkRequest.getUserId();
    if(nciUserId == null) {
    	//No userId specified, so set it to be the userId of the requester
    	nciUserId = invoker.getNciAccount().getUserId();
    } else {
    	//Ensure that the reauestor is authorized to add bookmark for someone else
    	if (!invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN) &&
    			!invoker.getUserRole().equals(HpcUserRole.GROUP_ADMIN)) {
    		throw new HpcException(
    	            "Not authorized to add bookmark for user " + nciUserId + ". Please contact system administrator",
    	            HpcRequestRejectReason.NOT_AUTHORIZED);
    	}
    	//If the given userId is not a group and the user is not present in DME
	    if (!dataManagementSecurityService.groupExists(nciUserId) && securityService.getUser(nciUserId) == null) {
	    	createUser(invoker, nciUserId);
	    }
    }
    
    
    if (dataBrowseService.getBookmark(nciUserId, bookmarkName) != null) {
      throw new HpcException(
          "Bookmark name already exists: " + bookmarkName, HpcErrorType.INVALID_REQUEST_INPUT);
    }
    
    HpcBookmark bookmark = new HpcBookmark();
    bookmark.setName(bookmarkName);
    bookmark.setPath(bookmarkRequest.getPath());
    bookmark.setGroup(bookmarkRequest.getGroup());
    bookmark.setCreated(Calendar.getInstance());

    // Save the bookmark.
    dataBrowseService.saveBookmark(nciUserId, bookmark);

    //Set the permission to the bookmark path
    if(bookmarkRequest.getPermission() != null) {

      try {
	      
	
	      HpcSubjectPermission subjectPermission = new HpcSubjectPermission();
	      subjectPermission.setPermission(bookmarkRequest.getPermission());
	      subjectPermission.setSubject(nciUserId);
	      
	      String path = bookmarkRequest.getPath();
	      if(dataManagementService.interrogatePathRef(path)) {
	    	  dataManagementService.setCollectionPermission(path, subjectPermission);
	      } else {
	    	  dataManagementService.setDataObjectPermission(path, subjectPermission);
	      }
      } catch(Exception e) {
    	dataBrowseService.deleteBookmark(nciUserId, bookmarkName);
    	throw e;
      }
    }
  }

  @Override
  public void updateBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(bookmarkName) || bookmarkRequest == null) {
      throw new HpcException(
          "Null or empty bookmark name / bookmark request", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    
    HpcRequestInvoker invoker = securityService.getRequestInvoker();    
    String nciUserId = bookmarkRequest.getUserId();
    if(nciUserId == null) {
    	//No userId specified, so set it to be the userId of the requester
    	nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();
    } else {
    	//Ensure that the reauestor is authorized to add bookmark for someone else
    	if (!invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN) &&
			!invoker.getUserRole().equals(HpcUserRole.GROUP_ADMIN)) {
		throw new HpcException(
	            "Not authorized to update bookmark for user " + nciUserId + ". Please contact system administrator",
	            HpcRequestRejectReason.NOT_AUTHORIZED);
	}
}
    
    // Get the bookmark.
    HpcBookmark bookmark = dataBrowseService.getBookmark(nciUserId, bookmarkName);
    if (bookmark == null) {
      throw new HpcException(
          "Bookmark name doesn't exist: " + bookmarkName, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Update the bookmark.
    if (bookmarkRequest.getPath() != null) {
      bookmark.setPath(bookmarkRequest.getPath());
    }
    if (bookmarkRequest.getGroup() != null) {
      bookmark.setGroup(bookmarkRequest.getGroup());
    }

    // Save the bookmark.
    dataBrowseService.saveBookmark(nciUserId, bookmark);

    //Set the permission to the bookmark path 
    if(bookmarkRequest.getPermission() != null) {

      if (!dataManagementSecurityService.groupExists(nciUserId) && securityService.getUser(nciUserId) == null) { 
        createUser(invoker, nciUserId);
      }

      HpcSubjectPermission subjectPermission = new HpcSubjectPermission();
      subjectPermission.setPermission(bookmarkRequest.getPermission());
      subjectPermission.setSubject(nciUserId);
      try {
    	String path = bookmarkRequest.getPath();
      	if(dataManagementService.interrogatePathRef(path)) {
      		dataManagementService.setCollectionPermission(path, subjectPermission);
      	} else {
      		dataManagementService.setDataObjectPermission(path, subjectPermission);
      	}
      } finally {
    	dataBrowseService.deleteBookmark(nciUserId, bookmarkName);
      }
    }
  }

  @Override
  public void deleteBookmark(String bookmarkName) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(bookmarkName)) {
      throw new HpcException("Null or empty bookmark name", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Delete the query.
    dataBrowseService.deleteBookmark(
        securityService.getRequestInvoker().getNciAccount().getUserId(), bookmarkName);
  }

  @Override
  public HpcBookmarkDTO getBookmark(String bookmarkName) throws HpcException {
    HpcBookmarkDTO bookmarkDTO = new HpcBookmarkDTO();
    bookmarkDTO.setBookmark(
        dataBrowseService.getBookmark(
            securityService.getRequestInvoker().getNciAccount().getUserId(), bookmarkName));

    return bookmarkDTO;
  }

  @Override
  public HpcBookmarkListDTO getBookmarks() throws HpcException {
    HpcBookmarkListDTO bookmarkList = new HpcBookmarkListDTO();
    String userId = securityService.getRequestInvoker().getNciAccount().getUserId();
    
    //Get bookmarks for this user
    bookmarkList
        .getBookmarks()
        .addAll(
            dataBrowseService.getBookmarks(userId));
    
    //Get bookmarks for all the groups this user belongs to
    List<String> groups = dataManagementSecurityService.getUserGroups(userId);
    for(String group: groups) {
        //Get all the bookmarks on this group
    	List<HpcBookmark> groupBookmarks = dataBrowseService.getBookmarks(group);
    	bookmarkList.getBookmarks().addAll(groupBookmarks);		
    }

    return bookmarkList;
  }


  //---------------------------------------------------------------------//
	// Helper Methods
  // ---------------------------------------------------------------------//


  private void createUser(HpcRequestInvoker invoker, String nciUserId)
      throws HpcException {

    //User does not exist, create if the requester has group admin privileges.
    if(HpcUserRole.GROUP_ADMIN.equals(invoker.getUserRole()) || HpcUserRole.SYSTEM_ADMIN.equals(invoker.getUserRole())) {
      HpcUserRequestDTO userRegistrationRequest = new HpcUserRequestDTO();
      userRegistrationRequest.setDoc(invoker.getNciAccount().getDoc());
      hpcSecurityBusService.registerUser(nciUserId, userRegistrationRequest);
      logger.info("Added new user: " + nciUserId + " for setting permissions");
    } else {
      throw new HpcException("User not found: " + nciUserId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
    }

  }


}
