/**
 * HpcException.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.util;

import java.util.Optional;
import org.springframework.util.StringUtils;

/**
 * The HPC exception.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcUtil {

  public static final char[] FORBIDDEN_CHARS =
    new char[] { '\\', ';', '"' };

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


  public static boolean doesPathContainForbiddenChars(String argPath) {
    return doesPathContainForbiddenChars(argPath, Optional.empty());
  }


  public static boolean doesPathContainForbiddenChars(String argPath,
    Optional<String> forbiddenChars) {
    boolean pathClean = false;
    char[] badChars = null;
    if (forbiddenChars.isPresent() && !forbiddenChars.get().isEmpty()) {
      badChars = forbiddenChars.get().toCharArray();
    } else {
      badChars = FORBIDDEN_CHARS;
    }
    for (char someBadChar : badChars) {
      if (-1 != argPath.indexOf(someBadChar)) {
        pathClean = true;
        break;
      }
    }

    return pathClean;
  }

}
