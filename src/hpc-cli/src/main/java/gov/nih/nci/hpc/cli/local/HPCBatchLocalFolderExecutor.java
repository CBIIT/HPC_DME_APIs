/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import gov.nih.nci.hpc.cli.HPCJobReportMerger;
import gov.nih.nci.hpc.cli.HpcJobReportFormatter;
import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.exception.HpcException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.easybatch.core.dispatcher.PoisonRecordBroadcaster;
import org.easybatch.core.dispatcher.RoundRobinRecordDispatcher;
import org.easybatch.core.filter.PoisonRecordFilter;
import org.easybatch.core.job.Job;
import org.easybatch.core.job.JobBuilder;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.job.JobResult;
import org.easybatch.core.job.JobStatus;
import org.easybatch.core.processor.RecordProcessingException;
import org.easybatch.core.reader.BlockingQueueRecordReader;
import org.easybatch.core.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HPCBatchLocalFolderExecutor {

  protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
  String inputFileName;
  int threadPoolSize = 1;
  private String errorRecordsFile;
  private String logFile;
  Map<String, String> criteriaMap;
  private HpcServerConnection connection;
  private int maxAttempts;
  private long backOffPeriod;

  public HPCBatchLocalFolderExecutor(Map<String, String> criteriaMap,
      HpcServerConnection connection, String logFile,
      String errorRecordsFile, String authToken, int maxAttempts, long backOffPeriod) {
    this.logFile = logFile;
    this.errorRecordsFile = errorRecordsFile;
    this.criteriaMap = criteriaMap;
    this.connection = connection;
    this.maxAttempts = maxAttempts;
    this.backOffPeriod = backOffPeriod;
  }

  public String processData() throws HpcBatchException {
    ExecutorService executorService = null;
    boolean success = false;
    boolean testRun = false;
    boolean metadataOnly = false;
    boolean checksum = true;
    boolean confirmation = true;
    String localPath = (String) criteriaMap.get("filePath");
    String fileList = (String) criteriaMap.get("fileList");
    String excludePattern = (String) criteriaMap.get("excludePatternFile");
    String includePattern = (String) criteriaMap.get("includePatternFile");
    String destinationBasePath = criteriaMap.get("destinationBasePath");
    String fileBasePath = criteriaMap.get("filePathBaseName");
    String threadStr = (String) criteriaMap.get("threads");
    logger.debug("localPath "+localPath);
    logger.debug("fileList "+fileList);
    logger.debug("excludePattern "+excludePattern);
    logger.debug("includePattern "+includePattern);
    logger.debug("destinationBasePath "+destinationBasePath);
    logger.debug("fileBasePath "+fileBasePath);
    logger.debug("threadStr "+threadStr);
    try {
      if (threadStr != null) {
        threadPoolSize = Integer.parseInt(threadStr);
      }
    } catch (NumberFormatException e) {
      logger.error(e.getMessage(), e);
      System.out.println("Failed to process number of threads input: " + threadStr);
      generateJobReport(null, Constants.CLI_2);
      return Constants.CLI_2;
    }
    if (criteriaMap.get("metadata") != null && criteriaMap.get("metadata")
        .equalsIgnoreCase("true")) {
      metadataOnly = true;
    }

    if (criteriaMap.get("test") != null && criteriaMap.get("test").equalsIgnoreCase("true")) {
      testRun = true;
    }

    if (criteriaMap.get("confirm") != null && criteriaMap.get("confirm")
        .equalsIgnoreCase("false")) {
      confirmation = false;
    }

    if (criteriaMap.get("checksum") != null && criteriaMap.get("checksum")
        .equalsIgnoreCase("false")) {
      checksum = false;
    }

    try {
      HpcLocalDirectoryListQuery impl = new HpcLocalDirectoryListQuery();
      List<String> excludePatterns = readPatternStringsfromFile(excludePattern);
      List<String> includePatterns = readPatternStringsfromFile(includePattern);
      List<HpcPathAttributes> paths = null;
      if (fileList != null) {
        paths = impl
            .getFileListPathAttributes(localPath, fileList, excludePatterns, includePatterns);
      } else {
        paths = impl.getPathAttributes(localPath, excludePatterns, includePatterns);
      }
      if (testRun) {
        generateJobReport(null, Constants.CLI_SUCCESS);
        return Constants.CLI_SUCCESS;
      }

      List<HpcPathAttributes> folders = new ArrayList<HpcPathAttributes>();
      List<HpcPathAttributes> files = new ArrayList<HpcPathAttributes>();
      if (paths.isEmpty()) {
        System.out.println("No files/folders found!");
        generateJobReport(null, Constants.CLI_3);
        return Constants.CLI_3;
      } else {
        for (HpcPathAttributes pathAttr : paths) {
          if (pathAttr.getIsDirectory()) {
            folders.add(pathAttr);
          } else {
            files.add(pathAttr);
          }
        }
      }
      
      Collections.sort(folders);
      Collections.sort(files);
      logger.debug("folders "+folders);
      logger.debug("files "+files);
      if (confirmation) {
        jline.console.ConsoleReader reader;
        try {
          reader = new jline.console.ConsoleReader();
          reader.setExpandEvents(false);
          System.out.println("Are you sure you want to register this directory? (Y/N):");
          String confirm = reader.readLine();
          if (confirm != null && !confirm.equalsIgnoreCase("Y")) {
            System.out.println("Skipped registering data files!");
            generateJobReport(null, Constants.CLI_3);
            return Constants.CLI_3;
          }
        } catch (IOException e) {
          System.out.println("Failed to get confirmation " + e.getMessage());
          generateJobReport(null, Constants.CLI_2);
          return Constants.CLI_2;
        }
      }

      // Process all folders first
      for (HpcPathAttributes folder : folders) {
        HpcLocalFolderProcessor folderProcessor;
        try {
          folderProcessor = new HpcLocalFolderProcessor(connection);
          folderProcessor.process(folder, localPath, fileBasePath, destinationBasePath, logFile,
              errorRecordsFile,
              metadataOnly, true, checksum);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          System.out.println("Failed to process collection " + folder.getAbsolutePath()
              + " registration due to: " + e.getMessage());
          generateJobReport(null, Constants.CLI_4);
          return Constants.CLI_4;
        } catch (RecordProcessingException e) {
          logger.error(e.getMessage(), e);
          System.out.println("Failed to process collection " + folder.getAbsolutePath()
              + " registration due to: " + e.getMessage());
          generateJobReport(null, Constants.CLI_4);
          return Constants.CLI_4;
        }
      }
      //Measure appx transfer speed
      //Get total size of the files and get total time to process
      long filesSize = 0L;
      for (HpcPathAttributes fileAttr : files) {
		File file = new File(fileAttr.getPath());
        //File file = new File(Paths.generateFileSystemResourceUri(fileAttr.getPath()));
        if (file.exists()) {
          filesSize += file.length();
        }
      }
      Long start = System.currentTimeMillis();

      // Process all files with multiple threads
      List<BlockingQueue<Record>> queueList = new ArrayList<BlockingQueue<Record>>();
      // Create queues
      for (int i = 0; i < threadPoolSize; i++) {
        BlockingQueue<Record> queue = new LinkedBlockingQueue<>();
        queueList.add(queue);
      }
      RoundRobinRecordDispatcher<Record> roundRobinRecordDispatcher = new RoundRobinRecordDispatcher<>(
          queueList);
      // Build a master job that will read records from the data source
      // and dispatch them to worker jobs
      Job masterJob = JobBuilder.aNewJob().named("master-job")
          .reader(new HPCBatchLocalRecordReader(files))
          .mapper(new HPCBatchLocalFileRecordMapper(HPCDataObject.class, criteriaMap, connection,
              logFile,
              errorRecordsFile,
              maxAttempts,
              backOffPeriod))
          .dispatcher(roundRobinRecordDispatcher)
          .jobListener(new PoisonRecordBroadcaster<>(queueList))
          .build();

      // Build worker jobs
      List<Job> jobs = new ArrayList<Job>();
      jobs.add(masterJob);
      for (int i = 0; i < threadPoolSize; i++) {
        Job workerJob = buildWorkerJob(queueList.get(i), "HPC DME Data object registration" + i);
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
        // System.out.println(jobReport.toString());
      }

      success = generateJobReport(jobReports, null);
      Long stop = System.currentTimeMillis();
      long secs = (stop - start) / 1000;
      if (filesSize > 0) {
        System.out.println("Total bytes attempted: " + filesSize);
        if(secs > 0)
        	System.out.println("Average processing speed: " + (filesSize / secs) + " (bytes/sec)");
      }
    } catch (ExecutionException e) {
      logger.error(e.getMessage(), e);
      System.out.println("Failed to process file registration due to: " + e.getMessage());
      generateJobReport(null, Constants.CLI_5);
      return Constants.CLI_5;
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
      System.out.println("Failed to process file registration due to: " + e.getMessage());
      generateJobReport(null, Constants.CLI_5);
      return Constants.CLI_5;
    } catch (HpcException e) {
      logger.error(e.getMessage(), e);
      System.out.println("Failed to process file registration due to: " + e.getMessage());
      generateJobReport(null, Constants.CLI_5);
      return Constants.CLI_5;
    }

    // Shutdown executor service
    if (executorService != null) {
      executorService.shutdown();
    }
    return Constants.CLI_SUCCESS;
  }

  private boolean generateJobReport(List<JobReport> jobReports, String returnCode) {
    if (jobReports == null || jobReports.isEmpty()) {
      jobReports = new ArrayList<>();
      JobReport report = new JobReport();
      if (returnCode.equals(Constants.CLI_SUCCESS)) {
        report.setStatus(JobStatus.COMPLETED);
      } else {
        report.setStatus(JobStatus.ABORTED);
      }
      jobReports.add(report);
    }

    HPCJobReportMerger reportMerger = new HPCJobReportMerger();
    JobReport finalReport = reportMerger.mergerReports(jobReports);
    finalReport
        .setJobResult(new JobResult(returnCode == null ? Constants.CLI_SUCCESS : returnCode));
    for (JobReport report : jobReports) {
      if (report.getMetrics().getErrorCount() != 0) {
        finalReport.setJobResult(new JobResult(Constants.CLI_5));
        break;
      }
    }

    System.out.println(new HpcJobReportFormatter().formatReport(finalReport));
    if (finalReport.getMetrics().getErrorCount() == 0) {
      return true;
    } else {
      return false;
    }
  }

  private List<String> readPatternStringsfromFile(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return null;
    }
    BufferedReader reader = null;
    List<String> patterns = new ArrayList<String>();
    try {
      reader = new BufferedReader(new FileReader(fileName));
      String line;
      while ((line = reader.readLine()) != null) {
        patterns.add(line);
      }

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new HpcCmdException(
          "Failed to read include/exclude pattern file due to: " + e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    return patterns;
  }

  public static Job buildWorkerJob(BlockingQueue<Record> queue, String jobName) {
    return JobBuilder.aNewJob().named(jobName).silentMode(true)
        .reader(new BlockingQueueRecordReader(queue))
        // .filter(new PoisonRecordFilter()).build();
        .filter(new PoisonRecordFilter()).processor(new HPCBatchLocalFileRecordProcessor()).build();

  }
}
