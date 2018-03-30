package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MiscUtil {

    private static final String EMPTY_STRING = "";
    private static final String FORWARD_SLASH = "/";


  public static String performUrlEncoding(String argInputStr) throws HpcWebException {
      String result;
      try {
        result = URLEncoder.encode(argInputStr,
          StandardCharsets.UTF_8.displayName());
        return result;
      } catch (UnsupportedEncodingException e) {
        throw new HpcWebException(e);
      }
    }


  public static String prepareUrlForExtending(String argOrigUrl) {
      final StringBuilder sb = new StringBuilder();
      sb.append(argOrigUrl.trim());
      if (!argOrigUrl.endsWith(FORWARD_SLASH)) {
        sb.append(FORWARD_SLASH);
      }
      final String preppedUrl = sb.toString();
      return preppedUrl;
    }


  public static String urlEncodeDmePath(String argThePath) {
      String encodedDmePath = null;
      if (null == argThePath) {
        encodedDmePath = null;
      } else if (EMPTY_STRING.equals(argThePath.trim())) {
        encodedDmePath = EMPTY_STRING;
      } else {
        final StringBuilder sb = new StringBuilder();
        final String[] pathParts = argThePath.trim().split(FORWARD_SLASH);
        boolean firstPartFlag = false;
        for (String somePathPart : pathParts) {
          if (firstPartFlag) {
            sb.append(FORWARD_SLASH);
          } else {
            firstPartFlag = true;
          }
          sb.append(performUrlEncoding(somePathPart));
        }
        encodedDmePath = sb.toString();
      }
      return encodedDmePath;
    }


 /*
  private static String removePrefix(String argText, String argPrefix) {
    String modText = null;
    if (null == argText) {
      modText = null;
    } else if (EMPTY_STRING.equals(argText) ||
        null == argPrefix ||
        EMPTY_STRING.equals(argPrefix) ||
        !argText.startsWith(argPrefix)) {
      modText = argText;
    } else {
      modText = argText.substring(argPrefix.length() +
          argText.indexOf(argPrefix));
    }
    return modText;
  }
*/

/*
  private static String removeSuffix(String argText, String argSuffix) {
    String modText = null;
    if (null == argText) {
      modText = null;
    } else if (EMPTY_STRING.equals(argText) ||
        null == argSuffix ||
        EMPTY_STRING.equals(argSuffix) ||
        !argText.endsWith(argSuffix)) {
      modText = argText;
    } else {
      modText = argText.substring(0, argText.lastIndexOf(argSuffix));
    }
    return modText;
  }
*/


/*
  private static String trimForwardSlashFromEnds(String argTheText) {
    return removeSuffix(
        removePrefix(argTheText, FORWARD_SLASH), FORWARD_SLASH);
  }
*/

}