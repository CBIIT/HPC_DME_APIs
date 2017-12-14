/**
 * HpcDataBrowseBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Browse Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataBrowseBusService {
  /**
   * Add a bookmark for a user.
   *
   * @param bookmarkName The bookmark name.
   * @param bookmarkRequest The bookmark request DTO.
   * @throws HpcException on service failure.
   */
  public void addBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
      throws HpcException;

  /**
   * Update a bookmark for a user.
   *
   * @param bookmarkName The bookmark name.
   * @param bookmarkRequest The bookmark request DTO.
   * @throws HpcException on service failure.
   */
  public void updateBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest)
      throws HpcException;

  /**
   * Delete a bookmark.
   *
   * @param bookmarkName The bookmark name.
   * @throws HpcException on service failure.
   */
  public void deleteBookmark(String bookmarkName) throws HpcException;

  /**
   * Get a bookmark by name.
   *
   * @param bookmarkName The bookmark name.
   * @return The bookmark DTO.
   * @throws HpcException on service failure.
   */
  public HpcBookmarkDTO getBookmark(String bookmarkName) throws HpcException;

  /**
   * Get all saved bookmarks.
   *
   * @return A list of bookmark DTO.
   * @throws HpcException on service failure.
   */
  public HpcBookmarkListDTO getBookmarks() throws HpcException;
}
