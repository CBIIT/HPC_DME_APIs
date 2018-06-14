package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.web.HpcWebException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

public class MiscUtil {

  /*
   * Forbidden characters for naming DME archive items:
   * \ backslash
   * / forward slash
   * : colon
   * ; semicolon
   * * asterisk
   * ? question mark
   * | pipe
   * " double quote
   * < less than symbol (left angle bracket)
   * > greater than symbol (right angle bracket)
   */
  private static final char[] FORBIDDEN_CHARS_FOR_DME_ITEM_NAMING =
      "\\/:;*?\"|<>".toCharArray();

  private static final String EMPTY_STRING = "";
  private static final String FORWARD_SLASH = "/";

  private static final String MSG_TEMPLATE__WORD_HAS_FORBIDDEN_CHARS =
    "The word \"%s\" cannot be used for naming a HPC DME archive item due to" +
    " presence of forbidden character(s).  Set of forbidden character is %s.";


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


  /**
   * Generates display-friendly string indicating characters which are forbidden
   * in naming DME archive items.
   *
   * @param charSeparator - character sequence to separate adjacent forbidden
   *                         characters; if omitted, defaults to single
   *                         whitespace
   * @param enclosureLeft - character sequence to put to left of forbidden
   *                         characters for enclosure purposes; if omitted,
   *                         defaults to left curly brace followed by single
   *                         whitespace "{ "
   * @param enclosureRight - character sequence to put to right of forbidden
   *                          characters for enclosure purposes; if omitted,
   *                          defaults to single whitespace followed by right
   *                          curly brace " }"
   * @return display-friendly string
   */
  public static String
    generateDisplayFriendlyStringForForbiddenCharactersInArchiveItemNaming(
        Optional<String> charSeparator, Optional<String> enclosureLeft,
        Optional<String> enclosureRight) {
    StringBuilder sb = new StringBuilder();
    for (char c : FORBIDDEN_CHARS_FOR_DME_ITEM_NAMING) {
      if (sb.length() > 0) {
        sb.append(charSeparator.orElse(" "));
      }
      sb.append(c);
    }
    sb.insert(0, enclosureLeft.orElse("{ "));
    sb.append(enclosureRight.orElse(" }"));
    return sb.toString();
  }


  /**
   * Checks whether given string contains forbidden characters for naming DME
   * archive items.
   *
   * @param potentialName - given string
   * @return true if given string contains forbidden character(s);
   *          false otherwise
   */
  public static boolean containsForbiddenCharactersForDmeItemNaming(
    String potentialName) {
    for (char forbiddenChar : FORBIDDEN_CHARS_FOR_DME_ITEM_NAMING) {
      if (potentialName.contains(String.valueOf(forbiddenChar))) {
        return true;
      }
    }
    return false;
  }


  /**
   * Checks whether given string contains forbidden characters for naming DME
   * archive items and throws HpcWebException if found.  Does nothing otherwise.
   *
   * @param potentialName - given string
   * @throws HpcWebException if given string contains forbidden character(s)
   */
  public static void validateWordForDmeItemNaming(String potentialName)
    throws HpcWebException
  {
    if (containsForbiddenCharactersForDmeItemNaming(potentialName)) {
      String dispFriendlyListOfChars =
        generateDisplayFriendlyStringForForbiddenCharactersInArchiveItemNaming(
          Optional.empty(), Optional.empty(), Optional.empty());
      String msg = String.format(MSG_TEMPLATE__WORD_HAS_FORBIDDEN_CHARS,
        potentialName, dispFriendlyListOfChars);
      throw new HpcWebException(msg);
    }
  }

}