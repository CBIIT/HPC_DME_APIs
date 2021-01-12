package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@EnableAutoConfiguration
@RequestMapping("/profile")
public class HpcProfileController extends AbstractHpcController {
    @Value("${gov.nih.nci.hpc.server.acl}")
    private String serverAclURL;
    @Value("${gov.nih.nci.hpc.server.user}")
    private String serviceURL;
    @Value("${gov.nih.nci.hpc.server.collection}")
    private String serverCollectionURL;
    @Value("${gov.nih.nci.hpc.server.dataObject}")
    private String serverDataObjectURL;
    @Value("${hpc.serviceaccount}")
    private String serviceAccount;
    @Value("${gov.nih.nci.hpc.server.user.all}")
    private String allUsersServiceURL;
    @Value("${gov.nih.nci.hpc.server.user.active}")
    private String activeUsersServiceURL;
    @Value("${gov.nih.nci.hpc.server.collection.acl.user}")
    private String collectionAclURL;
    @Value("${gov.nih.nci.hpc.server.model}")
    private String hpcModelURL;
    
    @Autowired
    private HpcModelBuilder hpcModelBuilder;
    
    // The logger instance.
    private final Logger logger =
        LoggerFactory.getLogger(this.getClass().getName());


    /**
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public String profile(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
                          HttpSession session, HttpServletRequest request) {
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      if (user == null) {
          ObjectError error = new ObjectError("hpcLogin",
            "Invalid user session!");
          bindingResult.addError(error);
          HpcLogin hpcLogin = new HpcLogin();
          model.addAttribute("hpcLogin", hpcLogin);
          return "redirect:/login?returnPath=profile";
      }
      final String authToken = (String) session.getAttribute("hpcUserToken");
      log.info("authToken: " + authToken);
      final String userId = (String) session.getAttribute("hpcUserId");
      log.info("userId: " + userId);
      HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO)
        session.getAttribute("userDOCModel");
      if (modelDTO == null) {
    	//Get DOC Models, go to server only if not available in cache
          modelDTO = hpcModelBuilder.getDOCModel(authToken, this.hpcModelURL, this.sslCertPath, this.sslCertPassword);
          session.setAttribute("userDOCModel", modelDTO);
      }
      log.info("userDOCModel: " + modelDTO);

      final List<String> collPaths = new ArrayList<>();
      for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
        for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
          collPaths.add(rule.getBasePath());
        }
      }
      final HpcUserPermsForCollectionsDTO permissions = HpcClientUtil
        .getPermissionForCollections(authToken, this.collectionAclURL, userId,
        collPaths.toArray(), this.sslCertPath, this.sslCertPassword);
      log.info("permissions: " + permissions);

      model.addAttribute("profile", user);
      model.addAttribute("userDOCModel", modelDTO);
      model.addAttribute("permissions", permissions);

      return "profile";
    }

}
