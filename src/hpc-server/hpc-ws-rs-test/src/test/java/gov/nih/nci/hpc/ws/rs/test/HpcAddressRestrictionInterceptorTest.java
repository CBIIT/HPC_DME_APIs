/**
 * HpcAddressRestrictionInterceptorTest.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import gov.nih.nci.hpc.ws.rs.interceptor.HpcAddressRestrictionInterceptor;

/**
 * HPC Address Restriction Interceptor JUnit Tests.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcAddressRestrictionInterceptorTest {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  private HpcAddressRestrictionInterceptor hpcAddressRestrictionInterceptor;

  private Message message;

  // The logger instance.
  protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  @Before
  public void setUp() throws Exception {
    hpcAddressRestrictionInterceptor = new HpcAddressRestrictionInterceptor();
    message = new MessageImpl();
  }

  //---------------------------------------------------------------------//
  // Unit Tests
  //---------------------------------------------------------------------//

  /** Restricted (IPV4 single address) */
  @Test
  public void testIpv4SingleRestrictedAddress() {
    setRemoteAddress("127.0.0.1");
    assertTrue(invokeHandleMessageAndCheckIfRestricted("127.0.0.1"));
  }

  /** Unrestricted (IPV4 single address) */
  @Test
  public void testIpv4SingleUnrestrictedAddress() {
    setRemoteAddress("127.0.0.1");
    assertFalse(invokeHandleMessageAndCheckIfRestricted("127.0.0.2"));
  }

  /** Restricted (IPV6 single address) */
  @Test
  public void testIpv6SingleRestrictedAddress() {
    setRemoteAddress("0:0:0:0:0:0:0:1");
    assertTrue(invokeHandleMessageAndCheckIfRestricted("0:0:0:0:0:0:0:1"));
  }

  /** Unrestricted (IPV6 single address) */
  @Test
  public void testIpv6SingleUnrestrictedAddress() {
    setRemoteAddress("0:0:0:0:0:0:0:2");
    assertFalse(invokeHandleMessageAndCheckIfRestricted("0:0:0:0:0:0:0:1"));
  }

  /** Restricted (IPV4 CIDR address) */
  @Test
  public void testIpv4CidrRestrictedAddress() {
    setRemoteAddress("127.0.0.1");
    //IPv4 address range, 127.0.0.0 to 127.0.0.7
    assertTrue(invokeHandleMessageAndCheckIfRestricted("127.0.0.0/29"));
  }

  /** Unrestricted (IPV4 CIDR address) */
  @Test
  public void testIpv4CidrUnrestrictedAddress() {
    setRemoteAddress("127.0.0.8");
    //IPv4 address range, 127.0.0.0 to 127.0.0.7
    assertFalse(invokeHandleMessageAndCheckIfRestricted("127.0.0.0/29"));
  }

  /** Restricted (IPV6 CIDR address) */
  @Test
  public void testIpv6CidrRestrictedAddress() {
    setRemoteAddress("0:0:0:0:0:0:0:3");
    //IPv6 address range, 0:0:0:0:0:0:0:0 to 0:0:0:0:0:0:0:3
    assertTrue(invokeHandleMessageAndCheckIfRestricted("0:0:0:0:0:0:0:1/126"));
  }

  /** Unrestricted (IPV6 CIDR address) */
  @Test
  public void testIpv6CidrUnrestrictedAddress() {
    setRemoteAddress("0:0:0:0:0:0:0:4");
    //IPv6 address range, 0:0:0:0:0:0:0:0 to 0:0:0:0:0:0:0:3
    assertFalse(invokeHandleMessageAndCheckIfRestricted("0:0:0:0:0:0:0:1/126"));
  }

  /** Restricted Proxy */
  @Test
  public void testRestrictedProxy() {
    setRemoteAddress("128.231.2.9", "10.1.200.242");
    assertTrue(invokeHandleMessageAndCheckIfRestricted("128.231.2.9"));
  }

  /** Unrestricted Proxy */
  @Test
  public void testUnrestrictedProxy() {
    setRemoteAddress("128.231.2.9", "10.1.200.242");
    assertFalse(invokeHandleMessageAndCheckIfRestricted("128.231.2.10"));
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  /**
   * Set the remoteAddress of the request
   *
   * @param remoteAddress The remote address
   */
  private void setRemoteAddress(String remoteAddress) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddress);
    message.put(AbstractHTTPDestination.HTTP_REQUEST, request);
  }

  /**
   * Set the remote and proxy address of the request
   *
   * @param remoteAddress The remote address
   * @param proxyAddress The proxy address
   */
  private void setRemoteAddress(String remoteAddress, String proxyAddress) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(proxyAddress);
    request.addHeader("X-Forwarded-For", remoteAddress);
    message.put(AbstractHTTPDestination.HTTP_REQUEST, request);
  }

  /**
   * Invoke the interceptor and check if user role is restricted.
   *
   * @param restrictedAddress The restricted address
   * @return true if restricted, false otherwise.
   */
  private boolean invokeHandleMessageAndCheckIfRestricted(String restrictedAddress) {
    List<String> restrictedTestAddress = new ArrayList<String>();
    restrictedTestAddress.add(restrictedAddress);
    ReflectionTestUtils.setField(
        hpcAddressRestrictionInterceptor, "restrictedAddress", restrictedTestAddress);
    hpcAddressRestrictionInterceptor.handleMessage(message);
    SecurityContext sc = (SecurityContext) message.get(SecurityContext.class);
    return sc == null ? false : sc.isUserInRole("RESTRICTED");
  }
}
