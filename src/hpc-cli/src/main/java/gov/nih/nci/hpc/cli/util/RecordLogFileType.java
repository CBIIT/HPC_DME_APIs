package gov.nih.nci.hpc.cli.util;

public enum RecordLogFileType {
    CSV("csv", ".csv"),
    JSON("json", ".json"),
    TEXT("txt", ".txt");

  private static final String REGEX_FILE_NAME_WITH_EXTENSION = "^\\p{Print}+\\.\\p{Alnum}+$";

  public static String appendFileExtensionIfNeeded(String argFileName, String argFileType) {
    String retVal = null;
    if (null == argFileName || argFileName.isEmpty() || argFileName
        .matches(REGEX_FILE_NAME_WITH_EXTENSION)) {
      retVal = argFileName;
    } else {
      retVal = argFileName.concat(RecordLogFileType.lookupFileExtensionOfFileType(argFileType));
    }
    return retVal;
  }

    public static String lookupFileExtensionOfFileType(String fileType) {
      String retFileExtension = null;
      if (RecordLogFileType.CSV.getTypeName().equalsIgnoreCase(fileType)) {
        retFileExtension = RecordLogFileType.CSV.getFileExtension();
      } else if (RecordLogFileType.JSON.getTypeName().equalsIgnoreCase(fileType)) {
        retFileExtension = RecordLogFileType.JSON.getFileExtension();
      } else {
        retFileExtension = RecordLogFileType.TEXT.getFileExtension();
      }
      return retFileExtension;
    }

    private final String typeName;
    private final String fileExtension;

    RecordLogFileType(String argTypeName, String argFileExtension) {
      this.typeName = argTypeName;
      this.fileExtension = argFileExtension;
    }

    public String getTypeName() {
      return this.typeName;
    }

    public String getFileExtension() {
      return this.fileExtension;
    }
}
