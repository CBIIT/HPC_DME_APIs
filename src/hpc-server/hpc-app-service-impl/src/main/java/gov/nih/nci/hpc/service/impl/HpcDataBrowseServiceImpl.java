/**
 * HpcDataBrowseServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidBookmark;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcUserBookmarkDAO;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataBrowseService;

/**
 * HPC Data Browse Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataBrowseServiceImpl implements HpcDataBrowseService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // User Bookmark DAO.
  @Autowired private HpcUserBookmarkDAO userBookmarkDAO = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @throws HpcException Constructor is disabled.
   */
  private HpcDataBrowseServiceImpl() throws HpcException {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcDataBrowseService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void saveBookmark(String nciUserId, HpcBookmark bookmark) throws HpcException {
    // Validate the bookmark
    HpcDomainValidationResult validationResult = isValidBookmark(bookmark);
    if (!validationResult.getValid()) {
      throw new HpcException(
          "Invalid bookmark request: " + validationResult.getMessage(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Set the update timestamp.
    bookmark.setUpdated(Calendar.getInstance());

    // Upsert the bookmark.
    userBookmarkDAO.upsertBookmark(nciUserId, bookmark);
  }

  @Override
  public void deleteBookmark(String nciUserId, String bookmarkName) throws HpcException {
    // Input validation.
    if (getBookmark(nciUserId, bookmarkName) == null) {
      throw new HpcException(
          "Bookmark doesn't exist: " + bookmarkName, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    userBookmarkDAO.deleteBookmark(nciUserId, bookmarkName);
  }

  @Override
  public List<HpcBookmark> getBookmarks(String nciUserId) throws HpcException {
    return userBookmarkDAO.getBookmarks(nciUserId);
  }

  @Override
  public HpcBookmark getBookmark(String nciUserId, String bookmarkName) throws HpcException {
    return userBookmarkDAO.getBookmark(nciUserId, bookmarkName);
  }
  
  @Override
  public List<HpcBookmark> getBookmarksByPath(String nciUserId, String bookmarkPath) throws HpcException {
    return userBookmarkDAO.getBookmarksByPath(nciUserId, bookmarkPath);
  }
}
