/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.commands;

import java.util.HashMap;
import java.util.Map;
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
import gov.nih.nci.hpc.cli.HPCCmdRegisterGlobusFile;
import gov.nih.nci.hpc.cli.HPCCmdRegisterLocalFile;
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
	private HPCCmdRegisterGlobusFile getGlobusDatafiles;
	@Autowired
	private HPCCmdRegisterLocalFile getLocalDatafiles;
	@Autowired
	private HPCBatchDatafile putDatafiles;
	@Autowired
	private HPCPermissions putPermissions;

	protected final Logger LOG = Logger.getLogger(getClass().getName());

	@CliCommand(value = "getCollection", help = "Get Collection from HPC Archive. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>")
	public String getCollection(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide collection path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getCollections.process("getCollection", criteriaMap, outputfile, format, null);
	}

	@CliCommand(value = "getCollections", help = "Get Collections from HPC Archive. Usage: getCollections --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>")
	public String getCollections(
			@CliOption(key = {
					"criteria" }, mandatory = true, help = "Please provide metadata criteria. Usage: getCollections --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String criteria,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format,
			@CliOption(key = {
					"detail" }, mandatory = false, help = "Please provide metadata criteria. Usage: getCollections --criteria <metadata criteria>  --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String detail) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(criteria, criteria);
		return getCollections.process("getCollections", criteriaMap, outputfile, format, detail);
	}

	@CliCommand(value = "getDatafile", help = "Get Collection from HPC Archive. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>")
	public String getDatafile(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide collection path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --format <data file path> --outputfile <output file full path> --format <json|csv>") final String format) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getDatafiles.process("getDatafile", criteriaMap, outputfile, format, null);
	}

	@CliCommand(value = "getDatafiles", help = "Get Data files from HPC Archive. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>")
	public String getDatafiles(
			@CliOption(key = {
					"criteria" }, mandatory = true, help = "Please provide metadata criteria. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String criteria,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafiles --criteria <metadata criteria> --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafiles --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format,
			@CliOption(key = {
					"detail" }, mandatory = false, help = "Please provide metadata criteria. Usage: getDatafiles --criteria <metadata criteria>  --outputfile <output file full path> --format <json|csv> --detail <yes|no>") final String detail) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(criteria, criteria);
		return getDatafiles.process("getDatafiles", criteriaMap, outputfile, format, detail);
	}

	@CliCommand(value = "registerFromGlobusPath", help = "Get Data files from HPC Archive. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusPath <Globus Endpoint Path> --destinationBasePath <Destination base path>")
	public String registerGlobusPath(
			@CliOption(key = {
					"globusEndpoint" }, mandatory = true, help = "Please provide Globus Endpoint. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusPath <Globus Endpoint Path> --destinationBasePath <Destination base path>") final String globusEndpoint,
			@CliOption(key = {
					"globusPath" }, mandatory = false, help = "Please provide Globus path. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusPath <Globus Endpoint Path> --destinationBasePath <Destination base path>") final String globusPath,
			@CliOption(key = {
					"destinationBasePath" }, mandatory = false, help = "Please provide destination base path. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusPath <Globus Endpoint Path> --destinationBasePath <Destination base path>") final String basePath) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("globusEndpoint", globusEndpoint);
		criteriaMap.put("globusPath", globusPath);
		criteriaMap.put("basePath", basePath);
		return getGlobusDatafiles.process("registerGlobusPath", criteriaMap, null, null, null);
	}

	@CliCommand(value = "registerFromFilePath", help = "Get Data files from HPC Archive. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <User Base Path>")
	public String registerFilePath(
			@CliOption(key = {
					"filePath" }, mandatory = true, help = "Please provide source file path. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String filePath,
			@CliOption(key = {
					"excludePatternFile" }, mandatory = false, help = "Please provide exclude pattern file. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String excludePattern,
			@CliOption(key = {
					"includePatternFile" }, mandatory = false, help = "Please provide include pattern file. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String includePattern,
			@CliOption(key = {
					"filePathBaseName" }, mandatory = false, help = "Please provide file base path name. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String filePathBaseName,
			@CliOption(key = {
					"destinationBasePath" }, mandatory = true, help = "Please provide destination base path. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String destinationBasePath,
			@CliOption(key = {
			"test" }, mandatory = false, help = "Test run to see the include and exclude files. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String test,
			@CliOption(key = {
			"confirm" }, mandatory = false, help = "Ask for confirmation before processing a file. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String confirm,
			@CliOption(key = {
			"metadata" }, mandatory = false, help = "Update metadata only. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false>  --confirm <true|false>--metadata <true|false>") final String metadata
			) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("filePath", filePath);
		criteriaMap.put("excludePatternFile", excludePattern);
		criteriaMap.put("includePatternFile", includePattern);
		criteriaMap.put("filePathBaseName", filePathBaseName);
		criteriaMap.put("destinationBasePath", destinationBasePath);
		criteriaMap.put("test", test);
		criteriaMap.put("confirm", confirm);
		criteriaMap.put("metadata", metadata);
		return getLocalDatafiles.process("registerFromFilePath", criteriaMap, null, null, null);
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
