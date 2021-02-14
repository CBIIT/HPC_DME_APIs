/**
 * HpcDataTieringBusService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.datatiering.HpcBulkDataObjectTierRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Tiering Business Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public interface HpcDataTieringBusService {
	/**
	 * Tier a data object to Glacier
	 * 
	 * @param path		The data object path
	 * @throws HpcException on service failure.
	 */
	public void tierDataObject(String path) throws HpcException;

	/**
	 * Tier a collection to Glacier
	 * 
	 * @param path		The collection path
	 * @throws HpcException on service failure.
	 */
	public void tierCollection(String path) throws HpcException;

	/**
	 * Tier a list of data objects and/or collections to Glacier.
	 * 
	 * @param tierRequest	The request to tier data objects and/or collections
	 * @throws HpcException on service failure.
	 */
	public void tierDataObjectsOrCollections(HpcBulkDataObjectTierRequestDTO tierRequest) throws HpcException;
}
