package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
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
@RequestMapping("/deleteCollection")
public class HpcDeleteCollectionController extends AbstractHpcController {

    @Value("${gov.nih.nci.hpc.server.collection}")
    private String serviceURL;

    /**
     * Controller action to present view that prompts user for confirmation
     * about deleting a collection.
     *
     * @param session The HTTP session object
     *
     * @return <code>String</code> indicating which view template to apply
     */
    @RequestMapping(method = RequestMethod.GET, path = "/confirmationView")
    public String presentConfirmationView() {
//      @RequestParam("collection") String collectionPath, HttpSession session) {
/*
        final String authToken = (String) session.getAttribute("hpcUserToken");
        HpcCollectionListDTO collection =
          HpcClientUtil.getCollection(
            authToken,
            serviceURL,
            collectionPath,
            true,
            false,
            sslCertPath,
            sslCertPassword);
        boolean emptyFlag =
          collection.getCollections().get(0).getSubCollections().isEmpty();
*/
        return "deleteCollectionConfirmation";
    }

}
