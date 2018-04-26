package gov.nih.nci.hpc.archivescleaner;

import gov.nih.nci.hpc.archivescleaner.ArchivesCleanerApplication.ResidualItem;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportGenerator {

  private static final int FIXED_SPACE_GAP_WIDTH = 5;
  private static final int QTY_2_TRIGGER_FLUSH = 100;

  private static final String SEVEN_ARG_CONSTRUCTOR_SIGNATURE =
    ReportGenerator.class.getSimpleName() +
    "(List<String> pDocsList," +
    " List<String> pCollDelList," +
    " List<String> pDataObjDelList," +
    " List<ResidualItem> pCollOutList," +
    " List<ResidualItem> pDataObjOutList," +
    " Date pStartTime," +
    " Date pFinishTime)";

  protected Comparator<ResidualItem> riComparatorByPathLen =
    new Comparator<ResidualItem>() {
      @Override
      public int compare(ResidualItem o1, ResidualItem o2) {
        final int o1PathLen = null == o1.itemPath ? 0 : o1.itemPath.length();
        final int o2PathLen = null == o2.itemPath ? 0 : o2.itemPath.length();
        return o1PathLen - o2PathLen;
      }
  };

  private final int sectionSeparatorNumLines = 2;
  private List<String> docsList;
  private List<String> collectionsDeleted;
  private List<String> dataObjectsDeleted;
  private Optional<Date> startTime;
  private Optional<Date> finishTime;
  private Optional<List<ResidualItem>> collectionsOutstanding;
  private Optional<List<ResidualItem>> dataObjectsOutstanding;
  private SimpleDateFormat fileNameTimestampFormat;
  private SimpleDateFormat reportDateFormat;
  private final String defaultReportFileExtension = ".txt";
  private final String defaultReportFilenamePrefix = "archives-clean-report-";
  private final String defaultReportFilenameTimestampPattern =
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private final String defaultReportOutputDir = "C:\\tmp";
  private final String reportDatePattern = "yyyy-MM-dd HH:mm:ss Z";
  private final String reportFirstLineContent =
      "HPC DME Archives Cleaning Report";
  private final String sectionSeparatorLine = "----------------------------------------------------------------------------------------------------";


  public ReportGenerator() {
    final List<String> emptyListOfString = new ArrayList<>();
    this.docsList = new ArrayList<>(emptyListOfString);
    this.collectionsDeleted = new ArrayList<>(emptyListOfString);
    this.dataObjectsDeleted = new ArrayList<>(emptyListOfString);
    this.collectionsOutstanding = Optional.empty();
    this.dataObjectsOutstanding = Optional.empty();
    this.startTime = Optional.empty();
    this.finishTime = Optional.empty();
  }


  public ReportGenerator(
    List<String> pDocsList,
    List<String> pCollDelList,
    List<String> pDataObjDelList,
    List<ResidualItem> pCollOutList,
    List<ResidualItem> pDataObjOutList,
    Date pStartTime,
    Date pFinishTime) {

    this.docsList = null == pDocsList ? Collections.emptyList() : pDocsList;
    this.collectionsDeleted = null == pCollDelList ?
      Collections.emptyList() : pCollDelList;
    this.dataObjectsDeleted = null == pDataObjDelList ?
      Collections.emptyList() : pDataObjDelList;
    this.collectionsOutstanding = (null == pCollOutList) ?
      Optional.empty() : Optional.of(pCollOutList);
    this.dataObjectsOutstanding = (null == pDataObjOutList) ?
      Optional.empty() : Optional.of(pDataObjOutList);
    this.startTime = (null == pStartTime) ?
      Optional.empty() : Optional.of(pStartTime);
    this.finishTime = (null == pFinishTime) ?
      Optional.empty() : Optional.of(pFinishTime);
  }


  public ReportGenerator(
    List<String> pDocsList,
    List<String> pCollDelList,
    List<String> pDataObjDelList,
    List<ResidualItem> pCollOutList,
    List<ResidualItem> pDataObjOutList) {
    this(
      pDocsList,
      pCollDelList,
      pDataObjDelList,
      pCollOutList,
      pDataObjOutList,
      null,
      null);
  }


  public Optional<Date> getStartTime() {
    return startTime;
  }

  public void setStartTime(Optional<Date> startTime) {
    this.startTime = startTime;
  }

  public Optional<Date> getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(Optional<Date> finishTime) {
    this.finishTime = finishTime;
  }

  public List<String> getDocsList() {
    return docsList;
  }

  public void setDocsList(List<String> docsList) {
    this.docsList = docsList;
  }

  public List<String> getCollectionsDeleted() {
    return collectionsDeleted;
  }

  public void setCollectionsDeleted(List<String> collectionsDeleted) {
    this.collectionsDeleted = collectionsDeleted;
  }

  public Optional<List<ResidualItem>> getCollectionsOutstanding() {
    return collectionsOutstanding;
  }

  public void setCollectionsOutstanding(Optional<List<ResidualItem>> collectionsOutstanding) {
    this.collectionsOutstanding = collectionsOutstanding;
  }

  public List<String> getDataObjectsDeleted() {
    return dataObjectsDeleted;
  }

  public void setDataObjectsDeleted(List<String> dataObjectsDeleted) {
    this.dataObjectsDeleted = dataObjectsDeleted;
  }

  public Optional<List<ResidualItem>> getDataObjectsOutstanding() {
    return dataObjectsOutstanding;
  }

  public void setDataObjectsOutstanding(Optional<List<ResidualItem>> dataObjectsOutstanding) {
    this.dataObjectsOutstanding = dataObjectsOutstanding;
  }


  public String generate() {
    return generate(null);
  }


  public String generate(String filePath2Report) {
    validateProperties();

    String fqPathForReportFile = StringUtils.hasText(filePath2Report) ?
      filePath2Report : produceReportFilename();

/*
    File outFile = new File(fqPathForReportFile);
    if (outFile.exists()) {
      throw new RuntimeException(this.getClass().getSimpleName() +
        " will not overwrite existing file at " + outFile.getAbsolutePath() +
        ".  Please specify a non-existent file system location for writing" +
        " report.");
    }
    try {
      outFile.createNewFile();
    } catch (IOException ioe) {
      throw new RuntimeException(this.getClass().getSimpleName() +
        " failed to initiate new file at " + outFile.getAbsolutePath(), ioe);
    }
*/
    FileWriter fWriter = null;
    BufferedWriter buffWriter = null;
    try {
      fWriter = new FileWriter(fqPathForReportFile, false);
      buffWriter = new BufferedWriter(fWriter);

      doReportTopSection(buffWriter);

      insertReportSectionSeparator(buffWriter);
      doReportSummarySection(buffWriter);

      insertReportSectionSeparator(buffWriter);
      doReportCollectionDetailsSection(buffWriter);

      insertReportSectionSeparator(buffWriter);
      doReportDataObjectsDetailsSection(buffWriter);

      buffWriter.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(this.getClass().getSimpleName() +
        " failed to produce report file!", ioe);
    } finally {
      // Any clean up as needed
      try {
        if (null != buffWriter) {
          buffWriter.close();
        }
        if (null != fWriter) {
          fWriter.close();
        }
      } catch (Exception e) {
        // do nothing intentional
      }
    }

//    final String outputFileAbsPath = outFile.getAbsolutePath();
//    outFile = null;
//    return outputFileAbsPath;
    return fqPathForReportFile;
  }


  private void doReportCollectionDetailsSection(BufferedWriter buffWriter) throws IOException {
    buffWriter.newLine();
    buffWriter.write("Following ");
    buffWriter.write(String.valueOf(this.collectionsDeleted.size()));
    buffWriter.write(" Collections were successfully deleted:");
    buffWriter.newLine();
    int largerQty = Math.max(this.collectionsDeleted.size(),
      this.collectionsOutstanding.orElse(Collections.emptyList()).size());
    int desiredWidth1 = String.valueOf(largerQty).length() +
      FIXED_SPACE_GAP_WIDTH;
    String rpadPattern1 = "%1$-" + desiredWidth1 + "s";
    int counter = 1;
    for (String aDeletedColl : this.collectionsDeleted) {
      buffWriter.write(String.format(rpadPattern1, counter + "."));
      buffWriter.write(aDeletedColl);
      buffWriter.newLine();
      counter += 1;
    }
    if (this.collectionsOutstanding.isPresent() &&
        !this.collectionsOutstanding.get().isEmpty()) {
      buffWriter.newLine();
      buffWriter.write("Following ");
      buffWriter.write(String.valueOf(this.collectionsOutstanding.get().size()));
      buffWriter.write(" Collections could not be deleted:");
      buffWriter.newLine();
      buffWriter.flush();
      counter = 1;
      int desiredWidth2 = Collections.max(this.collectionsOutstanding.get(),
        this.riComparatorByPathLen).itemPath.length() + FIXED_SPACE_GAP_WIDTH;
      String rpadPattern2 = "%1$-" + desiredWidth2 + "s";
      StringBuilder sb = new StringBuilder();
      for (ResidualItem ri : this.collectionsOutstanding.get()) {
        sb.append(String.format(rpadPattern1, counter + "."))
          .append(String.format(rpadPattern2, ri.itemPath))
          .append("|");
        for (int j = 0; j < FIXED_SPACE_GAP_WIDTH; j += 1) {
          sb.append(" ");
        }
        sb.append("Info: ")
          .append(ri.serviceResponse);
        String residualCollLine = sb.toString();
        sb.setLength(0);
        buffWriter.write(residualCollLine);
        buffWriter.newLine();
        counter += 1;
        if (counter > 1 && counter % QTY_2_TRIGGER_FLUSH == 0) {
          buffWriter.flush();
        }
      }
    }
    buffWriter.newLine();
    buffWriter.flush();
  }


  private void doReportDataObjectsDetailsSection(BufferedWriter buffWriter) throws IOException {
    buffWriter.newLine();
    buffWriter.write("Following ");
    buffWriter.write(String.valueOf(this.dataObjectsDeleted.size()));
    buffWriter.write(" Data Objects were successfully deleted:");
    buffWriter.newLine();
    int largerQty = Math.max(this.dataObjectsDeleted.size(),
      this.dataObjectsOutstanding.orElse(Collections.emptyList()).size());
    int desiredWidth1 = String.valueOf(largerQty).length() +
      FIXED_SPACE_GAP_WIDTH;
    String rpadPattern1 = "%1$-" + desiredWidth1 + "s";
    int counter = 1;
    for (String aDeletedDataObj : this.dataObjectsDeleted) {
      buffWriter.write(String.format(rpadPattern1, counter + "."));
      buffWriter.write(aDeletedDataObj);
      buffWriter.newLine();
      counter += 1;
    }
    if (this.dataObjectsOutstanding.isPresent() &&
        !this.dataObjectsOutstanding.get().isEmpty()) {
      buffWriter.newLine();
      buffWriter.write("Following ");
      buffWriter.write(String.valueOf(this.dataObjectsOutstanding.get().size()));
      buffWriter.write(" Data Objects could not be deleted:");
      buffWriter.newLine();
      buffWriter.flush();
      counter = 1;
      int desiredWidth2 = Collections.max(this.dataObjectsOutstanding.get(),
        this.riComparatorByPathLen).itemPath.length() + FIXED_SPACE_GAP_WIDTH;
      String rpadPattern2 = "%1$-" + desiredWidth2 + "s";
      StringBuilder sb = new StringBuilder();
      for (ResidualItem ri : this.dataObjectsOutstanding.get()) {
        sb.append(String.format(rpadPattern1, counter + "."))
          .append(String.format(rpadPattern2, ri.itemPath))
          .append("|");
        for (int j = 0; j < FIXED_SPACE_GAP_WIDTH; j += 1) {
          sb.append(" ");
        }
        sb.append("Info: ")
          .append(ri.serviceResponse);
        String residualDataObjLine = sb.toString();
        sb.setLength(0);
        buffWriter.write(residualDataObjLine);
        buffWriter.newLine();
        counter += 1;
        if (counter > 1 && counter % QTY_2_TRIGGER_FLUSH == 0) {
          buffWriter.flush();
        }
      }
    }
    buffWriter.newLine();
    buffWriter.flush();
  }


  private void doReportSummarySection(BufferedWriter buffWriter) throws IOException {
    final int numOutstandColls = this.collectionsOutstanding.isPresent() ?
        this.collectionsOutstanding.get().size() : 0;

    final int totalColls = this.collectionsDeleted.size() + numOutstandColls;

    final int numOutstandDataObjs = this.dataObjectsOutstanding.isPresent() ?
        this.dataObjectsOutstanding.get().size() : 0;

    final int totalDataObjs = this.dataObjectsDeleted.size() + numOutstandDataObjs;

    // 2 spaces per indent
    final String indent = "  ";

    buffWriter.newLine();
    buffWriter.write("Statistics:");
    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Collections Identified   =  ");
    buffWriter.write(String.valueOf(totalColls));

    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Collections Deleted      =  ");
    buffWriter.write(String.valueOf(this.collectionsDeleted.size()));

    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Collections Outstanding  =  ");
    buffWriter.write(String.valueOf(numOutstandColls));

    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Data Objects Identified   =  ");
    buffWriter.write(String.valueOf(totalDataObjs));

    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Data Objects Deleted      =  ");
    buffWriter.write(String.valueOf(this.dataObjectsDeleted.size()));

    buffWriter.newLine();

    buffWriter.write(indent);
    buffWriter.write("# Data Objects Outstanding  =  ");
    buffWriter.write(String.valueOf(numOutstandDataObjs));

    buffWriter.newLine();
    buffWriter.newLine();

    buffWriter.flush();
  }


  private void doReportTopSection(BufferedWriter buffWriter) throws IOException {
    buffWriter.write(this.reportFirstLineContent);

    buffWriter.newLine();
    buffWriter.newLine();

    StringBuilder sb = new StringBuilder();
    for (String someDoc : this.docsList) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(someDoc);
    }
    if (this.docsList.size() > 1) {
      buffWriter.write("Targeted DOCs: ");
    } else {
      buffWriter.write("Targeted DOC: ");
    }
    String docsListing = sb.toString();
    sb.setLength(0);
    buffWriter.write(docsListing);

    buffWriter.newLine();
    buffWriter.newLine();

    if (this.startTime.isPresent() && this.finishTime.isPresent()) {
      if (null == this.reportDateFormat) {
        this.reportDateFormat = new SimpleDateFormat(this.reportDatePattern);
      }
      sb.append("Clean started at ")
        .append(this.reportDateFormat.format(this.startTime.get()))
        .append(" and finished at ")
        .append(this.reportDateFormat.format(this.finishTime.get()));
      String startFinishTimingInfo = sb.toString();
      buffWriter.write(startFinishTimingInfo);
      buffWriter.newLine();
      buffWriter.newLine();
      sb.setLength(0);
    }

    buffWriter.flush();
  }


  private void insertReportSectionSeparator(BufferedWriter buffWriter) throws IOException {
    for (int i = 0; i < this.sectionSeparatorNumLines; i++) {
      buffWriter.write(this.sectionSeparatorLine);
      buffWriter.newLine();
    }
    buffWriter.flush();
  }


  private String produceReportFilename() {
    if (null == this.fileNameTimestampFormat) {
      this.fileNameTimestampFormat = new SimpleDateFormat(
        this.defaultReportFilenameTimestampPattern);
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(this.defaultReportOutputDir)
      .append(File.separator)
      .append(this.defaultReportFilenamePrefix)
      .append(this.fileNameTimestampFormat.format(new Date()))
      .append(this.defaultReportFileExtension);
    final String retFilename = sb.toString();
    return retFilename;
  }


  private void validateProperties() {
    if (null == this.docsList || this.docsList.isEmpty()) {
      throw new RuntimeException("To generate report, " +
        ReportGenerator.class.getSimpleName() +
        " requires non-null, non-empty value for property docsList!");
    }

    if (null == this.collectionsDeleted) {
      throw new RuntimeException("To generate report, " +
        ReportGenerator.class.getSimpleName() +
        " requires non-null value for property collectionsDeleted!");
    }

    if (null == this.dataObjectsDeleted) {
      throw new RuntimeException("To generate report, " +
        ReportGenerator.class.getSimpleName() +
        " requires non-null, non-empty value for property dataObjectsDeleted!");
    }
  }

}
