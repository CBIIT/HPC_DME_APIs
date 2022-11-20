/**
 * HpcDataSearchBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.catalog.HpcCatalog;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryFrequency;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntryForCollection;
import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
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
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcSystemAccountFunction;

/**
 * HPC Data Search Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataSearchBusServiceImpl implements HpcDataSearchBusService {
	
	private static final int USER_QUERY_SEARCH_RESULTS_PAGE_SIZE = 10000;
			
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

	// Notification Application service instance.
	@Autowired
	private HpcNotificationService notificationService = null;
	
	// Excel Exporter.
	@Autowired
	private HpcExporter exporter = null;
		
	// The collection download task executor.
	@Autowired
	@Qualifier("hpcGetAllDataObjectsExecutorService")
	ExecutorService hpcGetAllDataObjectsExecutorService = null;

	@Value("${hpc.bus.getAllDataObjectsDefaultPageSize}")
	private int getAllDataObjectsDefaultPageSize = 0;

	// Directory to create the user query excel export.
	@Value("${hpc.bus.exportDirectory}")
	private String exportDirectory = null;
		
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
			throw new HpcException("Invalid search results page or pageSize requested",
					HpcErrorType.INVALID_REQUEST_INPUT);
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
			logger.debug("Retrieving objects from DB for path {}", path);
			List<HpcSearchMetadataEntry> dataObjectPaths = dataSearchService
					.getAllDataObjectPaths(dataManagementUsername, path, offset, pageSize);

			logger.debug("Generating DTO list for objects from {}", path);
			dataObjectsDTO = toDetailedDataObjectListDTO(dataObjectPaths);

			// Set page, limit and total count.
			logger.debug("Setting page and limit for DTOList for {}", path);
			dataObjectsDTO.setPage(page);
			limit = dataSearchService.getSearchResultsPageSize(pageSize);
			dataObjectsDTO.setLimit(limit);

		} else {
			// Get the max limit, we will return that many records
			limit = dataSearchService.getSearchResultsPageSize(100000);
			// If the user requested a page size that is smaller than the max, set limit to
			// user page size
			limit = pageSize = pageSize != null && pageSize < limit ? pageSize : limit;
			// Compute the initial offset
			int offset = (page - 1) * pageSize;

			List<Callable<HpcDataObjectListDTO>> callableTasks = new ArrayList<>();

			for (int i = 0; i < limit; i += getAllDataObjectsDefaultPageSize) {
				callableTasks.add(
						new HpcSearchRequest(dataSearchService, securityService, dataManagementUsername, path, offset,
								limit - i < getAllDataObjectsDefaultPageSize ? limit - i
										: getAllDataObjectsDefaultPageSize,
								securityService.getRequestInvoker().getMetadataOnly()));
				offset += getAllDataObjectsDefaultPageSize;
			}

			List<Future<HpcDataObjectListDTO>> futures = null;
			try {
				futures = hpcGetAllDataObjectsExecutorService.invokeAll(callableTasks);
				for (Future<HpcDataObjectListDTO> future : futures) {
					HpcDataObjectListDTO result = future.get();
					if (result.getDataObjects() != null)
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
			dataObjectsDTO.setTotalCount(
					(page == 1 && count < limit) ? count : dataSearchService.getAllDataObjectCount(path));
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
			List<String> collectionPaths = dataSearchService.getDataObjectParentPaths(path,
					compoundMetadataQueryDTO.getCompoundQuery(), page, pageSize);
			collectionsDTO = toCollectionListDTO(collectionPaths, detailedResponse);
			count = collectionPaths.size();
		} else {
			List<HpcSearchMetadataEntryForCollection> collectionPaths = dataSearchService
					.getDetailedDataObjectParentPaths(path, compoundMetadataQueryDTO.getCompoundQuery(), page,
							pageSize);
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
		namedCompoundMetadataQuery.getSelectedColumns().addAll(compoundMetadataQueryDTO.getSelectedColumns());
		namedCompoundMetadataQuery.setFrequency(compoundMetadataQueryDTO.getFrequency());

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
		if (compoundMetadataQueryDTO.getSelectedColumns() != null && !compoundMetadataQueryDTO.getSelectedColumns().isEmpty()) {
			if(namedCompoundMetadataQuery.getSelectedColumns() != null)
				namedCompoundMetadataQuery.getSelectedColumns().clear();
			namedCompoundMetadataQuery.getSelectedColumns().addAll(compoundMetadataQueryDTO.getSelectedColumns());
		}
		namedCompoundMetadataQuery.setFrequency(compoundMetadataQueryDTO.getFrequency());

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
	
	@Override
	@HpcExecuteAsSystemAccount
	public void sendWeeklyQueryResults() throws HpcException {
		sendQueryResults(HpcCompoundMetadataQueryFrequency.WEEKLY);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void sendMonthlyQueryResults() throws HpcException {
		sendQueryResults(HpcCompoundMetadataQueryFrequency.MONTHLY);
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
		boolean encrypt = securityService.getRequestInvoker().getMetadataOnly();

		HpcCollectionListDTO collectionDTO = new HpcCollectionListDTO();
		if (!CollectionUtils.isEmpty(collectionPaths)) {
			collectionPaths.sort(Comparator
					.comparing(HpcSearchMetadataEntryForCollection::getAbsolutePath, String::compareTo)
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

			HpcMetadataEntry entry = new HpcMetadataEntry();
			BeanUtils.copyProperties(collectionPath, entry);
			HpcQueryConfiguration queryConfig = null;
			HpcEncryptor encryptor = null;
			if (encrypt) {
				try {
					String basePath = collectionPath.getAbsolutePath().substring(0,
							collectionPath.getAbsolutePath().indexOf('/', 1));
					queryConfig = securityService.getQueryConfig(basePath);
					if (queryConfig != null)
						encryptor = new HpcEncryptor(queryConfig.getEncryptionKey());
				} catch (HpcException e) {
					// Failed to get encryptor so don't return the value
					entry.setValue("");
				}
			}

			if (collectionPath.getLevel().intValue() == 1) {
				// Encrypt metadata if metadata only user
				if (encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
				collection.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				entry.setCollectionId(collectionPath.getMetaCollectionId());
				// Encrypt metadata if metadata only user
				if (encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
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
	@SuppressWarnings("deprecation")
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

		boolean encrypt = securityService.getRequestInvoker().getMetadataOnly();

		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();

		if (!CollectionUtils.isEmpty(dataObjectPaths)) {
			dataObjectPaths
					.sort(Comparator.comparing(HpcSearchMetadataEntry::getAbsolutePath, String::compareTo)
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

			HpcMetadataEntry entry = new HpcMetadataEntry();
			BeanUtils.copyProperties(dataObjectPath, entry);
			HpcQueryConfiguration queryConfig = null;
			HpcEncryptor encryptor = null;
			if (encrypt) {
				try {
					String basePath = dataObjectPath.getAbsolutePath().substring(0,
							dataObjectPath.getAbsolutePath().indexOf('/', 1));
					queryConfig = securityService.getQueryConfig(basePath);
					if (queryConfig != null)
						encryptor = new HpcEncryptor(queryConfig.getEncryptionKey());
				} catch (HpcException e) {
					// Failed to get encryptor so don't return the value
					entry.setValue("");
				}
			}

			if (dataObject.getMetadataEntries() == null) {
				HpcMetadataEntries entries = new HpcMetadataEntries();
				dataObject.setMetadataEntries(entries);
			}
			if (dataObjectPath.getLevel().intValue() == 1) {
				// Encrypt metadata if metadata only user
				if (encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
				dataObject.getMetadataEntries().getSelfMetadataEntries().add(entry);
			} else {
				// Encrypt metadata if metadata only user
				if (encryptor != null)
					entry.setValue(Base64.getEncoder().encodeToString(encryptor.encrypt(entry.getValue())));
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
	
	/**
	 * Run and send all scheduled user stored query for the specified frequency
	 *
	 * 
	 * @param HpcCompoundMetadataQueryFrequency The auto run frequency
	 * @throws HpcException If the user query was not found.
	 */
	private void sendQueryResults(HpcCompoundMetadataQueryFrequency frequency) throws HpcException {
		// Get a list of user stored queries where auto run is scheduled.
		List<HpcNamedCompoundMetadataQuery> queryList = dataSearchService.getQueriesByFrequency(frequency);
		
		// For each query, send user an email with the stored query results as an attachment.
		for (HpcNamedCompoundMetadataQuery query : queryList) {
			// Send query result of a namedQuery
			try {
				sendQuery(query.getUserId(), query.getName());
			} catch (HpcException e) {
				logger.error(e.getMessage(), e);
				notificationService.sendNotification(e);
			}
			
		}
	}
	
	/**
	 *  Run and construct the excel export of the specified query results and send to the user
	 *  This needs to run with correct user account to get the search results with user permissions
	 *  
	 * @param userId The userId of the stored query that is being run
	 * @param queryName The query name to be run
	 * @throws HpcException If the user query failed or failed to export
	 */
	private void sendQuery(String userId, String queryName) throws HpcException {
		
		logger.info("Running query {} for user {}", queryName, userId);
		HpcNamedCompoundMetadataQuery query = dataSearchService.getQuery(userId, queryName);
		//Construct the excel file name
		String exportFileName = exportDirectory + File.separator + "Search_Results_" + queryName
                + MessageFormat.format("_{0,date,MM_dd_yyyy}", new Date()).trim() + ".xls";
        
		//Construct the query
		HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
		compoundMetadataQueryDTO.setCompoundQuery(query.getCompoundQuery());
		compoundMetadataQueryDTO.setDetailedResponse(query.getDetailedResponse());
		compoundMetadataQueryDTO.setPageSize(USER_QUERY_SEARCH_RESULTS_PAGE_SIZE);
		compoundMetadataQueryDTO.setTotalCount(true);
		
		try {
			if(query.getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT)) {
				HpcDataObjectListDTO dataobjectListDTO = new HpcDataObjectListDTO();
				int pageNumber = 1;
				int totalPages = 1;
				do {
					compoundMetadataQueryDTO.setPage(pageNumber++);
					//Switch context to the query user
					HpcDataObjectListDTO dataobjectsDTO = executeAsUserAccount(() -> getDataObjects(null, compoundMetadataQueryDTO), userId);
					dataobjectListDTO.getDataObjectPaths().addAll(dataobjectsDTO.getDataObjectPaths());
					dataobjectListDTO.getDataObjects().addAll(dataobjectsDTO.getDataObjects());
					totalPages = getTotalPages(dataobjectsDTO.getTotalCount(), USER_QUERY_SEARCH_RESULTS_PAGE_SIZE);
				} while (pageNumber <= totalPages);
				if(CollectionUtils.isEmpty(dataobjectListDTO.getDataObjectPaths()) && CollectionUtils.isEmpty(dataobjectListDTO.getDataObjects())) {
					logger.info("No results found from query {} for user {}", queryName, userId);
					return;
				}
				exporter.exportDataObjects(exportFileName, dataobjectListDTO, query.getSelectedColumns());
			} else {
				HpcCollectionListDTO collectionListDTO = new HpcCollectionListDTO();
				int pageNumber = 1;
				int totalPages = 1;
				do {
					compoundMetadataQueryDTO.setPage(pageNumber++);
					//Switch context to the query user
					HpcCollectionListDTO collectionsDTO = executeAsUserAccount(() -> getCollections(compoundMetadataQueryDTO), userId);
					collectionListDTO.getCollectionPaths().addAll(collectionsDTO.getCollectionPaths());
					collectionListDTO.getCollections().addAll(collectionsDTO.getCollections());
					totalPages = getTotalPages(collectionsDTO.getTotalCount(), USER_QUERY_SEARCH_RESULTS_PAGE_SIZE);
				} while (pageNumber <= totalPages);
				if(CollectionUtils.isEmpty(collectionListDTO.getCollectionPaths()) && CollectionUtils.isEmpty(collectionListDTO.getCollections())) {
					logger.info("No results found from query {} for user {}", queryName, userId);
					return;
				}
				exporter.exportCollections(exportFileName, collectionListDTO, query.getSelectedColumns());
			}
			// Send the file to the user
			sendQueryNotification(userId, exportFileName, queryName, query.getFrequency().value());
			
		} catch (IOException e) {
			throw new HpcException("Error exporting search result for query: " + queryName, HpcErrorType.UNEXPECTED_ERROR, e);
		} finally {
			// Delete the file
			Path path = FileSystems.getDefault().getPath(exportFileName);
	        try {
	            Files.deleteIfExists(path);
	        } catch (IOException e) {
	        	logger.error("Failed to delete file: {}", exportFileName, e);
	        }
        }

	}
	
	/**
	 * Email the exported user query results to the user.
	 *
	 * @param nciUserId The NCI user ID.
	 * @param exportFileName The exported query results.
	 * @param queryName  The query name.
	 * @param frequency The scheduled frequency.
	 * @throws HpcException 
	 */
	private void sendQueryNotification(String nciUserId, String exportFileName, String queryName, String frequency) throws HpcException {
		
		logger.info("Sending {} query result for {} to {}", frequency, queryName, nciUserId);
		if (nciUserId == null) {
			throw new HpcException("Null nciUserId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcUser user = securityService.getUser(nciUserId);
		if (user == null) {
			throw new HpcException("User doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();
		HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("QUERY_NAME");
		payloadEntry.setValue(queryName);
		payloadEntries.add(payloadEntry);
		
		payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("FREQUENCY");
		payloadEntry.setValue(frequency);
		payloadEntries.add(payloadEntry);

		notificationService.sendNotification(nciUserId, HpcEventType.USER_QUERY_SENT, payloadEntries,
				HpcNotificationDeliveryMethod.EMAIL, exportFileName);
	}
	
	/**
	 * When the scheduler job is running using system account, the query results 
	 * will be different based on user's permissions. As a workaround, if the
	 * invoker is a system account then we simulate running as user account. 
	 * If the invoker is not a group-admin, then its own credentials are used.
	 *
	 * @param userAccountFunction The functional interface to execute as user
	 *                              account with return value.
	 * @param userId               The userId of the user account to be executed as.
	 * @param <T>                   A generic returned type.
	 * @return A generic returned type.
	 * @throws HpcException If it failed to identify the request invoker, or the
	 *                      function raised an exception.
	 */
	private <T> T executeAsUserAccount(HpcSystemAccountFunction<T> userAccountFunction, String userId)
			throws HpcException {
		HpcRequestInvoker invoker = securityService.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		return securityService.executeAsUserAccount(userId, userAccountFunction);
	}
	
	/**
	 * Compute the total number of pages for search results
	 * 
	 * @param totalCount The total result count
	 * @param limit The page size
	 * @return
	 */
	private static int getTotalPages(int totalCount, int limit)
	{
		int total = 0;
		if(limit <=0)
			limit = 100;
		total = totalCount / limit;
		if(totalCount % limit != 0)
			total++;
		return total;
	}
}
