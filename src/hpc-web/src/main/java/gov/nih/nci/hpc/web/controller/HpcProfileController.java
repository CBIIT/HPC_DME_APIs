package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.neo4j.cypher.internal.compiler.v2_1.functions.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.util.UriComponentsBuilder;

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
            ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
            bindingResult.addError(error);
            HpcLogin hpcLogin = new HpcLogin();
            model.addAttribute("hpcLogin", hpcLogin);
            return "redirect:/login?returnPath=profile";
        }
        String authToken = (String) session.getAttribute("hpcUserToken");
        String userId = (String) session.getAttribute("hpcUserId");

        HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
      if (modelDTO == null) {
            modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
        session.setAttribute("userDOCModel", modelDTO);
      }
      HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken,
          userId, collectionAclURL, sslCertPath, sslCertPassword);
      final UriComponentsBuilder ucBuilder =
        UriComponentsBuilder.fromHttpUrl(collectionAclURL)
                            .queryParams(generateQueryParamsMap(modelDTO));
      final HpcUserPermsForCollectionsDTO permissions =
          HpcClientUtil.getPermissionForCollections(authToken,
          ucBuilder.toUriString(), sslCertPath, sslCertPassword);

        HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, userId, collectionAclURL,
            sslCertPath, sslCertPassword);

        String queryParams = "?";

        for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
                queryParams += "collectionPath=" + rule.getBasePath() + "&";
        }
      }

       HpcUserPermsForCollectionsDTO permissions = HpcClientUtil.getPermissionForCollections(authToken,  collectionAclURL+"/"+userId,  queryParams,
            sslCertPath, sslCertPassword);

        log.info("authToken: " + authToken);
        log.info("userId: " + userId);
        log.info("userDOCModel: " + modelDTO);
        log.info("permissions: " + permissions);

        HpcWebUser webUser = new HpcWebUser();
        model.addAttribute("profile", user);
        model.addAttribute("userDOCModel", modelDTO);
        model.addAttribute("permissions", permissions);
        return "profile";
    }

  private MultiValueMap<String, String> generateQueryParamsMap(
    HpcDataManagementModelDTO argModelDTO) {
    final MultiValueMap<String, String> mvMap = new LinkedMultiValueMap<>();
    for (HpcDocDataManagementRulesDTO docRule : argModelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        mvMap.set("collectionPath", rule.getBasePath());
      }
    }
    return mvMap;
  }

}
