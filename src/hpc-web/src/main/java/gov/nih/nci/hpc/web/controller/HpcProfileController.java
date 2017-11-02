package gov.nih.nci.hpc.web.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@EnableAutoConfiguration
@RequestMapping("/profile")
public class HpcProfileController extends AbstractHpcController {

	@RequestMapping(method = RequestMethod.GET)
    public String profile() {
        return "profile";
    }

}
