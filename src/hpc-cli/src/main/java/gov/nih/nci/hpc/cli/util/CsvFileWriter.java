/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import gov.nih.nci.hpc.cli.domain.HPCCollectionRecord;
import gov.nih.nci.hpc.cli.domain.HPCDataFileRecord;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;

public class CsvFileWriter {

	// Delimiter used in CSV file
	private static final String NEW_LINE_SEPARATOR = "\n";

	public static void writeCollectionsCsvFile(String fileName, String[] header,
			List<HPCCollectionRecord> collecitons) {

		FileWriter fileWriter = null;

		CSVPrinter csvFilePrinter = null;

		// Create the CSVFormat object with "\n" as a record delimiter
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);

		try {

			// initialize FileWriter object
			fileWriter = new FileWriter(fileName);

			// initialize CSVPrinter object
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

			// Create CSV file header
			csvFilePrinter.printRecord(header);

			// Write a new student object list to the CSV file
			for (HPCCollectionRecord collection : collecitons) {
				List<String> collectionDataRecord = new ArrayList<String>();
				collectionDataRecord.add(collection.getCollectionId());
				collectionDataRecord.add(collection.getAbsolutePath());
				collectionDataRecord.add(collection.getCollectionParentName());
				collectionDataRecord.add(collection.getCreatedAt());
				collectionDataRecord.add(collection.getModifiedAt());
				for (int i = 5; i < header.length; i++) {
					HpcMetadataEntry entry = collection.getMetadataAttrs().get(header[i]);
					if (entry == null)
						collectionDataRecord.add("");
					else
						collectionDataRecord.add(entry.getValue());
				}

				csvFilePrinter.printRecord(collectionDataRecord);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
			}
		}
	}

	public static void writeDatafilesCsvFile(String fileName, String[] header, List<HPCDataFileRecord> datafiles) {

		FileWriter fileWriter = null;

		CSVPrinter csvFilePrinter = null;

		// Create the CSVFormat object with "\n" as a record delimiter
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);

		try {

			// initialize FileWriter object
			fileWriter = new FileWriter(fileName);

			// initialize CSVPrinter object
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

			// Create CSV file header
			csvFilePrinter.printRecord(header);

			// Write a new student object list to the CSV file
			for (HPCDataFileRecord datafile : datafiles) {
				List<String> fileDataRecord = new ArrayList<String>();
				fileDataRecord.add(datafile.getDataFileId());
				fileDataRecord.add(datafile.getCollectionId());
				fileDataRecord.add(datafile.getAbsolutePath());
				fileDataRecord.add(datafile.getCollectionName());
				fileDataRecord.add(datafile.getCreatedAt());
				fileDataRecord.add(datafile.getModifiedAt());
				for (int i = 6; i < header.length; i++) {
					HpcMetadataEntry entry = datafile.getMetadataAttrs().get(header[i]);
					if (entry == null)
						fileDataRecord.add("");
					else
						fileDataRecord.add(entry.getValue());
				}

				csvFilePrinter.printRecord(fileDataRecord);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
			}
		}
	}

	public static void writePathsCsvFile(String fileName, String[] header, List<String> paths) {

		FileWriter fileWriter = null;

		CSVPrinter csvFilePrinter = null;

		// Create the CSVFormat object with "\n" as a record delimiter
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);

		try {

			// initialize FileWriter object
			fileWriter = new FileWriter(fileName);

			// initialize CSVPrinter object
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

			// Create CSV file header
			csvFilePrinter.printRecord(header);

			// Write a new student object list to the CSV file
			for (String path : paths) {
				csvFilePrinter.printRecord(path);
			}

			System.out.println("CSV file was created successfully !!!");

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
			}
		}
	}
}
