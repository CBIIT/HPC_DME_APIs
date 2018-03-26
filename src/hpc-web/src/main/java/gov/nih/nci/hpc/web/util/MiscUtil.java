package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class MiscUtil {

    private static final String EMPTY_STRING = "";

    private static final String FORBIDDEN_CHARS_IN_DME_PATHS = "?;";

    private static final String FORWARD_SLASH = "/";

    private static final String[] VALID_URI_SCHEMES =
      new String[] {"http","https"};

    private static final String
      EXCEPTION_MSG_TEMPLATE__UNEXPECTED_URL_PROTOCOL =
        "Unexpected protocol in given URL: %s.  Expect any of the".concat(
            " following protocols: %s.");

    private static final String
      EXCEPTION_MSG_TEMPLATE__PATH_HAS_FORBIDDEN_CHARS =
        "Invalid path received: %s.  For defining DME archive path,".concat(
        " please avoid using following forbidden characters: {").concat(
        FORBIDDEN_CHARS_IN_DME_PATHS).concat("}.");


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


/*
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
*/

  public static String encodeFullURL(String argRawURLString)
      throws URISyntaxException, MalformedURLException {
    String uriScheme = null;
    String uriPath = null;
    final int colonPos = argRawURLString.indexOf(":");
    if (-1 == colonPos) {
      uriScheme = "http"; // let http be default scheme
      uriPath = argRawURLString;
    } else {
      uriScheme = argRawURLString.substring(0, colonPos);
      if (validateUriScheme(uriScheme)) {
        uriPath = argRawURLString.substring(colonPos + 1);
      } else {
        throw genHpcWebException4BadUriScheme(argRawURLString);
      }
    }
    final String retUrlStr =
      new URI(uriScheme, uriPath, null).toURL().toString();

    return retUrlStr;
  }


  public static void validateDmePathForForbiddenChars(String argPathName)
    throws HpcWebException {
    for (char forbiddenChar : FORBIDDEN_CHARS_IN_DME_PATHS.toCharArray()) {
      if (argPathName.contains(Character.toString(forbiddenChar))) {
        throw new HpcWebException(String.format(
          EXCEPTION_MSG_TEMPLATE__PATH_HAS_FORBIDDEN_CHARS, argPathName));
      }
    }
  }

  private static HpcWebException genHpcWebException4BadUriScheme(
    String argGivenUrl) {
    final StringBuilder sb = new StringBuilder();
    for (String validScheme : VALID_URI_SCHEMES) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(validScheme);
    }
    return new HpcWebException(String.format(
        EXCEPTION_MSG_TEMPLATE__UNEXPECTED_URL_PROTOCOL,
        argGivenUrl,
        sb.toString()));
  }


  private static boolean validateUriScheme(String argScheme) {
    boolean retSignal = false;
    for (String validScheme : VALID_URI_SCHEMES) {
      if (validScheme.equals(argScheme)) {
        retSignal = true;
        break;
      }
    }
    return retSignal;
  }

}