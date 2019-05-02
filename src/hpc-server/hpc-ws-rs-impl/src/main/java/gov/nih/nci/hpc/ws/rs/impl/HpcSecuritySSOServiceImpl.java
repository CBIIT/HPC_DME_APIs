/**
 * HpcSecurityRestServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import javax.ws.rs.core.Response;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcSecuritySSOService;

/**
 * HPC Security Single Sign On Service Implementation.
 *
 * @author dinhys
 */
public class HpcSecuritySSOServiceImpl extends HpcRestServiceImpl
    implements HpcSecuritySSOService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The Security Business Service instance.
  @Autowired private HpcSecurityBusService securityBusService = null;
      
  //---------------------------------------------------------------------//
  // constructors
  //---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   *
   * Constructor is disabled.
   */
  private HpcSecuritySSOServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcSecuritySSOService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public Response authenticate() {
    HpcAuthenticationResponseDTO authenticationResponse = null;
    try {
      Message message = PhaseInterceptorChain.getCurrentMessage();
      SAMLSecurityContext context = message.get(SAMLSecurityContext.class);
  
      // TODO Create token based on SAML expiration
      authenticationResponse = securityBusService.getAuthenticationResponse(true);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(authenticationResponse, false);
  }
  
}
