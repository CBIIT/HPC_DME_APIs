/**
 * HpcSystemAccountFunctionNoReturn.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC system account functional Interface w/ no return type.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcSystemAccountFunctionNoReturn {
  /**
   * Perform this code using system account credentials.
   *
   * @throws HpcException On any failure.
   */
  public void execute() throws HpcException;
}


