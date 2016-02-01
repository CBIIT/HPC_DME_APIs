package gov.nih.nci.hpc.cli.commands;

import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.irods.jargon.core.exception.JargonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCCollections;
import gov.nih.nci.hpc.cli.HPCDatafiles;
import gov.nih.nci.hpc.cli.HPCPermissions;
import gov.nih.nci.hpc.cli.IrodsClient;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

@Component
public class HPCCommands implements CommandMarker {

	private boolean hpcinitCommandExecuted = false;
	@Autowired
	private HpcConfigProperties configProperties;
	@Autowired
	private IrodsClient irodsClient;
	@Autowired
	private HPCCollections hpcCollections;
	@Autowired
	private HPCDatafiles hpcDatafiles;
	@Autowired
	private HPCPermissions hpcPermission;

	protected final Logger LOG = Logger.getLogger(getClass().getName());

	@CliAvailabilityIndicator({ "hpcput" })
	public boolean isHpcputAvailable() {
		if (hpcinitCommandExecuted) {
			return true;
		} else {
			return false;
		}
	}

	@CliAvailabilityIndicator({ "hpcmeta" })
	public boolean isHpcmetaAvailable() {
		if (hpcinitCommandExecuted) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * @CliCommand(value = "hpcget", help = "transfer file from irods ") public
	 * String hpcget(
	 * 
	 * @CliOption(key = { "file" }, mandatory = true, help =
	 * "Filename to transfer") final String filename,
	 * 
	 * @CliOption(key = { "location" }, mandatory = false, help =
	 * "Location of the file") final String location) { return "file = [" +
	 * filename + "] Location = [" + location + "]"; }
	 */
	@CliCommand(value = "hpcput", help = "Create/Add data to HPC Archive")
	public String hpcput(
			@CliOption(key = {
					"source" }, mandatory = false, help = "Source location for transfer") final String source,
			// @CliOption(key = { "collection"}, mandatory = true, help =
			// "Location/Collection of the file") final String collection,
			@CliOption(key = { "metadata" }, mandatory = true, help = "Metadata filename") final String metadata) {
		HPCDataObject hpcDataObject = new HPCDataObject(source, metadata);
		irodsClient.setHPCDataObject(hpcDataObject);
		try {
			irodsClient.setHPCAccount();
		} catch (NumberFormatException e) {
			// Handle this
			e.printStackTrace();
		} catch (JargonException e) {
			// Handle this
			e.printStackTrace();
		}
		return irodsClient.processDataObject();
	}

	@CliCommand(value = "hpcinit", help = "Initialize HPC configuration ")
	public String hpcinit(
			@CliOption(key = { "username" }, mandatory = true, help = "Username for storage") final String username,
			@CliOption(key = { "password" }, mandatory = true, help = "Password for storage") final String password) {
		hpcinitCommandExecuted = true;

		configProperties.setProperty("irods.username", username);
		String token = DatatypeConverter.printBase64Binary(password.getBytes());
		// ConfigurationDecoder.decode(password);
		configProperties.setProperty("irods.password", token);
		configProperties.save();

		return "HPC User  [" + configProperties.getProperty("irods.username") + "] initialized ";
	}

	@CliCommand(value = "hpcCollections", help = "Batch upload Collections to HPC Archive")
	public String hpcCollections(@CliOption(key = {
			"source" }, mandatory = false, help = "Source location for collections") final String source) {
		return hpcCollections.process(source);
	}

	@CliCommand(value = "hpcPermissions", help = "Batch assingment of permissions")
	public String hpcPermissions(@CliOption(key = {
			"source" }, mandatory = false, help = "Source location for permissions") final String source) {
		return hpcPermission.process(source);
	}

	@CliCommand(value = "hpcDatafiles", help = "Batch upload Collections to HPC Archive")
	public String hpcDatafiles(@CliOption(key = {
			"source" }, mandatory = false, help = "Source location for daatafiles") final String source) {
		return hpcDatafiles.process(source);
	}
	/*
	 * @CliCommand(value = "hpc init", help = "Initialize HPC configuration")
	 * public String einit(
	 * 
	 * @CliOption(key = { "message" }, mandatory = true, help =
	 * "Initialize HPC configuration") final MessageType message){ return
	 * "Initialize HPC configuration " + message; }
	 * 
	 * enum MessageType { Type1("type1"), Type2("type2"), Type3("type3");
	 * 
	 * private String type;
	 * 
	 * private MessageType(String type){ this.type = type; }
	 * 
	 * public String getType(){ return type; } }
	 */
}
