package gov.nih.nci.hpc.web.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

    @RequestMapping(method = RequestMethod.GET, path = "/confirmationView")
    public String presentConfirmationView() {
        return "deleteCollectionConfirmation";
    }

}
