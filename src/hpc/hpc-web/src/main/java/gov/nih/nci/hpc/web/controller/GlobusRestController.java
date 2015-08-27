package gov.nih.nci.hpc.web.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody; 
import javax.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;

@Controller
@EnableAutoConfiguration
public class GlobusRestController {

	@RequestMapping(value = "/getEndpoint", method = RequestMethod.GET)	
	public @ResponseBody String getUserEndpoint(@RequestParam("searchTerm") String searchTerm, HttpSession session) {
		JSONTransferAPIClient transferClient = null;
		Result endPointList = null;
		
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		
		GoauthClient cli = new GoauthClient("nexus.api.globusonline.org",
				"www.globusonline.org", user.getDataTransferAccount().getUsername(), user.getDataTransferAccount().getPassword());

				
		try {
			transferClient = getGOTransferClient(user.getDataTransferAccount().getUsername(), cli);
			Map<String, String> params = new HashMap<String, String>();
			params.put("filter", "canonical_name:~"+searchTerm+"*");
			endPointList = transferClient.getResult("endpoint_list", params);
		} catch (NexusClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (APIError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return endPointList == null ? "" : endPointList.document.toString();
	}

	@RequestMapping(value = "/getEndpointContents", method = RequestMethod.GET)
	public @ResponseBody String getUserEndpointContentList(
			@RequestParam("endpointName") String endpointName,
			HttpSession session) {

		JSONTransferAPIClient transferClient = null;
		Result endPointList = null;
		
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		
		GoauthClient cli = new GoauthClient("nexus.api.globusonline.org",
				"www.globusonline.org", user.getDataTransferAccount().getUsername(), user.getDataTransferAccount().getPassword());

		try {
			transferClient = getGOTransferClient(user.getDataTransferAccount().getUsername(), cli);
			Map<String, String> params = new HashMap<String, String>();

			transferClient.endpointAutoactivate(endpointName, params);
			endPointList = transferClient.endpointLs(endpointName, "");
		} catch (NexusClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (APIError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(endPointList.document.toString());
		return endPointList == null ? "" : endPointList.document.toString();
	}

	private JSONTransferAPIClient getGOTransferClient(String userGO,
			GoauthClient cli) throws NexusClientException, JSONException,
			KeyManagementException, NoSuchAlgorithmException {
		JSONTransferAPIClient transferClient;
		JSONObject accessTokenJSON;
		String accessToken;
		accessTokenJSON = cli.getClientOnlyAccessToken();
		accessToken = accessTokenJSON.getString("access_token");
		cli.validateAccessToken(accessToken);
		Authenticator authenticator = new GoauthAuthenticator(accessToken);
		transferClient = new JSONTransferAPIClient(userGO, null, null);
		transferClient.setAuthenticator(authenticator);
		return transferClient;
	}
}
