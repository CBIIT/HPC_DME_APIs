/**
 * HpcAuthenticationExceptionMapper.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.provider;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
/**
 * <p>
 * Mapping HPC Exceptions to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcAuthenticationExceptionMapper 
             implements ExceptionMapper<HpcAuthenticationException>
{
	@Context
    private HttpHeaders headers;
	
    public Response toResponse(HpcAuthenticationException exception) 
    {
    	HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    	exceptionDTO.setMessage("Access Denied: " + exception.getMessage());
    	exceptionDTO.setErrorType(HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
    	
        return Response.status(Response.Status.UNAUTHORIZED).entity(exceptionDTO).type(headers.getMediaType()).build();
    }
}
