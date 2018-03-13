package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MiscUtil {

    private static final String EMPTY_STRING = "";
    private static final String FORWARD_SLASH = "/";


    public static String encodeDmePathForUrlEmbedding(
      String argDmePath,
      boolean argKeepLeadingFs,
      boolean argKeepTrailingFs) throws HpcWebException {
      String encodedDmePath = EMPTY_STRING;
      if ( null != argDmePath && !EMPTY_STRING.equals(argDmePath.trim()) ) {
        String effPath = argDmePath.trim();
        // Remove leading forward slash as needed
        if (!argKeepLeadingFs && effPath.startsWith(FORWARD_SLASH)) {
          effPath = effPath.substring(FORWARD_SLASH.length() + effPath.indexOf(FORWARD_SLASH));
        }
        // Remove trailing forward slash as needed
        if (!argKeepTrailingFs && effPath.endsWith(FORWARD_SLASH)) {
          effPath = effPath.substring(0, effPath.lastIndexOf(FORWARD_SLASH));
        }
        final String[] pathParts = effPath.split(FORWARD_SLASH);
        final StringBuilder sb = new StringBuilder();
        for (String somePathPart : pathParts) {
          if (sb.length() > 0) {
            sb.append(FORWARD_SLASH);
          }
          sb.append( performUrlEncoding(somePathPart) );
        }
        encodedDmePath = sb.toString();
      }
      return encodedDmePath;
    }



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


    public static String urlEncodeDmePathWithSlashTrimming(String argThePath) {
      return encodeDmePathForUrlEmbedding(argThePath, false, false);
    }


    public static String urlEncodeDmePathWithPreserveSlashAtEnds(String argThePath) {
      return encodeDmePathForUrlEmbedding(argThePath, true, true);
    }

}