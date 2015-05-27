package gov.nih.nci.hpc.web.controller;

import java.util.List;

import javax.validation.Valid;

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.web.model.HpcRegistration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@EnableAutoConfiguration
@RequestMapping("/dataset")
public class HpcDataRegistrationController {
 
	  
  @RequestMapping(method = RequestMethod.GET)
  public List<HpcRegistration> findDatasets(){ 
    return null;
  }
	
  @RequestMapping(method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  public HpcRegistration register(@RequestBody  @Valid  HpcRegistration hpcDataset) {
	  hpcDataset.setInvestigatorName("name");
    return hpcDataset;
  }
}
