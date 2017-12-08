/**
 * HpcDataSearchBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataSearchService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * HPC Data Search Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public class HpcDataSearchBusServiceImpl implements HpcDataSearchBusService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Data Search Application Service instance.
  @Autowired private HpcDataSearchService dataSearchService = null;

  // Security Application Service instance.
  @Autowired private HpcSecurityService securityService = null;

  // Data Management Bus Service instance.
  @Autowired private HpcDataManagementBusService dataManagementBusService = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataSearchBusServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcDataSearchBusService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
      throws HpcException {
    // Input validation.
    if (compoundMetadataQueryDTO == null) {
      throw new HpcException("Null compound metadata query", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    boolean detailedResponse =
        compoundMetadataQueryDTO.getDetailedResponse() != null
            && compoundMetadataQueryDTO.getDetailedResponse();
    int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
    boolean totalCount =
        compoundMetadataQueryDTO.getTotalCount() != null
            && compoundMetadataQueryDTO.getTotalCount();

    // Execute the query and package the results in a DTO.
    List<String> collectionPaths =
        dataSearchService.getCollectionPaths(compoundMetadataQueryDTO.getCompoundQuery(), page);
    HpcCollectionListDTO collectionsDTO = toCollectionListDTO(collectionPaths, detailedResponse);

    // Set page, limit and total count.
    collectionsDTO.setPage(page);
    int limit = dataSearchService.getSearchResultsPageSize();
    collectionsDTO.setLimit(limit);

    if (totalCount) {
      int count = collectionPaths.size();
      collectionsDTO.setTotalCount(
          (page == 1 && count < limit)
              ? count
              : dataSearchService.getCollectionCount(compoundMetadataQueryDTO.getCompoundQuery()));
    }

    return collectionsDTO;
  }

  @Override
  public HpcCollectionListDTO getCollections(
      String queryName, Boolean detailedResponse, Integer page, Boolean totalCount)
      throws HpcException {
    return getCollections(
        toCompoundMetadataQueryDTO(queryName, detailedResponse, page, totalCount));
  }

  @Override
  public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
      throws HpcException {
    // Input validation.
    if (compoundMetadataQueryDTO == null) {
      throw new HpcException("Null compound metadata query", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    boolean detailedResponse =
        compoundMetadataQueryDTO.getDetailedResponse() != null
            && compoundMetadataQueryDTO.getDetailedResponse();
    int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
    boolean totalCount =
        compoundMetadataQueryDTO.getTotalCount() != null
            && compoundMetadataQueryDTO.getTotalCount();

    // Execute the query and package the results into a DTO.
    List<String> dataObjectPaths =
        dataSearchService.getDataObjectPaths(compoundMetadataQueryDTO.getCompoundQuery(), page);
    HpcDataObjectListDTO dataObjectsDTO =
        toDataObjectListDTO(dataObjectPaths, detailedResponse);

    // Set page, limit and total count.
    dataObjectsDTO.setPage(page);
    int limit = dataSearchService.getSearchResultsPageSize();
    dataObjectsDTO.setLimit(limit);

    if (totalCount) {
      int count = dataObjectPaths.size();
      dataObjectsDTO.setTotalCount(
          (page == 1 && count < limit)
              ? count
              : dataSearchService.getDataObjectCount(compoundMetadataQueryDTO.getCompoundQuery()));
    }

    return dataObjectsDTO;
  }

  @Override
  public HpcDataObjectListDTO getDataObjects(
      String queryName, Boolean detailedResponse, Integer page, Boolean totalCount)
      throws HpcException {
    return getDataObjects(
        toCompoundMetadataQueryDTO(queryName, detailedResponse, page, totalCount));
  }

  @Override
  public void addQuery(String queryName, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(queryName) || compoundMetadataQueryDTO == null) {
      throw new HpcException(
          "Null or empty queryName / compoundMetadataQueryDTO", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the user-id of this request invoker.
    String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();

    if (dataSearchService.getQuery(nciUserId, queryName) != null) {
      throw new HpcException(
          "Query name already exists: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = new HpcNamedCompoundMetadataQuery();
    namedCompoundMetadataQuery.setName(queryName);
    namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
    namedCompoundMetadataQuery.setCreated(Calendar.getInstance());
    namedCompoundMetadataQuery.setDetailedResponse(
        compoundMetadataQueryDTO.getDetailedResponse() != null
            ? compoundMetadataQueryDTO.getDetailedResponse()
            : false);
    namedCompoundMetadataQuery.setTotalCount(
        compoundMetadataQueryDTO.getTotalCount() != null
            ? compoundMetadataQueryDTO.getTotalCount()
            : false);
    namedCompoundMetadataQuery.setCompoundQueryType(
        compoundMetadataQueryDTO.getCompoundQueryType());

    // Save the query.
    dataSearchService.saveQuery(nciUserId, namedCompoundMetadataQuery);
  }

  @Override
  public void updateQuery(String queryName, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(queryName) || compoundMetadataQueryDTO == null) {
      throw new HpcException(
          "Null or empty queryName / compoundMetadataQueryDTO", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the user-id of this request invoker.
    String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();

    // Get the query.
    HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery =
        dataSearchService.getQuery(nciUserId, queryName);
    if (namedCompoundMetadataQuery == null) {
      throw new HpcException(
          "Query name doesn't exist: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Update the query.
    if (compoundMetadataQueryDTO.getCompoundQuery() != null) {
      namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
    }
    if (compoundMetadataQueryDTO.getDetailedResponse() != null) {
      namedCompoundMetadataQuery.setDetailedResponse(
          compoundMetadataQueryDTO.getDetailedResponse());
    }
    if (compoundMetadataQueryDTO.getTotalCount() != null) {
      namedCompoundMetadataQuery.setTotalCount(compoundMetadataQueryDTO.getTotalCount());
    }
    if (compoundMetadataQueryDTO.getCompoundQueryType() != null) {
      namedCompoundMetadataQuery.setCompoundQueryType(
          compoundMetadataQueryDTO.getCompoundQueryType());
    }

    // Save the query.
    dataSearchService.saveQuery(nciUserId, namedCompoundMetadataQuery);
  }

  @Override
  public void deleteQuery(String queryName) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(queryName)) {
      throw new HpcException("Null or empty query name", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Delete the query.
    dataSearchService.deleteQuery(
        securityService.getRequestInvoker().getNciAccount().getUserId(), queryName);
  }

  @Override
  public HpcNamedCompoundMetadataQueryDTO getQuery(String queryName) throws HpcException {
    HpcNamedCompoundMetadataQueryDTO queryDTO = new HpcNamedCompoundMetadataQueryDTO();
    queryDTO.setNamedCompoundQuery(
        dataSearchService.getQuery(
            securityService.getRequestInvoker().getNciAccount().getUserId(), queryName));

    return queryDTO;
  }

  @Override
  public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException {
    HpcNamedCompoundMetadataQueryListDTO queryList = new HpcNamedCompoundMetadataQueryListDTO();
    queryList
        .getNamedCompoundQueries()
        .addAll(
            dataSearchService.getQueries(
                securityService.getRequestInvoker().getNciAccount().getUserId()));

    return queryList;
  }

  @Override
  public HpcMetadataAttributesListDTO getMetadataAttributes(String levelLabel) throws HpcException {
    HpcMetadataAttributesListDTO metadataAttributes = new HpcMetadataAttributesListDTO();
    metadataAttributes
        .getCollectionMetadataAttributes()
        .addAll(dataSearchService.getCollectionMetadataAttributes(levelLabel));
    metadataAttributes
        .getDataObjectMetadataAttributes()
        .addAll(dataSearchService.getDataObjectMetadataAttributes(levelLabel));

    return metadataAttributes;
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  /**
   * Construct a collection list DTO.
   *
   * @param collectionPaths A list of collection paths.
   * @param detailedResponse If set to true, return entity details (attributes + metadata).
   * @return A collection list DTO.
   * @throws HpcException on service failure.
   */
  private HpcCollectionListDTO toCollectionListDTO(
      List<String> collectionPaths, boolean detailedResponse) throws HpcException {
    HpcCollectionListDTO collectionsDTO = new HpcCollectionListDTO();

    if (detailedResponse) {
      for (String collectionPath : collectionPaths) {
        collectionsDTO
            .getCollections()
            .add(dataManagementBusService.getCollection(collectionPath, false));
      }
    } else {
      collectionsDTO.getCollectionPaths().addAll(collectionPaths);
    }

    return collectionsDTO;
  }

  /**
   * Construct a data object list DTO.
   *
   * @param dataObjectPaths A list of data object paths.
   * @param detailedResponse If set to true, return entity details (attributes + metadata).
   * @return A data object list DTO.
   * @throws HpcException on service failure.
   */
  private HpcDataObjectListDTO toDataObjectListDTO(
      List<String> dataObjectPaths, boolean detailedResponse) throws HpcException {
    HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
   
    if (detailedResponse) {
      for (String dataObjectPath : dataObjectPaths) {
        dataObjectsDTO.getDataObjects().add(dataManagementBusService.getDataObject(dataObjectPath));
      }
    } else {
      dataObjectsDTO.getDataObjectPaths().addAll(dataObjectPaths);
    }

    return dataObjectsDTO;
  }

  /**
   * Construct a HpcCompoundMetadataQueryDTO from a named query.
   *
   * @param queryName The user query.
   * @param detailedResponse The detailed response indicator.
   * @param page The requested results page.
   * @param totalCount The requested total count of results.
   * @return A compound metadata query DTO.
   * @throws HpcException If the user query was not found.
   */
  private HpcCompoundMetadataQueryDTO toCompoundMetadataQueryDTO(
      String queryName, Boolean detailedResponse, Integer page, Boolean totalCount)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(queryName)) {
      throw new HpcException("Null or empty query name", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the user query.
    HpcNamedCompoundMetadataQuery namedCompoundQuery =
        dataSearchService.getQuery(
            securityService.getRequestInvoker().getNciAccount().getUserId(), queryName);
    if (namedCompoundQuery == null || namedCompoundQuery.getCompoundQuery() == null) {
      throw new HpcException(
          "User query not found: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Construct the query DTO.
    HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
    compoundMetadataQueryDTO.setCompoundQuery(namedCompoundQuery.getCompoundQuery());
    compoundMetadataQueryDTO.setDetailedResponse(
        detailedResponse != null ? detailedResponse : namedCompoundQuery.getDetailedResponse());
    compoundMetadataQueryDTO.setPage(page != null ? page : 1);
    compoundMetadataQueryDTO.setTotalCount(
        totalCount != null ? totalCount : namedCompoundQuery.getTotalCount());

    return compoundMetadataQueryDTO;
  }
}
