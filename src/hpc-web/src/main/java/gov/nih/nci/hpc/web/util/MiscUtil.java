package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class MiscUtil {

  private static final String EMPTY_STRING = "";
  private static final String FORWARD_SLASH = "/";

  private static UriComponentsBuilder ucBuilder = UriComponentsBuilder
    .newInstance().scheme("http").host("www.somehost.net").pathSegment("some",
    "path");


  public static String generateEncodedQueryString(Map<String, String>
    argQueryParamsMap) {
    final MultiValueMap<String, String> effMap = new LinkedMultiValueMap<>();
    for (String key : argQueryParamsMap.keySet()) {
      effMap.put(key, Collections.singletonList(argQueryParamsMap.get(key)));
    }
    return generateEncodedQueryString(effMap);
  }


  public static String generateEncodedQueryString(MultiValueMap<String, String>
    argQueryParamsMap) {
    return ucBuilder.replaceQueryParams(argQueryParamsMap).build().encode()
      .toUri().getRawQuery();
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

}