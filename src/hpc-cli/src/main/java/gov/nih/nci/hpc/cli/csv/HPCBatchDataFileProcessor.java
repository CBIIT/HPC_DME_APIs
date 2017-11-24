/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.easybatch.core.dispatcher.PoisonRecordBroadcaster;
import org.easybatch.core.dispatcher.RoundRobinRecordDispatcher;
import org.easybatch.core.filter.PoisonRecordFilter;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.reader.BlockingQueueRecordReader;
import org.easybatch.core.record.Record;
import org.easybatch.extensions.apache.common.csv.ApacheCommonCsvRecordReader;

import gov.nih.nci.hpc.cli.HPCJobReportMerger;
import gov.nih.nci.hpc.cli.HpcJobReportFormatter;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcBatchException;

public class HPCBatchDataFileProcessor {

	String inputFileName;
	int threadPoolSize;
	private String basePath;
	private String hpcCertPath;
	private String hpcCertPassword;
	private String userId;
	private String password;
	private String authToken;
	private String errorRecordsFile;
	private String logFile;

	public HPCBatchDataFileProcessor(String inputFileName, int threadPoolSize, String basePath, String hpcCertPath,
			String hpcCertPassword, String userId, String password, String logFile, String errorRecordsFile,
			String authToken) {
		this.inputFileName = inputFileName;
		this.threadPoolSize = threadPoolSize;
		this.basePath = basePath;
		this.hpcCertPath = hpcCertPath;
		this.hpcCertPassword = hpcCertPassword;
		this.userId = userId;
		this.password = password;
		this.logFile = logFile;
		this.errorRecordsFile = errorRecordsFile;
		this.authToken = authToken;
	}

	public boolean processData() throws HpcBatchException {
		boolean success = false;
		List<BlockingQueue<Record>> queueList = new ArrayList<BlockingQueue<Record>>();
		// Create queues
		for (int i = 0; i < threadPoolSize; i++) {
			BlockingQueue<Record> queue = new LinkedBlockingQueue<>();
			queueList.add(queue);
		}
		RoundRobinRecordDispatcher<Record> roundRobinRecordDispatcher = new RoundRobinRecordDispatcher<>(queueList);

		CSVParser csvFileParser = null;
		// Create the CSVFormat object with the header mapping
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();

		try {
			// initialize FileReader object
			FileReader fileReader = new FileReader(inputFileName);

			// initialize CSVParser object
			csvFileParser = new CSVParser(fileReader, csvFileFormat);
		} catch (IOException e) {
			throw new HpcBatchException(
					"Failed to parse input csv file: " + inputFileName + " due to: " + e.getMessage());
		}
		Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
		// Build a master job that will read records from the data source
		// and dispatch them to worker jobs
		Job masterJob = JobBuilder.aNewJob().named("master-job").reader(new ApacheCommonCsvRecordReader(csvFileParser))
				.mapper(new HPCBatchDataFileRecordMapper(HPCDataObject.class, headersMap, basePath, hpcCertPath,
						hpcCertPassword, userId, password, authToken, logFile, errorRecordsFile))
				.dispatcher(roundRobinRecordDispatcher).jobListener(new PoisonRecordBroadcaster<>(queueList)).build();

		// Build worker jobs
		List<Job> jobs = new ArrayList<Job>();
		jobs.add(masterJob);
		for (int i = 0; i < threadPoolSize; i++) {
			Job workerJob = buildWorkerJob(queueList.get(i), "HPC DME Data object registration" + i);
			jobs.add(workerJob);
		}

		// Create a thread pool to call master and worker jobs in parallel
		ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

		// Submit workers to executor service
		try {
			List<Future<JobReport>> reports = executorService.invokeAll(jobs);

			List<JobReport> jobReports = new ArrayList<JobReport>();
			for (int i = 0; i < reports.size(); i++) {
				JobReport jobReport = reports.get(i).get();
				jobReports.add(jobReport);
				// System.out.println(jobReport.toString());
			}

			HPCJobReportMerger reportMerger = new HPCJobReportMerger();
			JobReport finalReport = reportMerger.mergerReports(jobReports);
			if (finalReport.getMetrics().getErrorCount() == 0)
				success = true;
			System.out.println(new HpcJobReportFormatter().formatReport(finalReport));
		} catch (ExecutionException e) {
			throw new HpcBatchException(
					"Failed to process input csv file: " + inputFileName + " due to: " + e.getMessage());
		} catch (InterruptedException e) {
			throw new HpcBatchException(
					"Failed to process input csv file: " + inputFileName + " due to: " + e.getMessage());
		}

		// Shutdown executor service
		executorService.shutdown();
		return success;
	}

	public static Job buildWorkerJob(BlockingQueue<Record> queue, String jobName) {
		return JobBuilder.aNewJob().named(jobName).silentMode(true).reader(new BlockingQueueRecordReader(queue))
				.filter(new PoisonRecordFilter()).processor(new HPCBatchDataFileRecordProcessor()).build();

	}
}
