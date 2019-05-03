/**
 * HpcAuthentcationInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.rs.security.saml.SamlHeaderInHandler;

/**
 * HPC SAML Handler.
 *
 * @author dinhys
 */
public class HpcSamlHeaderInHandler extends SamlHeaderInHandler {

    private static final String SAML_AUTH = "SAML";
    
    @Context
    private HttpHeaders headers;
    
    @Override
    public void filter(ContainerRequestContext context) {
       
        List<String> values = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (values == null || values.size() != 1 || !values.get(0).startsWith(SAML_AUTH)) {
            //do nothing
        } else {
          super.filter(context);
        }         
    }
    
}
