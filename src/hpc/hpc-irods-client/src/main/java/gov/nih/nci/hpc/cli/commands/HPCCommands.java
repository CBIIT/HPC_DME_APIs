package gov.nih.nci.hpc.cli.commands;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.DatatypeConverter;

import gov.nih.nci.hpc.cli.HPCClient;
import gov.nih.nci.hpc.cli.IrodsClient;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

import org.irods.jargon.core.exception.JargonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

@Component
public class HPCCommands implements CommandMarker {
	
	private boolean hpcinitCommandExecuted = false;
	@Autowired
	private HpcConfigProperties configProperties;
	@Autowired
	private IrodsClient irodsClient;

	@CliAvailabilityIndicator({"hpcput"})
	public boolean isHpcputAvailable() {
			if (hpcinitCommandExecuted) {
				return true;
			} else {
				return false;
			}
	}
	
	@CliAvailabilityIndicator({"hpcmeta"})
	public boolean isHpcmetaAvailable() {
		if (hpcinitCommandExecuted) {
			return true;
		} else {
			return false;
		}
	}
	/*	
	@CliCommand(value = "hpcget", help = "transfer file from irods ")
	public String hpcget(
		@CliOption(key = { "file" }, mandatory = true, help = "Filename to transfer") final String filename,
		@CliOption(key = { "location" }, mandatory = false, help = "Location of the file") final String location) {		
		return "file = [" + filename + "] Location = [" + location + "]";
	}
	*/
	@CliCommand(value = "hpcput", help = "transfer file to irods")
	public String hpcput(
		@CliOption(key = { "file" }, mandatory = true, help = "Filename to transfer") final String filename,
		@CliOption(key = { "location"}, mandatory = false, help = "Location/Collection of the file") final String location,
		@CliOption(key = { "metadataFile" }, mandatory = true, help = "Metadata filename") final String metadataFile)
		//@CliOption(key = { "objectType" }, mandatory = true, help = "Object type") final String objectType)
		{
		/*
		HPCClient hpcClient = null;
		try {
			hpcClient = new IrodsClient();
		} catch (JargonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//HPCDataObject hpcDataObject = new HPCDataObject(filename,location,metadataFile,objectType);
		HPCDataObject hpcDataObject = new HPCDataObject(filename,location,metadataFile);
		irodsClient.setHPCDataObject(hpcDataObject);
		try {
			irodsClient.setHPCAccount();
		} catch (NumberFormatException e) {
			//Handle this
			e.printStackTrace();
		} catch (JargonException e) {
			//Handle this
			e.printStackTrace();
		}
		return  irodsClient.processDataObject();
	}
	
	@CliCommand(value = "hpcinit", help = "Initialize HPC configuration ")
	public String hpcinit(
		@CliOption(key = { "username" }, mandatory = true, help = "Username for storage") final String username,
		@CliOption(key = { "password" }, mandatory = false, help = "Password for storage") final String password) {		
		hpcinitCommandExecuted = true;

		configProperties.setProperty("irods.username", username);
		String token =DatatypeConverter.printBase64Binary(password.getBytes());
		//ConfigurationDecoder.decode(password);
		configProperties.setProperty("irods.password", token);
		configProperties.save();
		System.out.println("HOSTNAME::"+configProperties.getProperty("irods.default.host"));
 		
		return "username = [" + configProperties.getProperty("irods.username") + "] password = [" + new String(DatatypeConverter.parseBase64Binary(configProperties.getProperty("irods.password"))) + "]";
	}
		
	/*
	@CliCommand(value = "hpc init", help = "Initialize HPC configuration")
	public String einit(
		@CliOption(key = { "message" }, mandatory = true, help = "Initialize HPC configuration") final MessageType message){		
		return "Initialize HPC configuration " + message;
	}
	
	enum MessageType {		
		Type1("type1"),
		Type2("type2"),
		Type3("type3");
		
		private String type;
		
		private MessageType(String type){
			this.type = type;
		}
		
		public String getType(){
			return type;
		}
	}
	*/
}
