/**
 * HpcEmailNotificationSenderImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * HPC Metadata retriever from iRODS
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcOracleMetadataRetrieverImpl implements HpcMetadataRetriever {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcMetadataRetriever Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException {
		return metadataDAO.getCollectionMetadata(dataManagementProxy.getAbsolutePath(path),
				HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername());
	}

	@Override
	public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException {
		return metadataDAO.getDataObjectMetadata(dataManagementProxy.getAbsolutePath(path),
				HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername());
	}

	@Override
	public List<HpcDataObject> getDataObjects(List<HpcMetadataQuery> metadataQueries) throws HpcException {
		List<HpcDataObject> dataObjects = metadataDAO.getDataObjects(metadataQueries,
				HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername());

		dataObjects.forEach(dataObject -> {
			dataObject.setAbsolutePath(dataManagementProxy.getRelativePath(dataObject.getAbsolutePath()));
			dataObject.setCollectionName(dataManagementProxy.getRelativePath(dataObject.getCollectionName()));
		});

		return dataObjects;
	}
}
