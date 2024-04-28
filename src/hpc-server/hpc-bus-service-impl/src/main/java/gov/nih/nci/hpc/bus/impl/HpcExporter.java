/**
 * HpcExporter.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;

/**
 * <p>
 * HPC Exporter. 
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcExporter
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	/** Default Constructor for spring dependency injection. */
	private HpcExporter() {
	}
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
   
    /**
     * Export collection list.
     * 
     * @param text The text to encrypt.
     */
	public void exportCollections(String exportFileName, HpcCollectionListDTO collectionsDTO,
			List<String> deselectedColumns) throws IOException {
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
		List<String> headers = new ArrayList<String>();
		if (CollectionUtils.isNotEmpty(collectionsDTO.getCollections()) ||
				CollectionUtils.isNotEmpty(collectionsDTO.getCollectionPaths())) {
			List<List<String>> rows = new ArrayList<>();
			headers.add("path");
			if (deselectedColumns != null && !deselectedColumns.contains(("createdOn")))
				headers.add("created_on");
			if (deselectedColumns != null && !deselectedColumns.contains(("uniqueId")))
              headers.add("uuid");
			if (deselectedColumns != null && !deselectedColumns.contains(("registeredBy")))
              headers.add("registered_by");
			if (deselectedColumns != null && !deselectedColumns.contains(("collectionType")))
              headers.add("collection_type");
          
			// For non-detailed search
			for (String path: collectionsDTO.getCollectionPaths()) {
				List<String> result = new ArrayList<String>();
				result.add(path);
				rows.add(result);
			}
			// For detailed search
			for (HpcCollectionDTO collection : collectionsDTO.getCollections()) {
				List<String> result = new ArrayList<String>();
				result.add(collection.getCollection().getAbsolutePath());
				if(headers.contains("created_on"))
					result.add(format.format(collection.getCollection().getCreatedAt().getTime()));
				if (collection != null && collection.getMetadataEntries() != null) {
					List<HpcMetadataEntry> combinedMetadataEntries = new ArrayList<>();
					combinedMetadataEntries.addAll(collection.getMetadataEntries().getSelfMetadataEntries());
					combinedMetadataEntries.addAll(collection.getMetadataEntries().getParentMetadataEntries());
					for(HpcMetadataEntry entry : combinedMetadataEntries) {
						if(!headers.contains(entry.getAttribute()) && deselectedColumns == null || 
						   !headers.contains(entry.getAttribute()) && deselectedColumns != null && !deselectedColumns.contains(entry.getAttribute()))
							headers.add(entry.getAttribute());
					}
					for (String header : headers) {
						boolean found = false;
						for (HpcMetadataEntry entry : combinedMetadataEntries) {
							if ((header != null) && (entry != null) && (entry.getAttribute() != null) && header.equals(entry.getAttribute())) {
								result.add(entry.getValue());
								found = true;
								break;
							}
						}
						if(!found && (header == null)){
							result.add("");
						} else if (!found && !header.equals("path") && !header.equals("created_on")){
							result.add("");
						}
					}
				}
				rows.add(result);
			}
			export(exportFileName, headers, rows); 
		}
		
	}
	
	/**
     * Export data object list.
     * 
     * @param text The text to encrypt.
     */
	public void exportDataObjects(String exportFileName, HpcDataObjectListDTO dataObjectsDTO,
			List<String> deselectedColumns) throws IOException {
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
		List<String> headers = new ArrayList<String>();
		if (CollectionUtils.isNotEmpty(dataObjectsDTO.getDataObjects()) ||
				CollectionUtils.isNotEmpty(dataObjectsDTO.getDataObjectPaths())) {
			List<List<String>> rows = new ArrayList<>();
			headers.add("path");
			if (deselectedColumns != null && !deselectedColumns.contains(("createdOn")))
              headers.add("created_on");
            if (deselectedColumns != null && !deselectedColumns.contains(("uniqueId")))
              headers.add("uuid");
            if (deselectedColumns != null && !deselectedColumns.contains(("registeredBy")))
              headers.add("registered_by");

			// For non-detailed search
			for (String path: dataObjectsDTO.getDataObjectPaths()) {
				List<String> result = new ArrayList<String>();
				result.add(path);
				rows.add(result);
			}
			// For detailed search
			for (HpcDataObjectDTO datafile : dataObjectsDTO.getDataObjects()) {
				List<String> result = new ArrayList<String>();
				result.add(datafile.getDataObject().getAbsolutePath());
				if(headers.contains("created_on"))
					result.add(format.format(datafile.getDataObject().getCreatedAt().getTime()));
				if (datafile != null && datafile.getMetadataEntries() != null) {
					List<HpcMetadataEntry> combinedMetadataEntries = new ArrayList<>();
					combinedMetadataEntries.addAll(datafile.getMetadataEntries().getSelfMetadataEntries());
					combinedMetadataEntries.addAll(datafile.getMetadataEntries().getParentMetadataEntries());
					for(HpcMetadataEntry entry : combinedMetadataEntries) {
                      if(!headers.contains(entry.getAttribute()) && deselectedColumns == null || 
                         !headers.contains(entry.getAttribute()) && deselectedColumns != null && !deselectedColumns.contains(entry.getAttribute()))
                          headers.add(entry.getAttribute());
                    }
					for (String header : headers) {
						boolean found = false;
						for (HpcMetadataEntry entry : combinedMetadataEntries) {
							if ((header != null) && (entry != null) && (entry.getAttribute() != null) && header.equals(entry.getAttribute())) {
								result.add(entry.getValue());
								found = true;
								break;
							}
						}
						if(!found && (header == null)){
							result.add("");
						} else if(!found && !header.equals("path") && !header.equals("created_on")) {
							result.add("");
						}
					}
				}
				rows.add(result);
			}
			
			export(exportFileName, headers, rows); 
		}
		
	}
	
    /**
     * Export excel.
     * 
     * @param text The text to encrypt.
     */
		private void export(String filename, List<String> headers, List<List<String>> data) throws IOException, OutOfMemoryError  {
			XSSFWorkbook wb = new XSSFWorkbook();
			FileOutputStream outputStream = new FileOutputStream(filename);
			try {
				XSSFSheet s = wb.createSheet("Sheet1");
				Row r = null;

				int rownum = 0;
				r = s.createRow(rownum++); // header row
				int cellnum = 0;
				int i = 0;
				for (Object h : headers) {
					r.createCell(cellnum++).setCellValue(StringUtils.defaultString((String)h));
				}
				logger.debug("Header size=" + headers.size());
				try {
					for (List<String> row : data) {
						cellnum = 0;
						r = s.createRow(rownum++);
						for (String cell : row) {
							r.createCell(cellnum++).setCellValue(cell);
						}
						if(rownum % 1000 == 0) {
							logger.debug("rownum=" + rownum);
						}
					}
				} catch (Exception e) {
					logger.error("Error creating row data", e);
			 	}
				try {
					wb.write(outputStream);
				} catch (Exception e) {
					logger.error("Error writing to workbook", e);
			 	}
			} catch (OutOfMemoryError e) {
				logger.error("OutOfMemoryError", e);
				wb.close();
				outputStream.close();
			} finally {
				try {
					wb.close();
					outputStream.close();
				} catch (IOException e) {
					logger.error("Error closing excel workbook {}", filename, e);
				}
			}
		}
}

 