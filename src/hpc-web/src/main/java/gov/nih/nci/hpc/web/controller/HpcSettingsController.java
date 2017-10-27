package gov.nih.nci.hpc.web.controller;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@EnableAutoConfiguration
@RequestMapping("/settings")
public class HpcSettingsController {

	@RequestMapping(method = RequestMethod.GET)
    public String settings() {
        return "settings";
    }

}
