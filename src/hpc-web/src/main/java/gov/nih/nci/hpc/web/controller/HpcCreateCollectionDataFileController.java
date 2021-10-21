/**
 * HpcCollectionController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDirectoryScanPathMap;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3ScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcAccessTokenType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * <p>
 * 
 * 
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCollectionController.java
 */

@EnableAutoConfiguration
public abstract class HpcCreateCollectionDataFileController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server.collection.acl.user}")
	private String collectionAclURL;

	public static final String GOOGLE_DRIVE_BULK_TYPE = "drive";
	public static final String GOOGLE_CLOUD_BULK_TYPE = "googleCloud";

	protected String login(Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		// User Session validation
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}
		return null;
	}

	protected void clearSessionAttrs(HttpSession session) {
		session.removeAttribute("datafilePath");
		session.removeAttribute("collection_type");
		session.removeAttribute("basePathSelected");
		session.removeAttribute("GlobusEndpoint");
		session.removeAttribute("GlobusEndpointPath");
		session.removeAttribute("GlobusEndpointFiles");
		session.removeAttribute("GlobusEndpointFolders");
		session.removeAttribute("parentCollection");
		session.removeAttribute("metadataEntries");
		session.removeAttribute("userMetadataEntries");
		session.removeAttribute("parent");
		session.removeAttribute("includeCriteria");
		session.removeAttribute("excludeCriteria");
		session.removeAttribute("dryRun");
		session.removeAttribute("bulkType");
		session.removeAttribute("fileIds");
		session.removeAttribute("folderIds");
		session.removeAttribute("accessToken");
		session.removeAttribute("accessTokenGoogleCloud");
		session.removeAttribute("authorized");
		session.removeAttribute("authorizedGC");
	}

	protected void populateBasePaths(HttpServletRequest request, HttpSession session, Model model, String path)
			throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");
		Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
		String userId = (String) session.getAttribute("hpcUserId");
		if (basePaths == null || basePaths.isEmpty()) {
			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}
			HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, userId, collectionAclURL, sslCertPath,
					sslCertPassword);
			basePaths = (Set<String>) session.getAttribute("basePaths");
		}

		String selectedBasePath = HpcClientUtil.getBasePath(request);
		if (selectedBasePath == null)
			selectedBasePath = (String) session.getAttribute("basePathSelected");
		else
			session.setAttribute("basePathSelected", selectedBasePath);
		model.addAttribute("basePathSelected", selectedBasePath);

		setCollectionPath(model, request, path);
		model.addAttribute("basePaths", basePaths);
	}

	protected void setInputParameters(Model model, HttpServletRequest request, HttpSession session, String path,
			String parent, String source, boolean refresh) {
		String endPoint = request.getParameter("endpoint_id");
		String globusPath = request.getParameter("path");
		String accessToken = (String) session.getAttribute("accessToken");
		String accessTokenGoogleCloud = (String) session.getAttribute("accessTokenGoogleCloud");
		List<String> fileNames = new ArrayList<String>();
		List<String> folderNames = new ArrayList<String>();
		List<String> fileIds = new ArrayList<String>();
        List<String> folderIds = new ArrayList<String>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String paramName = names.nextElement();
			if (paramName.startsWith("fileNames") && request.getParameterValues(paramName) != null)
				fileNames.addAll(Arrays.asList(request.getParameterValues(paramName)));
			else if (paramName.startsWith("folderNames") && request.getParameterValues(paramName) != null)
				folderNames.addAll(Arrays.asList(request.getParameterValues(paramName)));
			else if (paramName.startsWith("fileIds") && request.getParameterValues(paramName) != null)
                fileIds.addAll(Arrays.asList(request.getParameterValues(paramName)));
            else if (paramName.startsWith("folderIds") && request.getParameterValues(paramName) != null)
                folderIds.addAll(Arrays.asList(request.getParameterValues(paramName)));
			else if (paramName.startsWith("file"))
                fileNames.add(request.getParameter(paramName));
            else if (paramName.startsWith("folder"))
                folderNames.add(request.getParameter(paramName));
		}
		if (endPoint == null)
			endPoint = (String) session.getAttribute("GlobusEndpoint");
		else
			session.setAttribute("GlobusEndpoint", endPoint);

		model.addAttribute("endpoint_id", endPoint);

		if (refresh || globusPath == null)
			globusPath = (String) session.getAttribute("GlobusEndpointPath");
		else
			session.setAttribute("GlobusEndpointPath", globusPath);

		model.addAttribute("endpoint_path", globusPath);

		if (fileNames.isEmpty())
			fileNames = (List<String>) session.getAttribute("GlobusEndpointFiles");
		else
			session.setAttribute("GlobusEndpointFiles", fileNames);

		if (folderNames.isEmpty())
			folderNames = (List<String>) session.getAttribute("GlobusEndpointFolders");
		else
			session.setAttribute("GlobusEndpointFolders", folderNames);

		if (fileIds.isEmpty())
		  fileIds = (List<String>) session.getAttribute("fileIds");
        else
            session.setAttribute("fileIds", fileIds);

        if (folderIds.isEmpty())
          folderIds = (List<String>) session.getAttribute("folderIds");
        else
            session.setAttribute("folderIds", folderIds);
      
		if (endPoint != null)
			model.addAttribute("async", true);

		if (fileNames != null && !fileNames.isEmpty())
			model.addAttribute("fileNames", fileNames);

		if (folderNames != null && !folderNames.isEmpty())
			model.addAttribute("folderNames", folderNames);
		
		if (fileIds != null && !fileIds.isEmpty())
            model.addAttribute("fileIds", fileIds);

        if (folderIds != null && !folderIds.isEmpty())
            model.addAttribute("folderIds", folderIds);
        
        if (accessToken != null) {
            model.addAttribute("accessToken", accessToken);
            model.addAttribute("authorized", "true");
        }
		if (accessTokenGoogleCloud != null) {
            model.addAttribute("accessTokenGoogleCloud", accessTokenGoogleCloud);
            model.addAttribute("authorizedGC", "true");
        }
		setCriteria(model, request, session);
		if (source == null)
			model.addAttribute("source", session.getAttribute("source"));

	}

	protected void setCriteria(Model model, HttpServletRequest request, HttpSession session)
	{
		String includeCriteria = request.getParameter("includeCriteria");
		String excludeCriteria = request.getParameter("excludeCriteria");
		String dryRun = request.getParameter("dryrun");

		if (includeCriteria == null)
			includeCriteria = (String) session.getAttribute("includeCriteria");
		else
			session.setAttribute("includeCriteria", includeCriteria);

		model.addAttribute("includeCriteria", includeCriteria == null ? "":includeCriteria);
		
		if (excludeCriteria == null)
			excludeCriteria = (String) session.getAttribute("excludeCriteria");
		else
			session.setAttribute("excludeCriteria", excludeCriteria);

		model.addAttribute("excludeCriteria", excludeCriteria == null ? "":excludeCriteria);

		if (dryRun == null)
			dryRun = (String) session.getAttribute("dryRun");
		else
			session.setAttribute("dryRun", dryRun);

		model.addAttribute("dryRun", (dryRun != null && dryRun.equals("on")) ? true:false);		
	}
	
	protected HpcBulkDataObjectRegistrationRequestDTO constructBulkRequest(HttpServletRequest request,
			HttpSession session, String path) {
		HpcBulkDataObjectRegistrationRequestDTO dto = new HpcBulkDataObjectRegistrationRequestDTO();
		String datafilePath = (String) session.getAttribute("datafilePath");
		String globusEndpoint = (String) session.getAttribute("GlobusEndpoint");
		String selectedBasePath = (String) session.getAttribute("basePathSelected");
		String globusEndpointPath = (String) session.getAttribute("GlobusEndpointPath");
		String includeCriteria = (String) session.getAttribute("includeCriteria");
		String excludeCriteria = (String) session.getAttribute("excludeCriteria");
		String dryRun = (String) request.getParameter("dryrun");
		String criteriaType = (String)request.getParameter("criteriaType");
		List<String> globusEndpointFiles = (List<String>) session.getAttribute("GlobusEndpointFiles");
		List<String> globusEndpointFolders = (List<String>) session.getAttribute("GlobusEndpointFolders");

		if (globusEndpointFiles != null) {
			List<HpcDataObjectRegistrationItemDTO> files = new ArrayList<HpcDataObjectRegistrationItemDTO>();
			for (String fileName : globusEndpointFiles) {
				HpcDataObjectRegistrationItemDTO file = new HpcDataObjectRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				source.setFileId(globusEndpointPath + fileName);
				file.setSource(source);
				file.setCreateParentCollections(true);
				file.setPath(path + "/" + fileName);
				System.out.println(path + "/" + fileName);
				// file.getParentCollectionMetadataEntries().addAll(metadataEntries);
				files.add(file);
			}
			dto.getDataObjectRegistrationItems().addAll(files);
		}

		List<String> include = new ArrayList<String>();
		if(includeCriteria != null && !includeCriteria.isEmpty())
		{
			StringTokenizer tokens = new StringTokenizer(includeCriteria, "\r\n");
			while(tokens.hasMoreTokens())
				include.add(tokens.nextToken());
		}
		
		List<String> exclude = new ArrayList<String>();
		if(excludeCriteria != null && !excludeCriteria.isEmpty())
		{
			StringTokenizer tokens = new StringTokenizer(excludeCriteria, "\r\n");
			while(tokens.hasMoreTokens())
				exclude.add(tokens.nextToken());
		}

		if (globusEndpointFolders != null) {
			List<HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<HpcDirectoryScanRegistrationItemDTO>();
			for (String folderName : globusEndpointFolders) {
				HpcDirectoryScanRegistrationItemDTO folder = new HpcDirectoryScanRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				String fromPath = globusEndpointPath.endsWith("/") ?  globusEndpointPath + folderName : globusEndpointPath + "/" + folderName;
				String toPath = "/" + folderName;
				source.setFileId(fromPath);
				folder.setBasePath(datafilePath);
				folder.setScanDirectoryLocation(source);
				folders.add(folder);
				if(!fromPath.equals(toPath)) {
					HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
					pathDTO.setFromPath(fromPath);
					pathDTO.setToPath(toPath);
					folder.setPathMap(pathDTO);
				}
				if(criteriaType != null && criteriaType.equals("Simple"))
					folder.setPatternType(HpcPatternType.SIMPLE);
				else
					folder.setPatternType(HpcPatternType.REGEX);
				if(exclude.size() > 0)
					folder.getExcludePatterns().addAll(exclude);
				if(include.size() > 0)
					folder.getIncludePatterns().addAll(include);
			}
			dto.getDirectoryScanRegistrationItems().addAll(folders);
		}
		dto.setDryRun(dryRun != null && dryRun.equals("on"));
		return dto;
	}

	protected gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO constructV2BulkRequest(HttpServletRequest request,
			HttpSession session, String path) {
		gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO dto = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO();
		String datafilePath = (String) session.getAttribute("datafilePath");
		String globusEndpoint = (String) session.getAttribute("GlobusEndpoint");
		String globusEndpointPath = (String) session.getAttribute("GlobusEndpointPath");
		String includeCriteria = (String) session.getAttribute("includeCriteria");
		String excludeCriteria = (String) session.getAttribute("excludeCriteria");
		String dryRun = (String) request.getParameter("dryrun");
		String criteriaType = (String)request.getParameter("criteriaType");
		List<String> globusEndpointFiles = (List<String>) session.getAttribute("GlobusEndpointFiles");
		List<String> globusEndpointFolders = (List<String>) session.getAttribute("GlobusEndpointFolders");
		List<String> googleDriveFileIds = (List<String>) session.getAttribute("fileIds");
        List<String> googleDriveFolderIds = (List<String>) session.getAttribute("folderIds");
        String accessToken = (String) session.getAttribute("accessToken");
		String accessTokenGoogleCloud = (String) session.getAttribute("accessTokenGoogleCloud");
		
		String bulkType = (String)request.getParameter("bulkType");
		String bucketName = (String)request.getParameter("bucketName");
		String s3Path = (String)request.getParameter("s3Path");
		s3Path = (s3Path != null ? s3Path.trim() : null);
		String gcPath = (String)request.getParameter("gcPath");
		gcPath = (gcPath != null ? gcPath.trim() : null);
		String gcToPath = (String)request.getParameter("gcToPath");
		String accessKey = (String)request.getParameter("accessKey");
		String secretKey = (String)request.getParameter("secretKey");
		String region = (String)request.getParameter("region");
		String s3File = (String)request.getParameter("s3File");
		boolean isS3File = s3File != null && s3File.equals("on");	
		String gcFile = (String)request.getParameter("gcFile");
		boolean isGcFile = gcFile != null && gcFile.equals("on");	
		
		
		if (StringUtils.equals(bulkType, "globus") && globusEndpointFiles != null) {
			List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO>();
			for (String fileName : globusEndpointFiles) {
				gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				source.setFileId(globusEndpointPath + fileName);
				HpcUploadSource globusSource = new HpcUploadSource();
				globusSource.setSourceLocation(source);
				file.setGlobusUploadSource(globusSource);
				file.setCreateParentCollections(true);
				file.setPath(path + "/" + fileName);
				System.out.println(path + "/" + fileName);
				files.add(file);
			}
			dto.getDataObjectRegistrationItems().addAll(files);
		} else if (StringUtils.equals(bulkType, GOOGLE_DRIVE_BULK_TYPE) && googleDriveFileIds != null) {
			//Upload File(s) From Google Drive
            List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO>();
            for (String fileId : googleDriveFileIds) {
                gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();
                HpcFileLocation source = new HpcFileLocation();
                source.setFileContainerId("MyDrive");
                source.setFileId(fileId);
                Path filePath = Paths.get(globusEndpointFiles.get(googleDriveFileIds.indexOf(fileId)));
                String fileName = filePath.getFileName().toString();
                HpcStreamingUploadSource googleDriveSource = new HpcStreamingUploadSource();
                googleDriveSource.setSourceLocation(source);
                googleDriveSource.setAccessToken(accessToken);
                file.setGoogleDriveUploadSource(googleDriveSource);
                file.setCreateParentCollections(true);
                file.setPath(path + "/" + fileName);
                System.out.println(path + "/" + fileName);
                files.add(file);
            }
            dto.getDataObjectRegistrationItems().addAll(files);
        }

		List<String> include = new ArrayList<String>();
		if(includeCriteria != null && !includeCriteria.isEmpty())
		{
			StringTokenizer tokens = new StringTokenizer(includeCriteria, "\r\n");
			while(tokens.hasMoreTokens())
				include.add(tokens.nextToken());
		}
		
		List<String> exclude = new ArrayList<String>();
		if(excludeCriteria != null && !excludeCriteria.isEmpty())
		{
			StringTokenizer tokens = new StringTokenizer(excludeCriteria, "\r\n");
			while(tokens.hasMoreTokens())
				exclude.add(tokens.nextToken());
		}

		if (StringUtils.equals(bulkType, "globus") && globusEndpointFolders != null) {
			List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO>();
			for (String folderName : globusEndpointFolders) {
				gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO folder = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				String fromPath = globusEndpointPath.endsWith("/") ?  globusEndpointPath + folderName : globusEndpointPath + "/" + folderName;
				String toPath = "/" + folderName;
				source.setFileId(fromPath);
				folder.setBasePath(datafilePath);
				HpcScanDirectory globusDirectory = new HpcScanDirectory();
				globusDirectory.setDirectoryLocation(source);
				folder.setGlobusScanDirectory(globusDirectory);
				folders.add(folder);
				if(!fromPath.equals(toPath)) {
					HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
					pathDTO.setFromPath(fromPath);
					pathDTO.setToPath(toPath);
					folder.setPathMap(pathDTO);
				}
				if(criteriaType != null && criteriaType.equals("Simple"))
					folder.setPatternType(HpcPatternType.SIMPLE);
				else
					folder.setPatternType(HpcPatternType.REGEX);
				if(exclude.size() > 0)
					folder.getExcludePatterns().addAll(exclude);
				if(include.size() > 0)
					folder.getIncludePatterns().addAll(include);
			}
			dto.getDirectoryScanRegistrationItems().addAll(folders);
		}
		if (StringUtils.equals(bulkType, GOOGLE_DRIVE_BULK_TYPE) && googleDriveFolderIds != null) {
			// Upload Directory/Folder from Google Cloud Storage
            List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO>();
            for (String folderId : googleDriveFolderIds) {
                gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO folder = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
                HpcFileLocation source = new HpcFileLocation();
                source.setFileContainerId("MyDrive");
                Path folderPath = Paths.get(globusEndpointFolders.get(googleDriveFolderIds.indexOf(folderId)));
                String folderName = folderPath.getFileName().toString();
                String fromPath = "/" + folderPath.toString();
                String toPath = "/" + folderName;
                source.setFileId(folderId);
                folder.setBasePath(datafilePath);
                HpcGoogleScanDirectory googleDriveDirectory = new HpcGoogleScanDirectory();
                googleDriveDirectory.setDirectoryLocation(source);
                googleDriveDirectory.setAccessToken(accessToken);
                folder.setGoogleDriveScanDirectory(googleDriveDirectory);
                folders.add(folder);
                if(!fromPath.equals(toPath)) {
                    HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
                    pathDTO.setFromPath(fromPath);
                    pathDTO.setToPath(toPath);
                    folder.setPathMap(pathDTO);
                }
                if(criteriaType != null && criteriaType.equals("Simple"))
                    folder.setPatternType(HpcPatternType.SIMPLE);
                else
                    folder.setPatternType(HpcPatternType.REGEX);
                if(exclude.size() > 0)
                    folder.getExcludePatterns().addAll(exclude);
                if(include.size() > 0)
                    folder.getIncludePatterns().addAll(include);
            }
            dto.getDirectoryScanRegistrationItems().addAll(folders);
        }
	      if (StringUtils.equals(bulkType, GOOGLE_CLOUD_BULK_TYPE) && gcPath != null && isGcFile) {
			//Upload File From Google Cloud Storage
			List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO>();
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();
            HpcFileLocation source = new HpcFileLocation();
            source.setFileContainerId(bucketName);
            source.setFileId(gcPath);
			HpcStreamingUploadSource googleCloudSource = new HpcStreamingUploadSource();
			googleCloudSource.setSourceLocation(source);
			googleCloudSource.setAccessToken(accessTokenGoogleCloud);
			googleCloudSource.setAccessTokenType(HpcAccessTokenType.USER_ACCOUNT);
            file.setGoogleCloudStorageUploadSource(googleCloudSource);
			Path gcFilePath = Paths.get(gcPath);
			file.setPath(path + "/" + gcFilePath.getFileName());
            files.add(file);
			dto.getDataObjectRegistrationItems().addAll(files);
            //Gson gson = new GsonBuilder().setPrettyPrinting().create();
            //String registerBodyJson = gson.toJson(dto);
            //System.out.println("Final JSON Body");
            //System.out.println(registerBodyJson);
	    }
		if (StringUtils.equals(bulkType, GOOGLE_CLOUD_BULK_TYPE) && gcPath != null && !isGcFile) {
			// Upload Directory/Folder from Google Cloud Storage
	        List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO>();  
            gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
            HpcFileLocation source = new HpcFileLocation();
            source.setFileContainerId(bucketName);
            source.setFileId(gcPath);
            HpcGoogleScanDirectory googleCloudSource = new HpcGoogleScanDirectory();
            googleCloudSource.setDirectoryLocation(source);
            googleCloudSource.setAccessToken(accessTokenGoogleCloud);
            googleCloudSource.setAccessTokenType(HpcAccessTokenType.USER_ACCOUNT);
            file.setGoogleCloudStorageScanDirectory(googleCloudSource);
            file.setBasePath(datafilePath);
			//Pathmap
			HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
			pathDTO.setFromPath(gcPath);
			gcToPath = (gcToPath == null || gcToPath.isEmpty()) ? gcPath : gcToPath.trim();
			pathDTO.setToPath(gcToPath);
			file.setPathMap(pathDTO);

            if(criteriaType != null && criteriaType.equals("Simple"))
                file.setPatternType(HpcPatternType.SIMPLE);
            else
				file.setPatternType(HpcPatternType.REGEX);
            if(exclude.size() > 0)
				file.getExcludePatterns().addAll(exclude);
            if(include.size() > 0)
				file.getIncludePatterns().addAll(include);
        
			files.add(file);
            dto.getDirectoryScanRegistrationItems().addAll(files);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String registerBodyJson = gson.toJson(dto);
            System.out.println("Final JSON Body");
            System.out.println(registerBodyJson);
	    }  
		if (StringUtils.equals(bulkType, "s3") && s3Path != null && isS3File) {
			List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO>();
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(bucketName);
			source.setFileId(s3Path);
			HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
			HpcS3Account s3Account = new HpcS3Account();
			s3Account.setAccessKey(accessKey);
			s3Account.setSecretKey(secretKey);
			s3Account.setRegion(region);
			s3UploadSource.setAccount(s3Account);
			s3UploadSource.setSourceLocation(source);
			file.setS3UploadSource(s3UploadSource);
			file.setCreateParentCollections(true);
			Path s3FilePath = Paths.get(s3Path);
			file.setPath(path + "/" + s3FilePath.getFileName());
			System.out.println(path + "/" + s3FilePath.getFileName());
			files.add(file);
			dto.getDataObjectRegistrationItems().addAll(files);
		}
		else if (StringUtils.equals(bulkType, "s3") && s3Path != null) {
			List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO>();
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO folder = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(bucketName);
			source.setFileId(s3Path);
			folder.setBasePath(datafilePath);
			HpcS3ScanDirectory s3Directory = new HpcS3ScanDirectory();
			s3Directory.setDirectoryLocation(source);
			HpcS3Account s3Account = new HpcS3Account();
			s3Account.setAccessKey(accessKey);
			s3Account.setSecretKey(secretKey);
			s3Account.setRegion(region);
			s3Directory.setAccount(s3Account);
			folder.setS3ScanDirectory(s3Directory);
			folders.add(folder);
			if(criteriaType != null && criteriaType.equals("Simple"))
				folder.setPatternType(HpcPatternType.SIMPLE);
			else
				folder.setPatternType(HpcPatternType.REGEX);
			if(exclude.size() > 0)
				folder.getExcludePatterns().addAll(exclude);
			if(include.size() > 0)
				folder.getIncludePatterns().addAll(include);

			dto.getDirectoryScanRegistrationItems().addAll(folders);
		}
		
		dto.setDryRun(dryRun != null && dryRun.equals("on"));
		return dto;
	}
	
	protected List<HpcMetadataEntry> getMetadataEntries(HttpServletRequest request, HttpSession session, String path) {
		Enumeration<String> params = request.getParameterNames();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				if (attrValue.length == 0 || attrValue[0].isEmpty())
					continue;
				entry.setValue(attrValue[0]);
				entry.setAttribute(attrName);
				entry.setValue(attrValue[0]);
				metadataEntries.add(entry);
			} else if (paramName.startsWith("_addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("_addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("_addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty()) {
					entry.setAttribute(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty())
						entry.setValue(attrValue[0]);
					else
						throw new HpcWebException("Invalid value for metadata attribute " + attrName[0] + ": Value cannot be empty");
				} else if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
					throw new HpcWebException("Invalid metadata attribute name for value " + attrValue[0] + ": Name cannot be empty");
				} else {
					//If both attrName and attrValue are empty, then we just
					//ignore it and move to the next element
					continue;
				}

				metadataEntries.add(entry);
			}
		}
		return metadataEntries;
	}

	protected void populateCollectionTypes(HttpSession session, Model model, String basePath, String parent)
			throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}

		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
		String collectionType = null;
		Set<String> collectionTypesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

		HpcCollectionListDTO collections = null;
		HpcCollectionDTO collection = null;
		if (parent != null && !parent.isEmpty()) {
			collection = (HpcCollectionDTO) session.getAttribute("parentCollection");
			if (collection == null) {
				collections = HpcClientUtil.getCollection(authToken, serviceURL, parent, false, sslCertPath,
						sslCertPassword);
				if (collections != null && collections.getCollections() != null
						&& collections.getCollections().size() > 0) {
					collection = collections.getCollections().get(0);
				}
			}
		}
		if (basePathRules != null) {
			List<HpcMetadataValidationRule> rules = basePathRules.getCollectionMetadataValidationRules();
			// Parent name is given
			if (parent != null) {
				if (collection != null) {
					if (collection.getMetadataEntries() == null) {
						if (basePathRules.getDataHierarchy() != null)
							collectionTypesSet.add(basePathRules.getDataHierarchy().getCollectionType());
						else
							collectionTypesSet.add("Folder");
					} else {
						for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
							if (entry.getAttribute().equals("collection_type")) {
								collectionType = entry.getValue();
								break;
							}
						}
					}
				} else {
					// Collection not found
					// Populate all collection types
					for (String type : getCollectionTypes(rules))
						collectionTypesSet.add(type);
				}

				if (collectionType != null) {
					List<String> subCollections = getSubCollectionTypes(collectionType,
							basePathRules.getDataHierarchy());
					if ((subCollections == null || subCollections.isEmpty()) && !rules.isEmpty())
						throw new HpcWebException("Adding a sub collection is not allowed with: " + parent);
					for (String type : subCollections)
						collectionTypesSet.add(type);
				}
			}

			if (collectionType == null && collectionTypesSet.isEmpty()) {
				for (HpcMetadataValidationRule validationrule : rules) {
					if (validationrule.getMandatory() && validationrule.getAttribute().equals("collection_type")) {
						for (String type : validationrule.getValidValues())
							collectionTypesSet.add(type);
					}
				}
			}
		}
		if (collectionTypesSet.isEmpty())
			collectionTypesSet.add("Folder");
		model.addAttribute("collectionTypes", collectionTypesSet);
	}

	protected void checkParent(String parent, HttpSession session) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (parent != null && !parent.isEmpty()) {
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, serviceURL, parent, false,
					sslCertPath, sslCertPassword);
			if (collections != null && collections.getCollections() != null && collections.getCollections().size() > 0)
				session.setAttribute("parentCollection", collections.getCollections().get(0));
		}

	}

	private List<String> getCollectionTypes(List<HpcMetadataValidationRule> rules) {
		List<String> collectionTypesSet = new ArrayList<String>();
		for (HpcMetadataValidationRule rule : rules) {
			if (rule.getMandatory() && rule.getAttribute().equals("collection_type"))
				collectionTypesSet.addAll(rule.getValidValues());
		}
		return collectionTypesSet;
	}

	private List<String> getSubCollectionTypes(String collectionType, HpcDataHierarchy dataHierarchy) {
		List<String> types = new ArrayList<String>();
		if (dataHierarchy == null || dataHierarchy.getSubCollectionsHierarchies() == null)
			return types;
		if (dataHierarchy.getCollectionType().equals(collectionType)) {
			List<HpcDataHierarchy> subs = dataHierarchy.getSubCollectionsHierarchies();
			for (HpcDataHierarchy sub : subs)
				types.add(sub.getCollectionType());
		} else {
			List<HpcDataHierarchy> subs = dataHierarchy.getSubCollectionsHierarchies();
			for (HpcDataHierarchy sub : subs)
				if(types.isEmpty())
					types.addAll(getSubCollectionTypes(collectionType, sub));
		}

		return types;
	}
	
	protected boolean isDataObjectContainer(String collectionType, HpcDataHierarchy dataHierarchy) {
		if (dataHierarchy == null)
			return true;
		if (dataHierarchy.getCollectionType().equals(collectionType))
			return dataHierarchy.getIsDataObjectContainer();
		else {
			List<HpcDataHierarchy> subs = dataHierarchy.getSubCollectionsHierarchies();
			for (HpcDataHierarchy sub : subs) {
				if(isDataObjectContainer(collectionType, sub))
					return true;
			}
		}
		return false;
	}

	protected void populateFormAttributes(HttpServletRequest request, HttpSession session,
			Model model, String basePath, String collectionType, boolean refresh, boolean datafile) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		List<HpcMetadataValidationRule> rules = null;
		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
		if (basePathRules != null) {
			HpcDataHierarchy dataHierarchy = basePathRules.getDataHierarchy();
			if(dataHierarchy != null) {
				model.addAttribute("hasHierarchy", true);
			}
			if (datafile) {
				rules = basePathRules.getDataObjectMetadataValidationRules();
			}
			else
				rules = basePathRules.getCollectionMetadataValidationRules();
		}

		HpcCollectionDTO collectionDTO = (HpcCollectionDTO) session.getAttribute("parentCollection");
		List<HpcMetadataAttrEntry> cachedEntries = (List<HpcMetadataAttrEntry>) session.getAttribute("metadataEntries");
		List<HpcMetadataAttrEntry> cachedUserEntries = (List<HpcMetadataAttrEntry>) session.getAttribute("userMetadataEntries");

		// For each collection type, get required attributes
		// Build list as type1:attribute1, type1:attribute2, type2:attribute2,
		// type2:attribute3

		// For each attribute, get valid values
		// Build list as type1:attribute1:value1, type1:attribute1:value2

		// For each attribute, get default value
		// Build list as type1:attribute1:defaultValue,
		// type2:attribute2:defaultValue
		List<HpcMetadataAttrEntry> metadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		List<HpcMetadataAttrEntry> userMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		List<String> attributeNames = new ArrayList<String>();
		if (rules != null && !rules.isEmpty()) {
			for (HpcMetadataValidationRule rule : rules) {
				if ((rule.getCollectionTypes().contains(collectionType) || rule.getCollectionTypes().isEmpty()) 
						&& !rule.getAttribute().equals("collection_type")) {
					HpcMetadataAttrEntry entry = new HpcMetadataAttrEntry();
					entry.setAttrName(rule.getAttribute());
					attributeNames.add(rule.getAttribute());
					entry.setAttrValue(
							getFormAttributeValue(request, "zAttrStr_" + rule.getAttribute(), cachedEntries, "zAttrStr_"));
					if (entry.getAttrValue() == null) {
						if (!refresh) {
							entry.setAttrValue(getCollectionAttrValue(collectionDTO, rule.getAttribute()));
						} else {
							entry.setAttrValue(rule.getDefaultValue());
						}
					}
					if (rule.getValidValues() != null && !rule.getValidValues().isEmpty()) {
						List<String> validValues = new ArrayList<String>();
						for (String value : rule.getValidValues())
							validValues.add(value);
						entry.setValidValues(validValues);
					}
					entry.setDescription(rule.getDescription());
					entry.setMandatory(rule.getMandatory());
					metadataEntries.add(entry);
				}
			}
		}

		// Handle custom attributes. If refresh, ignore them
		if (!refresh) {
			Enumeration<String> params = request.getParameterNames();
			while (params.hasMoreElements()) {
				String paramName = params.nextElement();
				if (paramName.startsWith("_addAttrName")) {
					HpcMetadataAttrEntry entry = new HpcMetadataAttrEntry();
					String[] attrName = request.getParameterValues(paramName);
					String attrId = paramName.substring("_addAttrName".length());
					String attrValue = getFormAttributeValue(request, "_addAttrValue" + attrId, cachedEntries, "_addAttrValue"); 
					if (attrName.length > 0 && !attrName[0].isEmpty())
						entry.setAttrName(attrName[0]);
					entry.setAttrValue(attrValue);
					userMetadataEntries.add(entry);
				}
			}
		}

		if (!attributeNames.isEmpty()) {
			model.addAttribute("attributeNames", attributeNames);
		}
		if (collectionType != null && !collectionType.isEmpty()) {
			model.addAttribute("collection_type", collectionType);
		}
		else {
			model.addAttribute("collection_type", "_select_null");
		}

		model.addAttribute("basePath", basePath);
		
		session.setAttribute("metadataEntries", metadataEntries);
		model.addAttribute("metadataEntries", metadataEntries);
		
		session.setAttribute("userMetadataEntries", userMetadataEntries);
		model.addAttribute("userMetadataEntries", userMetadataEntries);
		
		
		String criteriaType = (String)request.getParameter("criteriaType");
		model.addAttribute("criteriaType", criteriaType);
	}

	protected List<HpcMetadataAttrEntry> mergeMatadataEntries(List<HpcMetadataAttrEntry> savedMetadataEntries,
			List<HpcMetadataAttrEntry> userMetadataEntries) {
		List<HpcMetadataAttrEntry> mergedMetadataEntries = new ArrayList<>();
		for (HpcMetadataAttrEntry savedEntry: savedMetadataEntries) {
			for(HpcMetadataAttrEntry configEntry: userMetadataEntries) {
				if(savedEntry.getAttrName().equals(configEntry.getAttrName())) {
					savedEntry.setMandatory(configEntry.isMandatory());
					savedEntry.setValidValues(configEntry.getValidValues());
					savedEntry.setDescription(configEntry.getDescription());
					break;
				}
			}
			if(!savedEntry.isEncrypted())
				mergedMetadataEntries.add(savedEntry);
		}
		return mergedMetadataEntries;
	}
	
	private String getCollectionAttrValue(HpcCollectionDTO collectionDTO, String attrName) {
		if (collectionDTO == null || collectionDTO.getMetadataEntries() == null
				|| collectionDTO.getMetadataEntries().getSelfMetadataEntries() == null)
			return null;

		for (HpcMetadataEntry entry : collectionDTO.getMetadataEntries().getSelfMetadataEntries()) {
			if (entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		return null;
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName,
			List<HpcMetadataAttrEntry> cachedEntries, String prefix) {
		String[] attrValue = request.getParameterValues(attributeName);
		if (attrValue != null)
			return attrValue[0];
		else {
			if (cachedEntries == null || cachedEntries.size() == 0)
				return null;
			for (HpcMetadataAttrEntry entry : cachedEntries) {
				if (attributeName.equals(prefix + entry.getAttrName()))
					return entry.getAttrValue();
			}
		}
		return null;
	}

	private void setCollectionPath(Model model, HttpServletRequest request, String parentPath) {
		String path = request.getParameter("path");
		if (path != null && !path.isEmpty()) {
			model.addAttribute("collectionPath", request.getParameter("path"));
			return;
		}

		if (parentPath != null && !parentPath.isEmpty())
			model.addAttribute("collectionPath", parentPath);
			
	}

	protected HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session,
			String path, HpcCollectionModel hpcCollection) throws HpcWebException {
		Enumeration<String> params = request.getParameterNames();
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		List<HpcMetadataAttrEntry> selfMetadataEntries = new ArrayList<>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				if (attrValue.length == 0 || attrValue[0].isEmpty())
					continue;
				entry.setValue(attrValue[0]);
				entry.setAttribute(attrName);
				metadataEntries.add(entry);
				attrEntry.setAttrName(attrName);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
			} else if (paramName.startsWith("_addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("_addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("_addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty()) {
					entry.setAttribute(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty())
						entry.setValue(attrValue[0]);
					else
						throw new HpcWebException("Invalid value for metadata attribute " + attrName[0] + ": Value cannot be empty");
				} else if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
					throw new HpcWebException("Invalid metadata attribute name for value " + attrValue[0] + ": Name cannot be empty");
				} else {
					//If both attrName and attrValue are empty, then we just
					//ignore it and move to the next element
					continue;
				}

				metadataEntries.add(entry);
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				attrEntry.setAttrName(attrName[0]);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
			}
		}
		if (hpcCollection != null)
			hpcCollection.setSelfMetadataEntries(selfMetadataEntries);
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}

}