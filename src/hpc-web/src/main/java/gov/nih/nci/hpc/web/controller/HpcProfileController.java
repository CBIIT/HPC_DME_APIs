package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final String NAV_OUTCOME_PROFILE = "profile";
  private static final String NAV_OUTCOME_REDIRECT_TO_LOGIN =
    "redirect:/login?returnPath=profile";

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

    // The logger instance.
    private final Logger logger =
        LoggerFactory.getLogger(this.getClass().getName());


  /**
   * @return
   */
  @RequestMapping(method = RequestMethod.GET)
  public String profile(
    @RequestBody(required = false) String q,
    Model model,
    BindingResult bindingResult,
    HttpSession session) {
    final HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    String navOutcome;
    if (user == null) {
      bindingResult.addError(new ObjectError("hpcLogin",
                                              "Invalid user session!"));
      model.addAttribute("hpcLogin", new HpcLogin());
      navOutcome = NAV_OUTCOME_REDIRECT_TO_LOGIN;
    } else {
      final String authToken = (String) session.getAttribute("hpcUserToken");
      final String userId = (String) session.getAttribute("hpcUserId");
      HpcDataManagementModelDTO modelDTO =
          (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
      if (modelDTO == null) {
        modelDTO = HpcClientUtil.getDOCModel(
            authToken, hpcModelURL, sslCertPath, sslCertPassword);
        session.setAttribute("userDOCModel", modelDTO);
      }
      HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken,
          userId, collectionAclURL, sslCertPath, sslCertPassword);
      final String queryParams = generateQueryString(modelDTO);
      final String url2Call = String.format("%s/%s", collectionAclURL, userId);
      final HpcUserPermsForCollectionsDTO permissions =
          HpcClientUtil.getPermissionForCollections(
              authToken, url2Call, queryParams, sslCertPath, sslCertPassword);

      log.info(String.format("authToken: %s", authToken));
      log.info(String.format("userId: %s", userId));
      log.info(String.format("userDOCModel: %s", modelDTO));
      log.info(String.format("permissions: %s", permissions));

      //HpcWebUser webUser = new HpcWebUser();
      model.addAttribute("profile", user);
      model.addAttribute("userDOCModel", modelDTO);
      model.addAttribute("permissions", permissions);
      navOutcome = NAV_OUTCOME_PROFILE;
    }

    return navOutcome;
  }

  private String generateQueryString(HpcDataManagementModelDTO argModelDTO) {
    final StringBuilder sb = new StringBuilder("?");
    boolean firstParamFlag = true;
    for (HpcDocDataManagementRulesDTO docRule : argModelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        if (firstParamFlag) {
          firstParamFlag = false;
        } else {
          sb.append("&");
        }
        sb.append("collectionPath=")
          .append(MiscUtil.urlEncodeDmePath(rule.getBasePath()));
      }
    }
    final String retQueryString = sb.toString();
    return retQueryString;
  }

}
