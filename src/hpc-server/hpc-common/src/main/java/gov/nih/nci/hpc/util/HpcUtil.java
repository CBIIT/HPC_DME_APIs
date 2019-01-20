/**
 * HpcException.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.util;

import org.springframework.util.StringUtils;

/**
 * The HPC exception.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcUtil {
  // ---------------------------------------------------------------------//
  // constructors
  // ---------------------------------------------------------------------//

  /** Default constructor disabled */
  private HpcUtil() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Normalize a path. 1. It begins with '/' 2. No trailing '/' unless root. 3. No duplicate '/'.
   *
   * @param path The path.
   * @return The normalized path.
   */
  public static String toNormalizedPath(String path) {
    // Normalize the path - i.e. remove duplicate and trailing '/'
    String absolutePath = StringUtils.trimTrailingCharacter(path, '/').replaceAll("/+", "/");

    StringBuilder buf = new StringBuilder();
    if (absolutePath.isEmpty() || absolutePath.charAt(0) != '/') {
      buf.append('/');
    }
    buf.append(absolutePath);
    return buf.toString();
  }
}
