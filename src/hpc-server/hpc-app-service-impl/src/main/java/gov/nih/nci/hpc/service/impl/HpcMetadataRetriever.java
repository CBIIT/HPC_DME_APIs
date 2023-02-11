/**
 * HpcDataManagementProxy.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Metadata and Data Object Retriever Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcMetadataRetriever {
	/**
	 * Get metadata of a collection.
	 *
	 * @param path The collection path.
	 * @return List of metadata entries.
	 * @throws HpcException on data management system failure.
	 */
	public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException;

	/**
	 * Get metadata of a data object.
	 *
	 * @param path The data object path.
	 * @return List of metadata entries.
	 * @throws HpcException on data management system failure.
	 */
	public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException;

	/**
	 * Get data objects by metadata query.
	 *
	 * @param metadataQueries The metadata entries to query for.
	 * @return List of data objects.
	 * @throws HpcException on search failure.
	 */
	public List<HpcDataObject> getDataObjects(List<HpcMetadataQuery> metadataQueries) throws HpcException;
}
