/**
 * HpcDataBrowseService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.List;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Browse Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataBrowseService {
  /**
   * Save a bookmark for a user.
   *
   * @param nciUserId The user ID to save the bookmark for.
   * @param bookmark The bookmark.
   * @throws HpcException on service failure.
   */
  public void saveBookmark(String nciUserId, HpcBookmark bookmark) throws HpcException;

  /**
   * Delete a bookmark for a user.
   *
   * @param nciUserId The user ID to delete the bookmark for.
   * @param bookmarkName The bookmark name.
   * @throws HpcException on service failure.
   */
  public void deleteBookmark(String nciUserId, String bookmarkName) throws HpcException;

  /**
   * Get all saved bookmarks for a user.
   *
   * @param nciUserId The registered user ID.
   * @return A list of bookmarks
   * @throws HpcException on service failure.
   */
  public List<HpcBookmark> getBookmarks(String nciUserId) throws HpcException;

  /**
   * Get a saved bookmark by name for a user.
   *
   * @param nciUserId The registered user ID.
   * @param bookmarkName The bookmark name.
   * @return The requested bookmark.
   * @throws HpcException on service failure.
   */
  public HpcBookmark getBookmark(String nciUserId, String bookmarkName) throws HpcException;
  
  
  /**
   * Get the saved list of bookmarks by path for a user.
   *
   * @param nciUserId The registered user ID.
   * @param bookmarkPath The bookmark path.
   * @return The requested bookmark.
   * @throws HpcException on service failure.
   */
  public List<HpcBookmark> getBookmarksByPath(String nciUserId, String bookmarkPath) throws HpcException;
}
