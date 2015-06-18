/**
 * HpcDataRegistrationController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.web.model.HpcDatasetRegistration;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
/**
 * <p>
 * HPC DM Dataset registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/registerDataset")
public class HpcDataRegistrationController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataRegistration}")
    private String serviceURL;

  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
	  HpcDatasetRegistration hpcRegistration = new HpcDatasetRegistration();
	  model.addAttribute("hpcRegistration", hpcRegistration);
      return "datasetRegistration";
  }

  /*
	if(dataset.getName() == null || dataset.getType() == null ||
	    	   dataset.getLocation() == null ||
	    	   dataset.getLocation().getFacility() == null ||
	    	   dataset.getLocation().getEndpoint() == null ||
	    	   dataset.getLocation().getDataTransfer() == null)
	 */
  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid @ModelAttribute("hpcRegistration")  HpcDatasetRegistration registration, Model model) {
	  RestTemplate restTemplate = new RestTemplate();
	  String uri = "http://localhost:7737/hpc-server/registration";
	  HpcDataRegistrationInput input = new HpcDataRegistrationInput();
	  input.setInvestigatorName(registration.getInvestigatorName());
	  HpcDataset dataset = new HpcDataset();
	  /*
	  input.setProjectName(registration.getProjectName());
	  HpcDataset dataset = new HpcDataset();
	  dataset.setName("HPC Dataset");
	  input.setUsername("xyz");
	  input.setPassword("xyz");
	  HpcDatasetLocation target = new HpcDatasetLocation();
	  target.setEndpoint("nihfnlcr#gridftp1");
	  target.setFilePath(registration.getOriginDataLocation());
	  target.setDataTransfer(HpcDataTransfer.GLOBUS);
	  target.setFacility(HpcFacility.SHADY_GROVE);
	  dataset.setLocation(target);
	  dataset.setType(HpcDatasetType.RAW_SEQUENCING);
	  dataset.setSize(100000);
	  input.setType(HpcManagedDataType.EXPERIMENT);
	  HpcDatasetLocation source = new HpcDatasetLocation();
	  source.setEndpoint(registration.getOriginDataendpoint());
	  source.setFilePath(registration.getOriginDataLocation());
	  dataset.setSource(source);
	  */
	  List<HpcDataset> sets = input.getDatasets();
	  sets.add(dataset);
	  try
	  {
		  HttpEntity<String> response = restTemplate.postForEntity(uri,  input, String.class);
		  String resultString = response.getBody();
		  HttpHeaders headers = response.getHeaders();
		  String location = headers.getLocation().toString();
		  String id = location.substring(location.lastIndexOf("/")+1);
		  registration.setId(id);
		  model.addAttribute("registrationStatus", true);
		  model.addAttribute("registration", registration);
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("registrationOutput", "Failed to register your request due to: "+e.getMessage());
	  }
	  registration.setId("12345");
	  model.addAttribute("registrationStatus", true);
	  model.addAttribute("registration", registration);
	  
	  return "result";
  }
}
