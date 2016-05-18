package gov.nih.nci.hpc.cli.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
 
public class HpcCSVFileWriter{
 
 private static HpcCSVFileWriter writer=new HpcCSVFileWriter();
 
 public synchronized void writeRecord(String logFileName, CSVRecord record, Map<String, Integer> headers){
 
	 FileWriter fileRecordWriter = null;
	 CSVPrinter csvFilePrinter = null;
    try {
		File logFile = new File(logFileName);
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			fileRecordWriter = new FileWriter(logFile, true);
			csvFilePrinter = new CSVPrinter(fileRecordWriter, csvFileFormat);
			for (Entry<String, Integer> entry : headers.entrySet()) {
				csvFilePrinter.print(record.get(entry.getKey()));
			}
			csvFilePrinter.flush();
		} catch (IOException e) {
			System.out.println("Failed to write batch record into the log: " + e.getMessage());
			e.printStackTrace();
		}
    }finally{
      try {
 
    	  fileRecordWriter.close();
    	  csvFilePrinter.close();
 
      } catch (IOException e) {
 
               e.printStackTrace();
 
     }
  }
 
 }
 
public static HpcCSVFileWriter getInstance(){
 
     return writer;
 
 }
 
}