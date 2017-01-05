/**
 * HpcMetadataService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * HPC Metadata Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcMetadataService 
{  
    /**
     * Add metadata to a collection.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * @throws HpcException on service failure.
     */
    public void addMetadataToCollection(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Update a collection's metadata.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to update.
     * @throws HpcException on service failure.
     */
    public void updateCollectionMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException; 
    
    /**
     * Generate system metadata and attach to a collection.
     * System generated metadata is:
     * 		1. UUID.
     * 		2. Registrar user ID.
     * 		3. Registrar name.
     *      4. Registrar DOC.
     *
     * @param path The collection path.
     * @throws HpcException on service failure.
     */
    public void addSystemGeneratedMetadataToCollection(String path) 
    		                                          throws HpcException; 
    
    /**
     * Get the system generated metadata of a collection.
     *
     * @param path The collection path.
     * @return HpcSystemGeneratedMetadata The system generated metadata.
     * @throws HpcException on service failure.
     */
    public HpcSystemGeneratedMetadata 
              getCollectionSystemGeneratedMetadata(String path) throws HpcException; 
    
    /**
     * Get the system generated metadata object from a list of entries.
     *
     * @param systemGeneratedMetadataEntries The system generated metadata entries.
     * @return The system generated metadata.
     * @throws HpcException on service failure.
     */
    public HpcSystemGeneratedMetadata 
              toSystemGeneratedMetadata(List<HpcMetadataEntry> systemGeneratedMetadataEntries) 
            	                       throws HpcException; 
    
    /**
     * convert a list of metadata entries to Map&lt;attribute, value&gt;
     *
     * @param metadataEntries The list of metadata entries
     * @return A metadata attribute to value map.
     */
    public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries);
    
    /**
     * Get metadata of a collection.
     *
     * @param path The collection's path.
     * @return The collection's metadata entries.
     * @throws HpcException on service failure.
     */
    public HpcMetadataEntries getCollectionMetadataEntries(String path) throws HpcException;
    
    /**
     * Add metadata to a data object.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * @throws HpcException on service failure.
     */ 
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Generate system metadata and attach to the data object.
     * System generated metadata is:
     *      1. UUID.
     * 		2. Registrar user ID.
     * 		3. Registrar name.
     *      4. Registrar DOC.
     * 		5. Source location (file-container-id and file-id). (Optional)
     *      6. Archive location (file-container-id and file-id).
     *      7. Data Transfer Request ID. (Optional)
     *      8. Data Transfer Status.
     *      9. Data Transfer Type.
     *      10. Data Object File(s) size. (Optional)
     *      11. Metadata Origin
     *
     * @param path The data object path.
     * @param archiveLocation The physical file archive location.
     * @param sourceLocation (Optional) The source location of the file.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param checksum (Optional) The data checksum.
     * @param dataTransferStatus The data transfer upload status.
     * @param dataTransferType The data transfer type.
     * @param sourceSize (Optional) The data source size in bytes.
     * @param callerObjectId (Optional) The caller object ID.
     * @throws HpcException on service failure.
     */
    public void addSystemGeneratedMetadataToDataObject(String path, 
    		                                           HpcFileLocation archiveLocation,
    		                                           HpcFileLocation sourceLocation,
    		                                           String dataTransferRequestId,
    		                                           String checksum,
    		                                           HpcDataTransferUploadStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Long sourceSize, String callerObjectId) 
    		                                          throws HpcException; 
    
    /**
     * Get the system generated metadata of a data object.
     *
     * @param path The data object path.
     * @return The system generated metadata.
     * @throws HpcException on service failure.
     */
    public HpcSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException; 
    
    /**
     * Update system generated metadata of a data object.
     *
     * @param path The data object path.
     * @param archiveLocation (Optional) The physical file archive location.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param checksum (Optional) The data checksum.
     * @param dataTransferStatus (Optional) The data transfer upload status.
     * @param dataTransferType (Optional) The data transfer type.
     * @throws HpcException on service failure.
     */
    public void updateDataObjectSystemGeneratedMetadata(String path, 
    		                                            HpcFileLocation archiveLocation,
    		                                            String dataTransferRequestId,
    		                                            String checksum,
    		                                            HpcDataTransferUploadStatus dataTransferStatus,
    		                                            HpcDataTransferType dataTransferType) 
    		                                          throws HpcException; 
    
    /**
     * Update a data object's metadata.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to update.
     * @throws HpcException on service failure.
     */
    public void updateDataObjectMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException; 
    
    /**
     * Get metadata of a data object.
     *
     * @param path The data object's path.
     * @return HpcMetadataEntries The data object's metadata entries.
     * @throws HpcException on service failure.
     */
    public HpcMetadataEntries getDataObjectMetadataEntries(String path) throws HpcException;
    
    /**
     * Get collection metadata validation rules for a DOC.
     * 
     * @param doc The DOC.
     * @return A list of HpcMetadataValidationRule.
     * @throws HpcException on service failure.
     */
    public List<HpcMetadataValidationRule> 
           getCollectionMetadataValidationRules(String doc) throws HpcException;
    
    /**
     * Get data object metadata validation rules for a DOC.
     * 
     * @param doc The DOC.
     * @return A list of HpcMetadataValidationRule
     * @throws HpcException on service failure.
     */
    public List<HpcMetadataValidationRule> 
           getDataObjectMetadataValidationRules(String doc) throws HpcException;
    
    /**
     * Refresh all metadata materialized views.
     *
     * @throws HpcException on service failure.
     */
    public void refreshViews() throws HpcException;
}

 