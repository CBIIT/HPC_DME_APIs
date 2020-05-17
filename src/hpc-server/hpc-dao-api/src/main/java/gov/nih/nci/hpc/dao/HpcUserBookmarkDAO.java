/**
 * HpcUserBookmarkDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Bookmark DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcUserBookmarkDAO 
{    
    /**
     * Upsert a bookmark for a user.
     *
     * @param nciUserId The user ID to save this bookmark for.
     * @param bookmark The bookmark.
     * @throws HpcException on database error.
     */
    public void upsertBookmark(String nciUserId, HpcBookmark bookmark) 
    		                  throws HpcException;
    
    /**
     * Delete a bookmark for a user.
     *
     * @param nciUserId The user ID to delete the bookmark for.
     * @param bookmarkName The bookmark name.
     * @throws HpcException on database error.
     */
    public void deleteBookmark(String nciUserId, String bookmarkName) 
    		                  throws HpcException;

    /**
     * Get all bookmarks for a user.
     *
     * @param nciUserId The registered user ID.
     * @return A list of bookmarks of this user.
     * @throws HpcException on database error.
     */
    public List<HpcBookmark> getBookmarks(String nciUserId) throws HpcException;
    
    /**
     * Get a bookmark by name for a user.
     *
     * @param nciUserId The registered user ID.
     * @param bookmarkName The bookmark name.
     * @return The requested bookmark.
     * @throws HpcException on database error.
     */
    public HpcBookmark getBookmark(String nciUserId, String bookmarkName) 
    		                      throws HpcException;

    
    /**
     * Get bookmarks by path for a user.
     *
     * @param nciUserId The registered user ID.
     * @param bookmarkPath The bookmark path.
     * @return The requested bookmark.
     * @throws HpcException on database error.
     */
	public List<HpcBookmark> getBookmarksByPath(String nciUserId, String bookmarkPath) throws HpcException;
}

 