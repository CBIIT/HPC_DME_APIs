/**
 * HpcGlobusFileVisitor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.globus.impl;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * HPC Globus File Visitor Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcGlobusFileVisitor {
  /**
   * Called when a file is found during directory scan/traverse.
   *
   * @param path The directory path containing the file.
   * @param jsonFile The visited file JSON data.
   * @throws JSONException If it failed to visit the file.
   */
  public void onFile(String path, JSONObject jsonFile) throws JSONException;
}
