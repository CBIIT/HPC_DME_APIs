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
import gov.nih.nci.hpc.domain.catalog.HpcCatalog;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntryForCollection;
import gov.nih.nci.hpc.dto.catalog.HpcCatalogRequestDTO;
import gov.nih.nci.hpc.dto.catalog.HpcCatalogsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcCatalogService;
import gov.nih.nci.hpc.service.HpcDataSearchService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * HPC Data Search Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataSearchBusServiceImpl implements HpcDataSearchBusService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Data Search Application Service instance.
	@Autowired
	private HpcDataSearchService dataSearchService = null;

	// Catalog Application Service instance.
	@Autowired
	private HpcCatalogService catalogService = null;

	// Security Application Service instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// Data Management Bus Service instance.
	@Autowired
	private HpcDataManagementBusService dataManagementBusService = null;

	// The collection download task executor.
	@Autowired
	@Qualifier("hpcGetAllDataObjectsExecutorService")
	ExecutorService hpcGetAllDataObjectsExecutorService = null;
			
	@Value("${hpc.bus.getAllDataObjectsDefaultPageSize}")
	private int getAllDataObjectsDefaultPageSize = 0;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
		
	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataSearchBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataSearchBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
			throws HpcException {
		// Input validation.
		if (compoundMetadataQueryDTO == null) {
			throw new HpcException("Null compound metadata query", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null
				&& compoundMetadataQueryDTO.getDetailedResponse();
		int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
		int pageSize = compoundMetadataQueryDTO.getPageSize() != null ? compoundMetadataQueryDTO.getPageSize() : 0;
		boolean totalCount = compoundMetadataQueryDTO.getTotalCount() != null
				&& compoundMetadataQueryDTO.getTotalCount();

		// Execute the query and package the results in a DTO.
		int count = 0;
		HpcCollectionListDTO collectionsDTO = null;
		if (!detailedResponse) {
			List<String> collectionPaths = dataSearchService
					.getCollectionPaths(compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			collectionsDTO = toCollectionListDTO(collectionPaths, detailedResponse);
			count = collectionPaths.size();
		} else {
			List<HpcSearchMetadataEntryForCollection> collectionPaths = dataSearchService
					.getDetailedCollectionPaths(compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			collectionsDTO = toDetailedCollectionListDTO(collectionPaths);
			count = collectionsDTO.getCollections().size();
		}

		// Set page, limit and total count.
		collectionsDTO.setPage(page);
		int limit = dataSearchService.getSearchResultsPageSize(pageSize);
		collectionsDTO.setLimit(limit);

		if (totalCount) {
			collectionsDTO.setTotalCount((page == 1 && count < limit) ? count
					: dataSearchService.getCollectionCount(compoundMetadataQueryDTO.getCompoundQuery()));
		}

		return collectionsDTO;
	}

	@Override
	public HpcCollectionListDTO getCollections(String queryName, Boolean detailedResponse, Integer page,
			Integer pageSize, Boolean totalCount) throws HpcException {
		return getCollections(toCompoundMetadataQueryDTO(queryName, detailedResponse, page, pageSize, totalCount));
	}

	@Override
	public HpcDataObjectListDTO getDataObjects(String path, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
			throws HpcException {
		// Input validation.
		if (compoundMetadataQueryDTO == null) {
			throw new HpcException("Null compound metadata query", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null
				&& compoundMetadataQueryDTO.getDetailedResponse();
		int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
		int pageSize = compoundMetadataQueryDTO.getPageSize() != null ? compoundMetadataQueryDTO.getPageSize() : 0;
		boolean totalCount = compoundMetadataQueryDTO.getTotalCount() != null
				&& compoundMetadataQueryDTO.getTotalCount();

		// Execute the query and package the results into a DTO.
		int count = 0;
		HpcDataObjectListDTO dataObjectsDTO = null;
		if (!detailedResponse) {
			List<String> dataObjectPaths = dataSearchService.getDataObjectPaths(path,
					compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			dataObjectsDTO = toDataObjectListDTO(dataObjectPaths, detailedResponse);
			count = dataObjectPaths.size();
		} else {
			List<HpcSearchMetadataEntry> dataObjectPaths = dataSearchService.getDetailedDataObjectPaths(path,
					compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			dataObjectsDTO = toDetailedDataObjectListDTO(dataObjectPaths);
			count = dataObjectsDTO.getDataObjects().size();
		}

		// Set page, limit and total count.
		dataObjectsDTO.setPage(page);
		int limit = dataSearchService.getSearchResultsPageSize(pageSize);
		dataObjectsDTO.setLimit(limit);

		if (totalCount) {
			dataObjectsDTO.setTotalCount((page == 1 && count < limit) ? count
					: dataSearchService.getDataObjectCount(path, compoundMetadataQueryDTO.getCompoundQuery()));
		}

		return dataObjectsDTO;

	}
	
	@Override
	public HpcDataObjectListDTO getAllDataObjects(String path, Integer page, Integer pageSize, Boolean totalCount)
			throws HpcException {

		// Default page to 1 if user didn't request it
		page = page == null ? 1 : page;
		// input validation
		if (page < 1 || pageSize != null && pageSize < 1) {
		      throw new HpcException(
		          "Invalid search results page or pageSize requested", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
		
		// Get the user-id of the request invoker.
		String dataManagementUsername = securityService.getRequestInvoker().getDataManagementAccount().getUsername();

		totalCount = totalCount == null || totalCount;
		int limit = 0;
		if (pageSize != null && pageSize <= getAllDataObjectsDefaultPageSize) {
			// Compute the offset
			int offset = (page - 1) * pageSize;
			
			// Execute the query and package the results into a DTO.
			int count = 0;
			List<HpcSearchMetadataEntry> dataObjectPaths = dataSearchService.getAllDataObjectPaths(dataManagementUsername, path, offset, pageSize);
			dataObjectsDTO = toDetailedDataObjectListDTO(dataObjectPaths);

			// Set page, limit and total count.
			dataObjectsDTO.setPage(page);
			limit = dataSearchService.getSearchResultsPageSize(pageSize);
			dataObjectsDTO.setLimit(limit);
			
		} else {
			// Get the max limit, we will return that many records
			limit = dataSearchService.getSearchResultsPageSize(100000);
			// If the user requested a page size that is smaller than the max, set limit to user page size
			limit = pageSize = pageSize != null && pageSize < limit ? pageSize : limit;
			// Compute the initial offset
			int offset = (page - 1) * pageSize;
								
			List<Callable<HpcDataObjectListDTO>> callableTasks = new ArrayList<>();
			
			for (int i = 0; i < limit; i += getAllDataObjectsDefaultPageSize) {
				callableTasks.add(new HpcSearchRequest(dataSearchService, dataManagementUsername, path,
						offset, limit - i < getAllDataObjectsDefaultPageSize ? limit - i : getAllDataObjectsDefaultPageSize));
				offset += getAllDataObjectsDefaultPageSize;
			}
			
			List<Future<HpcDataObjectListDTO>> futures = null;
			try {
				futures = hpcGetAllDataObjectsExecutorService.invokeAll(callableTasks);
				for (Future<HpcDataObjectListDTO> future : futures) {
					HpcDataObjectListDTO result = future.get();
					if(result.getDataObjects() != null)
						dataObjectsDTO.getDataObjects().addAll(result.getDataObjects());
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new HpcException(e.getMessage(), HpcErrorType.DATABASE_ERROR);
			}
			
			// Set page, limit and total count.
			dataObjectsDTO.setPage(page);
			dataObjectsDTO.setLimit(limit);
		}
		
		if (totalCount) {
			int count = dataObjectsDTO.getDataObjects().size();
			dataObjectsDTO.setTotalCount((page == 1 && count < limit) ? count
					: dataSearchService.getAllDataObjectCount(path));
		}
		
		return dataObjectsDTO;

	}

	@Override
	public HpcDataObjectListDTO getDataObjects(String queryName, Boolean detailedResponse, Integer page,
			Integer pageSize, Boolean totalCount) throws HpcException {
		return getDataObjects(null,
				toCompoundMetadataQueryDTO(queryName, detailedResponse, page, pageSize, totalCount));
	}


	@Override
	public HpcCollectionListDTO getDataObjectParents(String path, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
			throws HpcException {
		// Input validation.
		if (compoundMetadataQueryDTO == null) {
			throw new HpcException("Null compound metadata query", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null
				&& compoundMetadataQueryDTO.getDetailedResponse();
		int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
		int pageSize = compoundMetadataQueryDTO.getPageSize() != null ? compoundMetadataQueryDTO.getPageSize() : 0;
		boolean totalCount = compoundMetadataQueryDTO.getTotalCount() != null
				&& compoundMetadataQueryDTO.getTotalCount();

		// Execute the query and package the results in a DTO.
		int count = 0;
		HpcCollectionListDTO collectionsDTO = null;
		if (!detailedResponse) {
			List<String> collectionPaths = dataSearchService
					.getDataObjectParentPaths(path, compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			collectionsDTO = toCollectionListDTO(collectionPaths, detailedResponse);
			count = collectionPaths.size();
		} else {
			List<HpcSearchMetadataEntryForCollection> collectionPaths = dataSearchService
					.getDetailedDataObjectParentPaths(path, compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			collectionsDTO = toDetailedCollectionListDTO(collectionPaths);
			count = collectionsDTO.getCollections().size();
		}

		// Set page, limit and total count.
		collectionsDTO.setPage(page);
		int limit = dataSearchService.getSearchResultsPageSize(pageSize);
		collectionsDTO.setLimit(limit);

		if (totalCount) {
			collectionsDTO.setTotalCount((page == 1 && count < limit) ? count
					: dataSearchService.getCollectionCount(compoundMetadataQueryDTO.getCompoundQuery()));
		}

		return collectionsDTO;
	}


	@Override
	public HpcCollectionListDTO getDataObjectParents(String queryName, Boolean detailedResponse, Integer page,
			Integer pageSize, Boolean totalCount) throws HpcException {
		return getDataObjectParents(null,
				toCompoundMetadataQueryDTO(queryName, detailedResponse, page, pageSize, totalCount));
	}


	@Override
	public void addQuery(String queryName, String userId, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
			throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(queryName) || compoundMetadataQueryDTO == null) {
			throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the user-id of the request invoker if user not specified.
		String nciUserId = userId;
		if (nciUserId == null) {
			nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();
		}

		if (dataSearchService.getQuery(nciUserId, queryName) != null) {
			throw new HpcException("Query name already exists: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = new HpcNamedCompoundMetadataQuery();
		namedCompoundMetadataQuery.setName(queryName);
		namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
		namedCompoundMetadataQuery.setCreated(Calendar.getInstance());
		namedCompoundMetadataQuery.setDetailedResponse(
				compoundMetadataQueryDTO.getDetailedResponse() != null ? compoundMetadataQueryDTO.getDetailedResponse()
						: false);
		namedCompoundMetadataQuery.setTotalCount(
				compoundMetadataQueryDTO.getTotalCount() != null ? compoundMetadataQueryDTO.getTotalCount() : false);
		namedCompoundMetadataQuery.setCompoundQueryType(compoundMetadataQueryDTO.getCompoundQueryType());

		// Save the query.
		dataSearchService.saveQuery(nciUserId, namedCompoundMetadataQuery);
	}

	@Override
	public void updateQuery(String queryName, HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
			throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(queryName) || compoundMetadataQueryDTO == null) {
			throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the user-id of this request invoker.
		String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();

		// Get the query.
		HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = dataSearchService.getQuery(nciUserId, queryName);
		if (namedCompoundMetadataQuery == null) {
			throw new HpcException("Query name doesn't exist: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Update the query.
		if (compoundMetadataQueryDTO.getCompoundQuery() != null) {
			namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
		}
		if (compoundMetadataQueryDTO.getDetailedResponse() != null) {
			namedCompoundMetadataQuery.setDetailedResponse(compoundMetadataQueryDTO.getDetailedResponse());
		}
		if (compoundMetadataQueryDTO.getTotalCount() != null) {
			namedCompoundMetadataQuery.setTotalCount(compoundMetadataQueryDTO.getTotalCount());
		}
		if (compoundMetadataQueryDTO.getCompoundQueryType() != null) {
			namedCompoundMetadataQuery.setCompoundQueryType(compoundMetadataQueryDTO.getCompoundQueryType());
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
		dataSearchService.deleteQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), queryName);
	}

	@Override
	public HpcNamedCompoundMetadataQueryDTO getQuery(String queryName) throws HpcException {
		HpcNamedCompoundMetadataQueryDTO queryDTO = new HpcNamedCompoundMetadataQueryDTO();
		queryDTO.setNamedCompoundQuery(
				dataSearchService.getQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), queryName));

		return queryDTO;
	}

	@Override
	public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException {
		HpcNamedCompoundMetadataQueryListDTO queryList = new HpcNamedCompoundMetadataQueryListDTO();
		queryList.getNamedCompoundQueries()
				.addAll(dataSearchService.getQueries(securityService.getRequestInvoker().getNciAccount().getUserId()));

		return queryList;
	}

	@Override
	public HpcMetadataAttributesListDTO getMetadataAttributes(String levelLabel) throws HpcException {
		HpcMetadataAttributesListDTO metadataAttributes = new HpcMetadataAttributesListDTO();
		metadataAttributes.getCollectionMetadataAttributes()
				.addAll(dataSearchService.getCollectionMetadataAttributes(levelLabel));
		metadataAttributes.getDataObjectMetadataAttributes()
				.addAll(dataSearchService.getDataObjectMetadataAttributes(levelLabel));

		return metadataAttributes;
	}

	@Override
	public HpcCatalogsDTO getCatalog(HpcCatalogRequestDTO catalogRequestDTO) throws HpcException {
		// Input validation.
		if (catalogRequestDTO == null) {
			throw new HpcException("Null catalog request query", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (catalogRequestDTO.getPage() == null)
			catalogRequestDTO.setPage(1);
		if (catalogRequestDTO.getPageSize() == null)
			catalogRequestDTO.setPageSize(0);
		boolean totalCount = catalogRequestDTO.getTotalCount() != null && catalogRequestDTO.getTotalCount();

		HpcCatalogCriteria criteria = new HpcCatalogCriteria();
		BeanUtils.copyProperties(catalogRequestDTO, criteria);

		// Execute the query and package the results into a DTO.
		int count = 0;
		HpcCatalogsDTO catalogsDTO = new HpcCatalogsDTO();
		List<HpcCatalog> hpcCatalogs = catalogService.getCatalog(criteria);
		count = hpcCatalogs.size();

		// Set page, limit and total count.
		catalogsDTO.setPage(catalogRequestDTO.getPage());
		int limit = dataSearchService.getSearchResultsPageSize(catalogRequestDTO.getPageSize());
		catalogsDTO.setPageSize(limit);

		if (totalCount) {
			catalogsDTO.setTotalCount((catalogRequestDTO.getPage() == 1 && count < limit) ? count
					: catalogService.getCatalogCount(criteria));
		}

		catalogsDTO.getCatalogs().addAll(hpcCatalogs);

		return catalogsDTO;

	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Construct a collection list DTO.
	 *
	 * @param collectionPaths  A list of collection paths.
	 * @param detailedResponse If set to true, return entity details (attributes +
	 *                         metadata).
	 * @return A collection list DTO.
	 * @throws HpcException on service failure.
	 */
	private HpcCollectionListDTO toCollectionListDTO(List<String> collectionPaths, boolean detailedResponse)
			throws HpcException {
		HpcCollectionListDTO collectionsDTO = new HpcCollectionListDTO();

		if (detailedResponse) {
			for (String collectionPath : collectionPaths) {
				collectionsDTO.getCollections()
						.add(dataManagementBusService.getCollection(collectionPath, false, false));
			}
		} else {
			collectionsDTO.getCollectionPaths().addAll(collectionPaths);
		}

		return collectionsDTO;
	}

	private HpcCollectionListDTO toDetailedCollectionListDTO(
			List<HpcSearchMetadataEntryForCollection> collectionPaths) {
		HpcCollectionListDTO collectionDTO = new HpcCollectionListDTO();
		if (!CollectionUtils.isEmpty(collectionPaths)) {
			collectionPaths.sort(Comparator
					.comparing(HpcSearchMetadataEntryForCollection::getAbsolutePath, String::compareToIgnoreCase)
					.thenComparing(HpcSearchMetadataEntryForCollection::getLevel));
		}
		int prevId = 0;
		HpcCollectionDTO collection = null;
		for (HpcSearchMetadataEntryForCollection collectionPath : collectionPaths) {
			if (collection == null || collectionPath.getCollectionId() != prevId) {
				collection = new HpcCollectionDTO();
				HpcCollection coll = new HpcCollection();
				BeanUtils.copyProperties(collectionPath, coll);
				collection.setCollection(coll);
				collectionDTO.getCollections().add(collection);
			}
			if (collection.getMetadataEntries() == null) {
				HpcMetadataEntries entries = new HpcMetadataEntries();
				collection.setMetadataEntries(entries);
			}
			if (collectionPath.getLevel().intValue() == 1) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(collectionPath, entry);
				collection.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(collectionPath, entry);
				entry.setCollectionId(collectionPath.getMetaCollectionId());
				collection.getMetadataEntries().getParentMetadataEntries().add(entry);
			}
			prevId = collectionPath.getCollectionId();
		}
		return collectionDTO;
	}

	/**
	 * Construct a data object list DTO.
	 *
	 * @param dataObjectPaths  A list of data object paths.
	 * @param detailedResponse If set to true, return entity details (attributes +
	 *                         metadata).
	 * @return A data object list DTO.
	 * @throws HpcException on service failure.
	 */
	private HpcDataObjectListDTO toDataObjectListDTO(List<String> dataObjectPaths, boolean detailedResponse)
			throws HpcException {
		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();

		if (detailedResponse) {
			for (String dataObjectPath : dataObjectPaths) {
				dataObjectsDTO.getDataObjects().add(dataManagementBusService.getDataObjectV1(dataObjectPath, false));
			}
		} else {
			dataObjectsDTO.getDataObjectPaths().addAll(dataObjectPaths);
		}

		return dataObjectsDTO;
	}

	private HpcDataObjectListDTO toDetailedDataObjectListDTO(List<HpcSearchMetadataEntry> dataObjectPaths) {
		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
		if (!CollectionUtils.isEmpty(dataObjectPaths)) {
			dataObjectPaths
					.sort(Comparator.comparing(HpcSearchMetadataEntry::getAbsolutePath, String::compareToIgnoreCase)
							.thenComparing(HpcSearchMetadataEntry::getLevel));
		}
		int prevId = 0;
		HpcDataObjectDTO dataObject = null;
		for (HpcSearchMetadataEntry dataObjectPath : dataObjectPaths) {
			if (dataObject == null || dataObjectPath.getId() != prevId) {
				dataObject = new HpcDataObjectDTO();
				HpcDataObject dataObj = new HpcDataObject();
				BeanUtils.copyProperties(dataObjectPath, dataObj);
				dataObject.setDataObject(dataObj);
				dataObjectsDTO.getDataObjects().add(dataObject);
			}
			if (dataObject.getMetadataEntries() == null) {
				HpcMetadataEntries entries = new HpcMetadataEntries();
				dataObject.setMetadataEntries(entries);
			}
			if (dataObjectPath.getLevel().intValue() == 1) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(dataObjectPath, entry);
				dataObject.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				BeanUtils.copyProperties(dataObjectPath, entry);
				dataObject.getMetadataEntries().getParentMetadataEntries().add(entry);
			}
			prevId = dataObjectPath.getId();
		}
		return dataObjectsDTO;
	}

	/**
	 * Construct a HpcCompoundMetadataQueryDTO from a named query.
	 *
	 * @param queryName        The user query.
	 * @param detailedResponse The detailed response indicator.
	 * @param page             The requested results page.
	 * @param pageSize         The requested page size.
	 * @param totalCount       The requested total count of results.
	 * @return A compound metadata query DTO.
	 * @throws HpcException If the user query was not found.
	 */
	private HpcCompoundMetadataQueryDTO toCompoundMetadataQueryDTO(String queryName, Boolean detailedResponse,
			Integer page, Integer pageSize, Boolean totalCount) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(queryName)) {
			throw new HpcException("Null or empty query name", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the user query.
		HpcNamedCompoundMetadataQuery namedCompoundQuery = dataSearchService
				.getQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), queryName);
		if (namedCompoundQuery == null || namedCompoundQuery.getCompoundQuery() == null) {
			throw new HpcException("User query not found: " + queryName, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Construct the query DTO.
		HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
		compoundMetadataQueryDTO.setCompoundQuery(namedCompoundQuery.getCompoundQuery());
		compoundMetadataQueryDTO.setDetailedResponse(
				detailedResponse != null ? detailedResponse : namedCompoundQuery.getDetailedResponse());
		compoundMetadataQueryDTO.setPage(page != null ? page : 1);
		compoundMetadataQueryDTO.setPageSize(pageSize != null ? pageSize : 0);
		compoundMetadataQueryDTO.setTotalCount(totalCount != null ? totalCount : namedCompoundQuery.getTotalCount());

		return compoundMetadataQueryDTO;
	}
}
