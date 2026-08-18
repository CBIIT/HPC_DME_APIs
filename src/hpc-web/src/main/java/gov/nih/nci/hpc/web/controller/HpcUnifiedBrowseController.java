/**
 * HpcUnifiedBrowseController.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for
 * details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcListObjectsResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;



/**
 * <p>
 * Unified browse controller to return user external archive to front end
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/global")
@ResponseBody
public class HpcUnifiedBrowseController extends AbstractHpcController {

  @Value("${gov.nih.nci.hpc.server.external.listObjects}")
  private String hpcListObjectsURL;

  @Value("${gov.nih.nci.hpc.server.model}")
  private String hpcModelURL;

  @Autowired
  private HpcModelBuilder hpcModelBuilder;

  @GetMapping(value = "/externalArchives", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getExternalArchives(HttpSession session, HttpServletRequest request) {

    // Verify User session
    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    String authToken = (String) session.getAttribute("hpcUserToken");

    if (user == null || authToken == null) {
    	return new ResponseEntity<>("Authentication error", HttpStatus.UNAUTHORIZED);
    }

    log.info("get external archives for user: " + user.getUserId());

    try {
      // Get User DOC model for base path
      HpcDataManagementModelDTO modelDTO =
          (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
      if (modelDTO == null) {
        // go to server only if not available in cache
        modelDTO =
            hpcModelBuilder.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
        session.setAttribute("userDOCModel", modelDTO);
      }

      // Get external archive with user permissions
      if (session.getAttribute("userBasePaths") == null) {
        String userId = (String) session.getAttribute("hpcUserId");
        HpcPermission[] hpcPermissions =
            {HpcPermission.OWN, HpcPermission.WRITE, HpcPermission.READ};
        populateUserBasePaths(modelDTO, authToken, userId, hpcPermissions, "userBasePaths",
            sslCertPath, sslCertPassword, session, hpcModelBuilder);
      }
      Set<String> userExternalArchives = (Set<String>) session.getAttribute("userExternalArchives");

      return new ResponseEntity<>(userExternalArchives, HttpStatus.OK);
    } catch (Exception e) {
      log.error("Failed to get external archives for user: " + user.getUserId());
      throw new HpcWebException(e);
    }

  }

  @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getExternalArchives(@RequestParam(value = "path") String path,
      HttpSession session, HttpServletRequest request) throws HpcWebException {

    // Verify User session
    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    String authToken = (String) session.getAttribute("hpcUserToken");

    if (user == null || authToken == null) {
      return new ResponseEntity<>("Authentication error", HttpStatus.UNAUTHORIZED);
    }

    log.info("list external archive path: " + path);

    try {
      // Get directory listing of an external path
      HpcListObjectsResponseDTO listObjectsDTO = HpcClientUtil.listObjectsForExternalPath(authToken,
          hpcListObjectsURL, sslCertPath, sslCertPassword, path);

      return new ResponseEntity<>(listObjectsDTO, HttpStatus.OK);
    } catch (HpcWebException e) {
      log.error("Failed to list external archive path: " + path);
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

}
