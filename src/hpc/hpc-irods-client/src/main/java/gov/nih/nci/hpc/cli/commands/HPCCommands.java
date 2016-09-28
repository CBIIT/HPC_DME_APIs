package gov.nih.nci.hpc.cli.commands;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCBatchCollection;
import gov.nih.nci.hpc.cli.HPCBatchDatafile;
import gov.nih.nci.hpc.cli.HPCCmdCollection;
import gov.nih.nci.hpc.cli.HPCCmdDatafile;
import gov.nih.nci.hpc.cli.HPCPermissions;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;

@Component
public class HPCCommands implements CommandMarker {

	private boolean hpcinitCommandExecuted = false;
	@Autowired
	private HpcConfigProperties configProperties;
	@Autowired
	private HPCBatchCollection putCollections;
	@Autowired
	private HPCCmdCollection getCollections;
	@Autowired
	private HPCCmdDatafile getDatafiles;
	@Autowired
	private HPCBatchDatafile putDatafiles;
	@Autowired
	private HPCPermissions putPermissions;

	protected final Logger LOG = Logger.getLogger(getClass().getName());

	@CliCommand(value = "getCollection", help = "Get Collection from HPC Archive. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>")
	public String getCollection(@CliOption(key = {
			"path" }, mandatory = true, help = "Please provide collection path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
			"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
			"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format) {
		return getCollections.process("getCollection", path, outputfile, format, null);
	}
	 	
	@CliCommand(value = "getCollections", help = "Get Collections from HPC Archive. Usage: getCollections --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>")
	public String getCollections(@CliOption(key = {
			"criteria" }, mandatory = true, help = "Please provide metadata criteria. Usage: getCollections --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String criteria,
			@CliOption(key = {
			"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String outputfile,
			@CliOption(key = {
			"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format,
			@CliOption(key = {
			"detail" }, mandatory = false, help = "Please provide metadata criteria. Usage: getCollections --criteria <metadata criteria>  --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String detail) {
		return getCollections.process("getCollections", criteria, outputfile, format, detail);
	}

	@CliCommand(value = "getDatafile", help = "Get Collection from HPC Archive. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>")
	public String getDatafile(@CliOption(key = {
			"path" }, mandatory = true, help = "Please provide collection path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
			"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
			"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --format <data file path> --outputfile <output file full path> --format <json|csv>") final String format) {
		return getDatafiles.process("getDatafile", path, outputfile, format, null);
	}

	@CliCommand(value = "getDatafiles", help = "Get Data files from HPC Archive. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>")
	public String getDatafiles(@CliOption(key = {
			"criteria" }, mandatory = true, help = "Please provide metadata criteria. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String criteria,
			@CliOption(key = {
			"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String outputfile,
			@CliOption(key = {
			"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafiles --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format,
			@CliOption(key = {
			"detail" }, mandatory = false, help = "Please provide metadata criteria. Usage: getDatafiles --criteria <metadata criteria>  --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String detail) {
		return getDatafiles.process("getDatafiles", criteria, outputfile, format, detail);
	}

	@CliCommand(value = "putCollections", help = "Batch upload Collections to HPC Archive. Usage: putCollections --source <file path>")
	public String putCollections(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for collections. Usage: putCollections --source <file path>") final String source) {
		return putCollections.process(source);
	}

	@CliCommand(value = "putPermissions", help = "Batch assingment of permissions. Usage: putPermissions --source <file path>")
	public String putPermissions(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for permissions. Usage: putPermissions --source <file path>") final String source) {
		return putPermissions.process(source);
	}

	@CliCommand(value = "putDatafiles", help = "Batch upload data objects to HPC Archive. Usage: putDatafiles --source <file path>")
	public String putDatafiles(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for daatafiles. Usage: putDatafiles --source <file path>") final String source) {
		return putDatafiles.process(source);
	}
}
