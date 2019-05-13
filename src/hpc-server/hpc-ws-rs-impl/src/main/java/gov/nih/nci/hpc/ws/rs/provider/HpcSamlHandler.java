/**
 * HpcAuthentcationInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.provider;

import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.rs.security.saml.SamlHeaderInHandler;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.ws.rs.interceptor.HpcAuthenticationInterceptor;

/**
 * HPC SAML Handler.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcSamlHandler extends SamlHeaderInHandler {
   
    @Context
    private HttpHeaders headers;
    
    @Override
    public void filter(ContainerRequestContext context) {
       
      try {
        List<String> values = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (values == null || values.size() != 1 || !values.get(0).startsWith(HpcAuthenticationInterceptor.SAML_AUTHORIZATION)) {
            //If authentication header does not include a SAML token,
            //the SAML assertion validation is skipped.
        } else {
          super.filter(context);
        }
      } catch (Exception e) {
        throw new HpcAuthenticationException("Authentication failed", e);
      }
    }
    
}
