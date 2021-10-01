/**
 * HpcSecurityRestServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import java.net.URLDecoder;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcSecurityRestService;

/**
 * HPC Security REST Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSecurityRestServiceImpl extends HpcRestServiceImpl
    implements HpcSecurityRestService {
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
   * @throws HpcException Constructor is disabled.
   */
  private HpcSecurityRestServiceImpl() throws HpcException {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcSecurityRestService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public Response registerUser(String nciUserId, HpcUserRequestDTO userRegistrationRequest) {
    try {
      securityBusService.registerUser(nciUserId, userRegistrationRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return createdResponse(null);
  }

  @Override
  public Response updateUser(String nciUserId, HpcUserRequestDTO userUpdateRequest) {
    try {
      securityBusService.updateUser(nciUserId, userUpdateRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response deleteUser(String nciUserId) {
    try {
      securityBusService.deleteUser(nciUserId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response getUser(String nciUserId) {
    HpcUserDTO user = null;
    try {
      user = securityBusService.getUser(nciUserId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(user, true);
  }

  @Override
  public Response getInvoker() {
    HpcUserDTO user = null;
    try {
      user = securityBusService.getUser(null);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(user, true);
  }

  @Override
  public Response getActiveUsers(
      String nciUserId,
      String firstNamePattern,
      String lastNamePattern,
      String doc,
      String defaultBasePath) {
    HpcUserListDTO users = null;
    try {
      users =
          securityBusService.getUsers(
              nciUserId, firstNamePattern, lastNamePattern, doc, defaultBasePath, true, false);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!users.getUsers().isEmpty() ? users : null, true);
  }


  @Override
  public Response getUsersByRole(
      String roleName,
      String doc,
      String defaultBasePath) {
    HpcUserListDTO users = null;
    try {
      users =
          securityBusService.getUsersByRole(
              roleName, doc, defaultBasePath, true);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!users.getUsers().isEmpty() ? users : null, true);
  }


  @Override
  public Response queryUsers(
      String nciUserId,
      String firstNamePattern,
      String lastNamePattern,
      String doc,
      String defaultBasePath) {
    HpcUserListDTO users = null;
    try {
      users =
          securityBusService.getUsers(
        	  nciUserId, firstNamePattern, lastNamePattern, doc, defaultBasePath, true, true);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!users.getUsers().isEmpty() ? users : null, true);
  }
  
  @Override
  public Response getAllUsers(
      String nciUserId,
      String firstNamePattern,
      String lastNamePattern,
      String doc,
      String defaultBasePath) {
    HpcUserListDTO users = null;
    try {
      users =
          securityBusService.getUsers(
              nciUserId, firstNamePattern, lastNamePattern, doc, defaultBasePath, false, false);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!users.getUsers().isEmpty() ? users : null, true);
  }

  @Override
  public Response getUserGroups(String nciUserId) {
	HpcGroupListDTO group = null;
    try {
    	group = securityBusService.getUserGroups(nciUserId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(group, true);
  }

  @Override
  public Response getInvokerGroups() {
	HpcGroupListDTO group = null;
    try {
    	group = securityBusService.getUserGroups(null);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(group, true);
  }
  
  @Override
  public Response authenticate() {
    HpcAuthenticationResponseDTO authenticationResponse = null;
    try {
      authenticationResponse = securityBusService.getAuthenticationResponse(true);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(authenticationResponse, false);
  }

  @Override
  public Response registerGroup(String groupName, HpcGroupMembersRequestDTO groupMembersRequest) {
    HpcGroupMembersResponseDTO groupMembersResponse = null;
    try {
      groupMembersResponse =
          securityBusService.registerGroup(URLDecoder.decode(groupName), groupMembersRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return createdResponse(null, groupMembersResponse);
  }

  @Override
  public Response updateGroup(String groupName, HpcGroupMembersRequestDTO groupMembersRequest) {
    HpcGroupMembersResponseDTO groupMembersResponse = null;
    try {
      groupMembersResponse =
          securityBusService.updateGroup(URLDecoder.decode(groupName), groupMembersRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(groupMembersResponse, false);
  }

  @Override
  public Response getGroup(String groupName) {
    HpcGroupMembersDTO groupMembers = null;
    try {
      groupMembers = securityBusService.getGroup(URLDecoder.decode(groupName));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(groupMembers, true);
  }

  @Override
  public Response getGroups(String groupPattern) {
    HpcGroupListDTO groups = null;
    try {
      groups = securityBusService.getGroups(groupPattern);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(groups, true);
  }

  @Override
  public Response deleteGroup(String groupName) {
    try {
      securityBusService.deleteGroup(URLDecoder.decode(groupName));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response registerSystemAccount(HpcSystemAccountDTO systemAccountRegistration) {
    try {
      securityBusService.registerSystemAccount(systemAccountRegistration);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return createdResponse(systemAccountRegistration.getAccount().getIntegratedSystem().value());
  }
  
  @Override
  public Response refreshDataManagementConfigurations() {
	
    try {
      securityBusService.refreshDataManagementConfigurations();

    } catch (HpcException e) {
      return errorResponse(e);
    }
    return okResponse(null, false);
   
  }
  
  @Override
  public Response sendToQueue(String queueName, String taskId) {
	
    try {
      securityBusService.sendToQueue(queueName, taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }
    return okResponse(null, false);
   
  }
}
