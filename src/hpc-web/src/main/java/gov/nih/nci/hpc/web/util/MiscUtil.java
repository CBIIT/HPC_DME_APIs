package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class MiscUtil {

    private static final String EMPTY_STRING = "";
    private static final String FORWARD_SLASH = "/";


  public static String performUrlEncoding(String argInputStr) throws HpcWebException {
      String result;
      try {
        result = URLEncoder.encode(argInputStr, "UTF-8");
        return result;
      } catch (UnsupportedEncodingException e) {
        throw new HpcWebException(e);
      }
    }


  public static String prepareUrlForExtending(String argOrigUrl) {
      final StringBuilder sb = new StringBuilder();
      sb.append(argOrigUrl.trim());
      if (!argOrigUrl.endsWith("/")) {
        sb.append("/");
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


    public static String encodeFullURL(String argRawURLString)
        throws URISyntaxException, MalformedURLException {
      String retURL = null;
      final int posFirstColon = argRawURLString.indexOf(":");
      if (-1 == posFirstColon) {
        retURL = new URI(argRawURLString).toURL().toString();
      } else {
        // [scheme:]scheme-specific-part[#fragment]
        final String scheme = argRawURLString.substring(0, posFirstColon);
        String schemeSpecificPart;
        String fragment;
        final int posLastHash = argRawURLString.lastIndexOf("#");
        if (-1 == posLastHash) {
          schemeSpecificPart = argRawURLString.substring(1 + posFirstColon);
          fragment = null;
        } else {
          schemeSpecificPart = argRawURLString.substring(
            1 + posFirstColon, posLastHash);
          fragment = argRawURLString.substring(1 + posLastHash);
        }
        retURL = new URI(scheme, schemeSpecificPart, fragment)
                      .toURL().toString();
      }

      return retURL;
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