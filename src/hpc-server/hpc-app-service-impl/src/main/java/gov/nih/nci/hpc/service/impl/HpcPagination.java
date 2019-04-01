/**
 * HpcPagination.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Pagination Support.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcPagination {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The page size.
  private int pageSize = 0;

  //The max page size.
  private int maxPageSize = 0;
 
  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Default constructor disabled.
   *
   * @throws HpcException Constructor disabled.
   */
  private HpcPagination() throws HpcException {
    throw new HpcException("Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param pageSize The page size
   */
  private HpcPagination(int pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * Constructor for Pagination used in Search.
   *
   * @param pageSize The page size
   * @param maxPageSize The max page size
   */
  private HpcPagination(int pageSize, int maxPageSize) {
    this.pageSize = pageSize;
    this.maxPageSize = maxPageSize;
  }
  
  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Get the page size.
   *
   * @return The page size.
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Get the max page size.
   *
   * @return The page size.
   */
  public int getMaxPageSize() {
    return maxPageSize;
  }
  
  /**
   * Calculate search offset by requested page.
   *
   * @param page The requested page.
   * @return The calculated offset
   * @throws HpcException if the page is invalid.
   */
  public int getOffset(int page) throws HpcException {
    if (page < 1) {
      throw new HpcException(
          "Invalid search results page: " + page, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return (page - 1) * pageSize;
  }
}
