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

  /**
   * Forbidden characters in DME archive paths as array of char values.
   */
  public static final char[] FORBIDDEN_CHARS_ARRAY =
    new char[] { '\\', ';', '?' };

  /**
   * String representation of FORBIDDEN_CHARS_ARRAY
   */
  public static final String FORBIDDEN_CHARS_STRING =
    String.valueOf(FORBIDDEN_CHARS_ARRAY);

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


  /**
   * Check if given path string contains forbidden characters which are
   * defined as those in this class's constant named FORBIDDEN_CHARS_ARRAY.
   *
   * @param argPath path string
   * @return boolean true if given path string contains forbidden characters
   *          or false otherwise
   */
  public static boolean doesPathContainForbiddenChars(String argPath) {
    return doesPathContainForbiddenChars(argPath, Optional.empty());
  }


  /**
   * Check if given path string contains forbidden characters.
   *
   * @param argPath path string
   * @param forbiddenChars Optional<String> that if set, has value interpreted
   *                        as string comprised of all forbidden characters;
   *                        if unset, this method falls back on this class's
   *                        constant FORBIDDEN_CHARS_ARRAY.
   * @return boolean true if given path string contains forbidden characters
   *          or false otherwise
   */
  public static boolean doesPathContainForbiddenChars(String argPath,
    Optional<String> forbiddenChars) {
    boolean pathClean = false;
    char[] badChars = null;
    if (forbiddenChars.isPresent() && !forbiddenChars.get().isEmpty()) {
      badChars = forbiddenChars.get().toCharArray();
    } else {
      badChars = FORBIDDEN_CHARS_ARRAY;
    }
    for (char someBadChar : badChars) {
      if (-1 != argPath.indexOf(someBadChar)) {
        pathClean = true;
        break;
      }
    }

    return pathClean;
  }


  /**
   * Generates formatted string representation of forbidden characters.
   *
   * @param forbiddenChars Optional that if set, contains the forbidden
   *                       characters; if unset, this class's constant
   *                       FORBIDDEN_CHARS_ARRAY is applied.
   * @param seriesDelimiter Optional that if set, is text to use for
   *                        delimiting (separating) adjacent forbidden
   *                        characters; if unset, single space is applied
   *                        as delimiter (separator).
   * @param prefix Optional that if set, is text that belongs before the series
   *               of forbidden characters; could be something like left
   *               parenthesis or left square bracket; only applied if both this
   *               parameter and "suffix" parameter are set.
   * @param suffix Optional that if set, is text that belongs after the series
   *               of forbidden characters; could be something like right
   *               parenthesis or right square bracket; only applied if both
   *               this parameter and "prefix" parameter are set.
   *
   * @return formatted string that conveys forbidden characters
   */
  public static String generateForbiddenCharsFormatted(
    Optional<char[]> forbiddenChars,
    Optional<String> seriesDelimiter,
    Optional<String> prefix,
    Optional<String> suffix) {
    final char[] forbiddenChars2Apply = forbiddenChars.orElse(
      FORBIDDEN_CHARS_ARRAY);
    final String effSeriesDelimiter = seriesDelimiter.orElse(" ");
    final StringBuilder sb = new StringBuilder();
    for (char c : forbiddenChars2Apply) {
      if (sb.length() > 0) {
        sb.append(effSeriesDelimiter);
      }
      sb.append(c);
    }
    if (prefix.isPresent() && suffix.isPresent()) {
      sb.insert(0, prefix.get());
      sb.append(suffix.get());
    }
    final String forbiddenCharsFormatted = sb.toString();
    return forbiddenCharsFormatted;
  }

}
