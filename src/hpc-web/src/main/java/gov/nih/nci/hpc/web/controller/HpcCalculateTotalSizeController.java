/**
 * HpcCalculateTotalSizeController.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for
 * details.
 */
package gov.nih.nci.hpc.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcCalculateTotalSizeResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.util.HpcClientUtil;



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
@RequestMapping("/api/usage")
@ResponseBody
public class HpcCalculateTotalSizeController extends AbstractHpcController {

  @Value("${gov.nih.nci.hpc.server.external.calculateTotalSize}")
  private String hpcCalculateTotalSizeURL;

  @GetMapping(value = "/calcTotalSize", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getExternalArchives(@RequestParam(value = "path") String path,
      HttpSession session, HttpServletRequest request) throws HpcWebException {

    // Verify User session
    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    String authToken = (String) session.getAttribute("hpcUserToken");

    if (user == null || authToken == null) {
      return new ResponseEntity<>("Authentication error", HttpStatus.UNAUTHORIZED);
    }

    log.info("calc total size of external archive path: " + path);
    String[] paths = {path};

    try {
      // Calculate total size for an external path
      HpcCalculateTotalSizeResponseDTO calculateTotalSizeDTO =
          HpcClientUtil.calcTotalSizeForExternalPath(authToken, hpcCalculateTotalSizeURL,
              sslCertPath, sslCertPassword, paths);

      return new ResponseEntity<>(calculateTotalSizeDTO, HttpStatus.OK);
    } catch (HpcWebException e) {
      log.error("Failed to calculate total size of external archive path: " + path);
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

}
