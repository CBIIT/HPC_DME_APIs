package gov.nih.nci.hpc.web.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@EnableAutoConfiguration
@RequestMapping("/settings")
public class HpcSettingsController extends AbstractHpcController {

	@RequestMapping(method = RequestMethod.GET)
    public String settings() {
        return "settings";
    }

}
