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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.bus.HpcDataBrowseBusService;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataBrowseService;
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

    // Get the user-id of this request invoker.
    String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();

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
  }

  @Override
  public void updateBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(bookmarkName) || bookmarkRequest == null) {
      throw new HpcException(
          "Null or empty bookmark name / bookmark request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the user-id of this request invoker.
    String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();

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
    bookmarkList
        .getBookmarks()
        .addAll(
            dataBrowseService.getBookmarks(
                securityService.getRequestInvoker().getNciAccount().getUserId()));

    return bookmarkList;
  }
}
