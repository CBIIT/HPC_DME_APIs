/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.download;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easybatch.core.dispatcher.PoisonRecordBroadcaster;
import org.easybatch.core.dispatcher.RoundRobinRecordDispatcher;
import org.easybatch.core.filter.PoisonRecordFilter;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.reader.BlockingQueueRecordReader;
import org.easybatch.core.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.cli.HPCJobReportMerger;
import gov.nih.nci.hpc.cli.HpcJobReportFormatter;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCBatchCollectionDownloadProcessor {

	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

	Map<String, String> criteriaMap = null;
	private HpcServerConnection connection;
	int threadPoolSize;
	private String errorRecordsFile;
	private String logFile;

	public HPCBatchCollectionDownloadProcessor(Map<String, String> criteriaMap, HpcServerConnection connection,
			String logFile, String errorRecordsFile) {
		this.criteriaMap = criteriaMap;
		this.connection = connection;
		this.logFile = logFile;
		this.errorRecordsFile = errorRecordsFile;
	}

	public String processData() throws HpcBatchException {
		ExecutorService executorService = null;
		List<HpcCollectionListingEntry> dataObjects = new ArrayList<>();
		String threadStr = criteriaMap.get("threads");
		String sourceArchivePath = criteriaMap.get("sourceArchivePath");
		String destinationPath = criteriaMap.get("destinationPath");
		logger.debug("sourceArchivePath {}", sourceArchivePath);
		logger.debug("destinationPath {}", destinationPath);
		logger.debug("threadStr {}", threadStr);

		if (StringUtils.isNotBlank(threadStr)) {
			try {
				threadPoolSize = Integer.parseInt(threadStr);
			} catch (NumberFormatException e) {
				logger.error(e.getMessage(), e);
				System.out.println("Failed to process number of threads input: " + threadStr);
				return Constants.CLI_2;
			}
		} else {
			threadPoolSize = 1;
		}

		// Get all data objects recursively that belongs to this collection
		try {
			dataObjects = getAllDataObjectsForCollection(sourceArchivePath, dataObjects);
		} catch (Exception e) {
			System.out.println("Failed to download collection: " + sourceArchivePath + " due to: " + e.getMessage());
			return Constants.CLI_5;
		}

		try {
			List<BlockingQueue<Record>> queueList = new ArrayList<BlockingQueue<Record>>();
			// Create queues
			for (int i = 0; i < threadPoolSize; i++) {
				BlockingQueue<Record> queue = new LinkedBlockingQueue<>();
				queueList.add(queue);
			}
			RoundRobinRecordDispatcher<Record> roundRobinRecordDispatcher = new RoundRobinRecordDispatcher<>(queueList);

			// Build a master job that will read data object records from the response
			// and dispatch them to worker jobs
			Job masterJob = JobBuilder.aNewJob().named("master-job")
					.reader(new HPCBatchCollectionDownloadRecordReader(dataObjects))
					.mapper(new HPCBatchCollectionDownloadRecordMapper(HPCDataObject.class, criteriaMap, connection,
							logFile, errorRecordsFile))
					.dispatcher(roundRobinRecordDispatcher).jobListener(new PoisonRecordBroadcaster<>(queueList))
					.build();

			// Build worker jobs
			List<Job> jobs = new ArrayList<Job>();
			jobs.add(masterJob);
			for (int i = 0; i < threadPoolSize; i++) {
				Job workerJob = buildWorkerJob(queueList.get(i), "HPC DME Data object download" + i);
				jobs.add(workerJob);
			}

			// Create a thread pool to call master and worker jobs in parallel
			executorService = Executors.newFixedThreadPool(threadPoolSize);

			// Submit workers to executor service
			List<Future<JobReport>> reports = executorService.invokeAll(jobs);

			List<JobReport> jobReports = new ArrayList<JobReport>();
			for (int i = 0; i < reports.size(); i++) {
				JobReport jobReport = reports.get(i).get();
				jobReports.add(jobReport);
			}

			HPCJobReportMerger reportMerger = new HPCJobReportMerger();
			JobReport finalReport = reportMerger.mergerReports(jobReports);
			System.out.println(new HpcJobReportFormatter().formatReport(finalReport));

		} catch (ExecutionException e) {
			System.out.println("Failed to download collection: " + sourceArchivePath + " due to: " + e.getMessage());
			return Constants.CLI_5;
		} catch (InterruptedException e) {
			System.out.println("Failed to download collection: " + sourceArchivePath + " due to: " + e.getMessage());
			return Constants.CLI_5;
		} finally {
			// Shutdown executor service
			if (executorService != null) {
				executorService.shutdown();
			}
		}

		return Constants.CLI_SUCCESS;
	}

	private List<HpcCollectionListingEntry> getAllDataObjectsForCollection(String sourceArchivePath,
			List<HpcCollectionListingEntry> dataObjects) throws HpcBatchException {

		HpcCollectionDTO collection;

		// Get a list of data object belonging to this collection
		String apiUrl2Apply = null;
		try {
			final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(connection.getHpcServerURL())
					.path("/collection/{collection-path}");
			ucBuilder.queryParam("list", Boolean.TRUE.toString());
			apiUrl2Apply = ucBuilder.buildAndExpand(sourceArchivePath).encode().toUri().toURL().toExternalForm();

		} catch (MalformedURLException e) {
			throw new HpcBatchException("Error malformed URL", e);
		}

		WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply, connection.getHpcServerProxyURL(),
				connection.getHpcServerProxyPort(), connection.getHpcCertPath(), connection.getHpcCertPassword());
		client.header("Authorization", "Bearer " + connection.getAuthToken());
		Response restResponse = client.get();

		if (restResponse.getStatus() == 200) {
			ObjectMapper mapper = new ObjectMapper();
			AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
					new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()), new JacksonAnnotationIntrospector());
			mapper.setAnnotationIntrospector(intr);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			MappingJsonFactory factory = new MappingJsonFactory(mapper);
			JsonParser parser;
			try {
				parser = factory.createParser((InputStream) restResponse.getEntity());
				HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
				collection = collections.getCollections().get(0);
				dataObjects.addAll(collection.getCollection().getDataObjects());
			} catch (IOException e) {
				throw new HpcBatchException("failed to parse response", e);
			}
		} else {
			HpcExceptionDTO errorResponse = null;
			try {
				ObjectMapper mapper = new ObjectMapper();
				MappingJsonFactory factory = new MappingJsonFactory(mapper);

				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				errorResponse = parser.readValueAs(HpcExceptionDTO.class);

			} catch (IOException e) {
				throw new HpcBatchException("Error response code: " + restResponse.getStatus(), e);
			}

			throw new HpcBatchException("Error : " + errorResponse.getMessage());
		}

		// Iterate through the sub-collections and download them.
		for (HpcCollectionListingEntry subCollectionEntry : collection.getCollection().getSubCollections()) {
			getAllDataObjectsForCollection(subCollectionEntry.getPath(), dataObjects);
		}

		return dataObjects;

	}

	public static Job buildWorkerJob(BlockingQueue<Record> queue, String jobName) {
		return JobBuilder.aNewJob().named(jobName).silentMode(true).reader(new BlockingQueueRecordReader(queue))
				.filter(new PoisonRecordFilter()).processor(new HPCBatchCollectionDownloadRecordProcessor()).build();

	}
}
