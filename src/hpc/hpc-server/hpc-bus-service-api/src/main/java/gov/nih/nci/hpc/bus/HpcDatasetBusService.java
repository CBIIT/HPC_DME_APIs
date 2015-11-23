/**
 * HpcDatasetBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddFilesDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAssociateFileProjectsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetUpdateFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFileDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Calendar;
import java.util.List;

/**
 * <p>
 * HPC Dataset Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDatasetBusService 
{         
    /**
     * Register Dataset.
     *
     * @param datasetRegistrationDTO The dataset registration DTO.
     * @return The registered dataset ID.
     * 
     * @throws HpcException
     */
    public String registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO) 
    		                     throws HpcException;
    
    /**
     * Add files to a registered dataset.
     *
     * @param addFilesDTO The add-files request DTO.
     * 
     * @throws HpcException
     */
    public void addFiles(HpcDatasetAddFilesDTO addFilesDTO) throws HpcException;
    
    /**
     * Associate projects with file in a registered dataset.
     *
     * @param associateFileProjectsDTO The projects to file association request DTO.
     * 
     * @throws HpcException
     */
    public void associateProjects(
    		             HpcDatasetAssociateFileProjectsDTO associateFileProjectsDTO) 
    		             throws HpcException;
    
    /**
     * Add metadata items to a file primary metadata in a registered dataset.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     * @return The updated primary metadata DTO.
     * @throws HpcException
     */
    public HpcFilePrimaryMetadataDTO addPrimaryMetadataItems(
    		             HpcDatasetAddMetadataItemsDTO addMetadataItemsDTO) 
    		             throws HpcException;
    
    /**
     * Update file primary metadata in a registered dataset.
     *
     * @param updateMetadataDTO The update-metadata request DTO.
     * @return The updated primary metadata DTO.
     * @throws HpcException
     */
    public HpcFilePrimaryMetadataDTO updatePrimaryMetadata(
    		             HpcDatasetUpdateFilePrimaryMetadataDTO updateMetadataDTO) 
    		             throws HpcException;
    
    /**
     * Get a dataset by its ID.
     *
     * @param id The dataset id.
     * @param skipDataTransferStatusUpdate If set to true, the service will not poll
     *                                     Data Transfer for an updated status on transfer
     *                                     requests in-flight. 
     * @return The dataset DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetDTO getDataset(String id, 
    		                        boolean skipDataTransferStatusUpdate) 
    		                       throws HpcException;
    
    /**
     * Get a file by its ID.
     *
     * @param id The file id.
     * @return The file DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcFileDTO getFile(String id) throws HpcException;
    
    /**
     * Get all datasets.
     *
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO getDatasets() throws HpcException;
    
    /**
     * Get datasets associated with a specific user(s).
     *
     * @param userIds The list of user ids to match.
     * @param association The association between the dataset and the user.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO 
              getDatasets(List<String> userIds, 
    		              HpcDatasetUserAssociation association) 
    		             throws HpcException;
    
    /**
     * Get datasets associated with a specific user's first and last name.
     *
     * @param nciUserId the user id.
     * @param association The association between the dataset and the user.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO 
              getDatasets(String firstName, String lastName, 
    		              HpcDatasetUserAssociation association) 
    		             throws HpcException;
    
    /**
     * Get datasets which has 'name' contained within its name.
     *
     * @param name Get datasets which 'name' is contained in their name.
     * @param regex If set to true, the 'name' will be queried as a regular expression. 
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO getDatasets(String name, boolean regex) 
    		                                  throws HpcException;
    
    /**
     * Get Datasets by file primary metadata.
     *
     * @param primaryMetadataDTO The metadata to query for. All datasets will be 
     *        returned if empty metadata is provided.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO getDatasets(
    		         HpcFilePrimaryMetadataDTO primaryMetadataDTO) 
    		         throws HpcException;

    /**
     * Get datasets with specific data transfer status.
     *
     * @param dataTransferStatus The data transfer status to query for.
     * @param uploadRequests Search the upload data transfer requests.
     * @param downloadRequests Search the download data transfer requests.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
	public HpcDatasetCollectionDTO getDatasets(HpcDataTransferStatus dataTransferStatus,
			                                   Boolean uploadRequests, 
			                                   Boolean downloadRequests) 
			                                  throws HpcException;
	
    /**
     * Get datasets by registration date range.
     *
     * @param from The from date.
     * @param to The to date.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
	public HpcDatasetCollectionDTO getDatasets(Calendar from, Calendar to) 
			                                  throws HpcException;
	
    /**
     * Get datasets associated with given projectId.
     *
     * @param projectId Get datasets associated with given projectId.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO getDatasetsByProjectId(String projectId) throws HpcException;   
}

 