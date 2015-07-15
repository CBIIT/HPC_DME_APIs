/**
 * HpcAccessDeniedExceptionMapper.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * <p>
 * Mapping AccessDeniedException to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcAccessDeniedExceptionMapper implements ExceptionMapper<AuthenticationCredentialsNotFoundException> 
{
    public Response toResponse(AuthenticationCredentialsNotFoundException exception) 
    {
    	HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    	exceptionDTO.setMessage("Access Denied: " + exception.getMessage());
    	exceptionDTO.setErrorType(HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
    	
        return Response.status(Response.Status.FORBIDDEN).entity(exceptionDTO).build();
    }
}
