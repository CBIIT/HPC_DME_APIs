package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * Controller to delete a collection.
 * </p>
 *
 * @author <a href=mailto:mailto:wiliam.liu2@nih.gov">William Liu</a>
 * @version $Id$
 */
@Controller
@EnableAutoConfiguration
public class HpcDeleteCollectionController extends AbstractHpcController {

    @Value("${gov.nih.nci.hpc.server.collection}")
    private String collectionURL;

    @RequestMapping(method = RequestMethod.POST, path = "/collection/delete")
    public String processDeleteCollection(
            @RequestParam("collectionPath4Delete") String collectionPath,
            HttpSession session,
            Model model) {
        String resultNav = null;
        //System.out.println("Received collectionPath method param as collectionPath4Delete request param, value is " + collectionPath);
        try {
            final String authToken = (String) session.getAttribute("hpcUserToken");
            if (HpcClientUtil.deleteCollection(authToken, collectionURL, collectionPath,
                                                sslCertPath, sslCertPassword)) {
                resultNav = "redirect:/browse";
            } else {
                model.addAttribute("message",
                  "Failed to delete collection for unknown reason.");
                resultNav = "redirect:/stayput";
            }
        } catch (Exception e) {
            model.addAttribute("message",
              "Failed to delete collection.  Reason: " + e.getMessage());
            e.printStackTrace();
            resultNav = "redirect:/stayput";
        }
        return resultNav;
    }

}
