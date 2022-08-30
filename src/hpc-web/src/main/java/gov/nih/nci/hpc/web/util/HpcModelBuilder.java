package gov.nih.nci.hpc.web.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;

/**
 * DOC Model Builder. Gets DOC Model from cache if present, else goes 
 * to the server.
 *
 * @author menons2
 * @since 2019-08-16
 */

@Component
public class HpcModelBuilder {

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	@Cacheable(value="model", key="#hpcModelURL")
	public HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL,
		String hpcCertPath, String hpcCertPassword) {

		log.info("Getting DOCModels");
		   return HpcClientUtil.getDOCModel(token, hpcModelURL, hpcCertPath, hpcCertPassword);
	}


	@CachePut(value="model", key="#hpcModelURL")
	public HpcDataManagementModelDTO updateDOCModel(String token, String hpcModelURL,
		String hpcCertPath, String hpcCertPassword) {

		log.info("Upgating DOCModels");
		return HpcClientUtil.getDOCModel(token, hpcModelURL, hpcCertPath, hpcCertPassword);
	}


	@Cacheable(value="modelPermissions", key="#hpcPermissionsURL")
	public HpcPermsForCollectionsDTO getModelPermissions(HpcDataManagementModelDTO modelDTO, String token,
			String hpcPermissionsURL, String hpcCertPath, String hpcCertPassword) {

		log.info("Getting base path permissions");
		   return HpcClientUtil.getAllPermissionsForBasePaths(modelDTO, token,
				   hpcPermissionsURL, hpcCertPath, hpcCertPassword);
	}


	@CachePut(value="modelPermissions", key="#hpcPermissionsURL")
	public HpcPermsForCollectionsDTO updateModelPermissions(HpcDataManagementModelDTO modelDTO, String token, 
			String hpcPermissionsURL, String hpcCertPath, String hpcCertPassword) {

		log.info("Upgating base path permissions");
		return HpcClientUtil.getAllPermissionsForBasePaths(modelDTO, token,
				hpcPermissionsURL, hpcCertPath, hpcCertPassword);
	}

}
