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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.cli.HPCCmdCollection;
import gov.nih.nci.hpc.cli.HPCCmdDatafile;
import gov.nih.nci.hpc.cli.HPCCmdPutDatafile;
import gov.nih.nci.hpc.cli.HPCPermissions;
import gov.nih.nci.hpc.cli.csv.HPCBatchCollection;
import gov.nih.nci.hpc.cli.csv.HPCBatchDatafile;
import gov.nih.nci.hpc.cli.download.HPCBatchCollectionDownload;
import gov.nih.nci.hpc.cli.globus.HPCCmdRegisterGlobusFile;
import gov.nih.nci.hpc.cli.local.HPCBatchLocalfile;
import gov.nih.nci.hpc.cli.util.Constants;
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
	private HPCCmdPutDatafile putDatafile;
	@Autowired
	private HPCCmdRegisterGlobusFile getGlobusDatafiles;
//	@Autowired
//	private HPCCmdRegisterLocalFile getLocalDatafiles;
	@Autowired
	private HPCBatchDatafile putDatafiles;
	@Autowired
	private HPCPermissions putPermissions;
	@Autowired
	private HPCBatchLocalfile batchLocalFiles;
	@Autowired
	private HPCBatchCollectionDownload downloadCollection;

	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
    
	@CliCommand(value = "getCollection", help = "Get Collection from HPC Archive. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>")
	public String getCollection(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide collection path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --path <collection path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getCollection --format <collection path> --outputfile <output file full path> --format <json|csv>") final String format) {
	  logger.debug("getCollection");
	  Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getCollections.process("getCollection", criteriaMap, outputfile, format, "yes");
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
        logger.debug("getCollections");
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(criteria, criteria);
		return getCollections.process("getCollections", criteriaMap, outputfile, format, "yes");
	}
	
	
	@CliCommand(value = "deleteCollection", help = "Delete Collection from HPC Archive. Usage: deleteCollection --path <data file path --recursive <true|false>")
	public String deleteCollection(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide collection path. Usage: deleteCollection --path <data file path> --recursive <true|false>") final String path,
			@CliOption(key = {
			"recursive" }, mandatory = false, help = "Please provide recursion option. Usage: deleteCollection --path <data file path>  --recursive <true|false>") final String recursive) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getCollections.process("deleteCollection", criteriaMap, null, null, recursive);
	}

	@CliCommand(value = "getDatafile", help = "Get Collection from HPC Archive. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>")
	public String getDatafile(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide collection path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String path,
			@CliOption(key = {
					"outputfile" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --path <data file path> --outputfile <output file full path> --format <json|csv>") final String outputfile,
			@CliOption(key = {
					"format" }, mandatory = false, help = "Please provide outputfile path. Usage: getDatafile --format <data file path> --outputfile <output file full path> --format <json|csv>") final String format) {
	    logger.debug("getDatafile");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getDatafiles.process("getDatafile", criteriaMap, outputfile, format, "yes");
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
	    logger.debug("getCollection");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(criteria, criteria);
		return getDatafiles.process("getDatafiles", criteriaMap, outputfile, format, "yes");
	}
	
	@CliCommand(value = "putDatafile", help = "Register datafile to HPC Archive. Usage: putDatafile --sourceFilePath <Source file path> --destinationArchivePath <Destination file path> --metadataFile <Metadata JSON file> ")
	public String putDatafile(
			@CliOption(key = {
					"sourceFilePath" }, mandatory = true, help = "Please provide source file path. Usage: putDatafile --sourceFilePath <Source file path> --destinationArchivePath <Destination file path> --metadataFile <Metadata JSON file> ") final String sourcePath,
			@CliOption(key = {
					"destinationArchivePath" }, mandatory = true, help = "Please provide destination file path. Usage: putDatafile --sourceFilePath <Source file path> --destinationArchivePath <Destination file path> --metadataFile <Metadata JSON file>") final String destinationPath,
			@CliOption(key = {
					"metadataFile" }, mandatory = true, help = "Please provide metadata JSON file path. Usage: putDatafile --sourceFilePath <Source file path> --destinationArchivePath <Destination file path> --metadataFile <Metadata JSON file>") final String metadataFile) {
	    logger.debug("getDatafile");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("sourcePath", sourcePath);
		criteriaMap.put("destinationPath", destinationPath);
		criteriaMap.put("metadataFile", metadataFile);
		return putDatafile.process("putDatafile", criteriaMap, null, null, null);
	}
	
	@CliCommand(value = "deleteDatafile", help = "Delete data object from HPC Archive. Usage: deleteDatafile --path <data file path>")
	public String deleteDatafile(
			@CliOption(key = {
					"path" }, mandatory = true, help = "Please provide data file path. Usage: deleteDatafile --path <data file path>") final String path) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put(path, path);
		return getDatafiles.process("deleteDatafile", criteriaMap, null, null, null);
	}

	@CliCommand(value = "registerFromGlobusPath", help = "Register Data files from Globus endpoint with HPC Archive. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>")
	public String registerGlobusPath(
			@CliOption(key = {
					"globusEndpoint" }, mandatory = true, help = "Please provide Globus Endpoint. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String globusEndpoint,
			@CliOption(key = {
					"globusSourcePath" }, mandatory = true, help = "Please provide Globus source path. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String globusPath,
			@CliOption(key = {
			"excludePatternFile" }, mandatory = false, help = "Please provide exclude pattern file. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String excludePattern,
			@CliOption(key = {
			"includePatternFile" }, mandatory = false, help = "Please provide include pattern file. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String includePattern,
			@CliOption(key = {
			"patternType" }, mandatory = false, help = "Pattern Type. Valid values are 'Simple', 'RegEx'. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String patternType,
			@CliOption(key = {
			"dryRun" }, mandatory = false, help = "Dryrun to see the registration files. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String dryRun,
			@CliOption(key = {
					"destinationArchivePath" }, mandatory = true, help = "Please provide destination base path. Usage: registerFromGlobusPath --globusEndpoint <Globus Endpoint Name> --globusSourcePath <Globus Endpoint Path> --destinationArchivePath <Destination base path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --patternType <Simple|RegEx> --dryRun <true|false>") final String basePath) {
	    logger.debug("registerFromGlobusPath");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("globusEndpoint", globusEndpoint);
		criteriaMap.put("globusPath", globusPath);
		criteriaMap.put("basePath", basePath);
		criteriaMap.put("dryRun", dryRun);
		criteriaMap.put("patternType", patternType);
		criteriaMap.put("excludePatternFile", excludePattern);
		criteriaMap.put("includePatternFile", includePattern);
		return getGlobusDatafiles.process("registerGlobusPath", criteriaMap, null, null, null);
	}

	/*
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
			"directUpload" }, mandatory = false, help = "Ask for confirmation before processing a file. Usage: registerFromFilePath --filePath <Souce file path> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationBasePath <Destination base path> --test <true|false> --confirm <true|false> --metadata <true|false>") final String directUpload,
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
		criteriaMap.put("directUpload", directUpload);
		return getLocalDatafiles.process("registerFromFilePath", criteriaMap, null, null, null);
	}
	*/

	@CliCommand(value = "registerFromFilePath", help = "Register Data files with the HPC DME Archive from a local folder. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --test <true|false> --confirm <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>")
	public String registerFilePathS3(
			@CliOption(key = {
					"sourceFilePath" }, mandatory = true, help = "Please provide source file path. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String filePath,
			@CliOption(key = {
					"sourceFileList" }, mandatory = false, help = "Please provide source file list. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String fileList,
			@CliOption(key = {
					"excludePatternFile" }, mandatory = false, help = "Please provide exclude pattern file. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String excludePattern,
			@CliOption(key = {
					"includePatternFile" }, mandatory = false, help = "Please provide include pattern file. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String includePattern,
			@CliOption(key = {
					"filePathBaseName" }, mandatory = false, help = "Please provide file base path name. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String filePathBaseName,
			@CliOption(key = {
					"destinationArchivePath" }, mandatory = true, help = "Please provide destination base path. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String destinationBasePath,
			@CliOption(key = {
					"archiveType" }, mandatory = false, help = "Please provide archive type <S3 | POSIX>. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String archiveType,
			@CliOption(key = {
					"checksum" }, mandatory = false, help = "Do you want to verfiy checksum <true|false>. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String checksum,			
			@CliOption(key = {
					"dryRun" }, mandatory = false, help = "Dryrun run to see the include and exclude files. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String test,
			@CliOption(key = {
					"confirm" }, mandatory = false, help = "Ask for confirmation before processing a file. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String confirm,
			@CliOption(key = {
					"metadataOnly" }, mandatory = false, help = "Update metadata only. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --dryRun <true|false>  --archiveType<S3|POSIX> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String metadataOnly,
            @CliOption(key = {
                    "extractMetadata" }, mandatory = false, help = "Extract metadata from file. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --dryRun <true|false>  --archiveType<S3|POSIX> --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String extractMetadata,
			@CliOption(key = {
					"threads" }, mandatory = false, help = "Number of threads to process. Usage: registerFromFilePath --sourceFilePath <Souce file path> --sourceFileList <Source files list> --excludePatternFile <Patterns to exclude files> --includePatternFile <Patterns to include files> --filePathBaseName <Source file path Base name> --destinationArchivePath <Destination base path> --archiveType<S3|POSIX> --dryRun <true|false>  --confirm <true|false> --checksum <true|false> --metadataOnly <true|false> --extractMetadata <true|false> --threads <number>") final String threads
			) {
	    logger.debug("registerFromFilePath");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		if(fileList != null && (excludePattern != null || includePattern != null))
		{
			System.out.println("Invalid options. Specify sourceFileList without include and exclude criteria.");
			return Constants.CLI_2;
		}
		criteriaMap.put("filePath", filePath);
		criteriaMap.put("fileList", fileList);
		criteriaMap.put("excludePatternFile", excludePattern);
		criteriaMap.put("includePatternFile", includePattern);
		criteriaMap.put("filePathBaseName", filePathBaseName);
		criteriaMap.put("destinationBasePath", destinationBasePath);
		criteriaMap.put("test", test);
		criteriaMap.put("confirm", confirm);
		criteriaMap.put("archiveType", archiveType);
		criteriaMap.put("metadata", metadataOnly);
		criteriaMap.put("extractMetadata", extractMetadata);
		criteriaMap.put("checksum", checksum);
		criteriaMap.put("threads", threads);
		batchLocalFiles.setCriteria("registerFromFilePathS3", criteriaMap);
		return batchLocalFiles.process(null);
	}
	
	@CliCommand(value = "putCollections", help = "Batch upload Collections to HPC Archive. Usage: putCollections --source <file path>")
	public String putCollections(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for collections. Usage: putCollections --source <file path>") final String source) {
		return putCollections.process(source);
	}
/*
	@CliCommand(value = "getSignedS3URL", help = "Get signed S3 URL to upload data object")
	public String getSignedS3URL(@CliOption(key = {
			"expiration" }, mandatory = true, help = "Please provide expiration time in minutes --expiration <Signed URL expiration>") final String expiration,
			@CliOption(key = {
			"objectPath" }, mandatory = true, help = "Please provide object path --objectPath <object path>") final String path) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("expiration", expiration);
		criteriaMap.put("path", path);
		return signedURL.process("getSignedS3URL", criteriaMap, null, null, null);
	}


	@CliCommand(value = "uploadtoS3", help = "Upload data object to S3")
	public String uploadtoS3(
			@CliOption(key = {
			"filePath" }, mandatory = true, help = "Please provide file path --filePath <file path>") String filepath,
			@CliOption(key = {
			"signedURL" }, mandatory = true, help = "Please provide presigned URL --signedURL <Presigned URL>") String signedURL) {
		Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("filepath", filepath);
		criteriaMap.put("signedURL", signedURL);
		return uploadS3.process("uploadtoS3", criteriaMap, null, null, null);
	}
*/

	@CliCommand(value = "putPermissions", help = "Batch assingment of permissions. Usage: putPermissions --source <file path>")
	public String putPermissions(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for permissions. Usage: putPermissions --source <file path>") final String source) {
	    logger.debug("putPermissions");
	    return putPermissions.process(source);
	}

	@CliCommand(value = "putDatafiles", help = "Batch upload data objects to HPC Archive. Usage: putDatafiles --source <file path>")
	public String putDatafiles(@CliOption(key = {
			"source" }, mandatory = true, help = "Please provide file location for daatafiles. Usage: putDatafiles --source <file path>") final String source) {
	    logger.debug("putDatafiles");
	    return putDatafiles.process(source);
	}
	
	@CliCommand(value = "downloadCollection", help = "Download collection from the HPC DME Archive to a local folder. Usage: downloadCollection --sourceArchivePath <Source archive path> --destinationPath <Destination path> --threads <number>")
	public String downloadCollection(
			@CliOption(key = {
					"sourceArchivePath" }, mandatory = true, help = "Please provide source archive path. Usage: downloadCollection --sourceArchivePath <Source archive path> --destinationPath <Destination path> --threads <number>") final String sourceArchivePath,
			@CliOption(key = {
					"destinationPath" }, mandatory = true, help = "Please provide source archive path. Usage: downloadCollection --sourceArchivePath <Source archive path> --destinationPath <Destination path> --threads <number>") final String destinationPath,
			@CliOption(key = {
					"threads" }, mandatory = false, help = "Number of threads to process. Usage: downloadCollection --sourceArchivePath <Source archive path> --destinationPath <Destination path> --threads <number>") final String threads
			) {
	    logger.debug("downloadCollection");
	    Map<String, String> criteriaMap = new HashMap<String, String>();
		criteriaMap.put("sourceArchivePath", sourceArchivePath);
		criteriaMap.put("destinationPath", destinationPath);
		criteriaMap.put("threads", threads);
		downloadCollection.setCriteria(criteriaMap);
		return downloadCollection.process(null);
	}
}
