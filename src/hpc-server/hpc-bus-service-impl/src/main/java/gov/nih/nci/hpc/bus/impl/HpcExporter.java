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

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			List<String> selectedColumns) throws IOException {
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
		List<String> headers = new ArrayList<String>();
		if (CollectionUtils.isNotEmpty(collectionsDTO.getCollections()) ||
				CollectionUtils.isNotEmpty(collectionsDTO.getCollectionPaths())) {
			List<List<String>> rows = new ArrayList<>();
			headers.add("path");
			if (selectedColumns != null && selectedColumns.contains(("createdOn")))
				headers.add("created_on");
			for (String selectedColumn : selectedColumns) {
				if (selectedColumn.equals("uniqueId"))
					headers.add("uuid");
				else if (selectedColumn.equals("registeredBy"))
					headers.add("registered_by");
				else if (selectedColumn.equals("collectionType"))
					headers.add("collection_type");
				else if (!selectedColumn.equals("path") && !selectedColumn.equals("createdOn") && !selectedColumn.equals("download")
						&& !selectedColumn.equals("permission"))
					headers.add(selectedColumn);
			}
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
					if(selectedColumns != null && selectedColumns.isEmpty()) {
						for(HpcMetadataEntry entry : combinedMetadataEntries) {
							if(!headers.contains(entry.getAttribute()))
								headers.add(entry.getAttribute());
						}
					}
					for (String header : headers) {
						boolean found = false;
						for (HpcMetadataEntry entry : combinedMetadataEntries) {
							if (header.equals(entry.getAttribute())) {
								result.add(entry.getValue());
								found = true;
								break;
							}
						}
						if(!found && !header.equals("path") && !header.equals("created_on"))
							result.add("");
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
			List<String> selectedColumns) throws IOException {
		
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
		List<String> headers = new ArrayList<String>();
		if (CollectionUtils.isNotEmpty(dataObjectsDTO.getDataObjects()) ||
				CollectionUtils.isNotEmpty(dataObjectsDTO.getDataObjectPaths())) {
			List<List<String>> rows = new ArrayList<>();
			headers.add("path");
			if (selectedColumns != null && selectedColumns.contains(("createdOn")))
				headers.add("created_on");
			for (String selectedColumn : selectedColumns) {
				if (selectedColumn.equals("uniqueId"))
					headers.add("uuid");
				else if (selectedColumn.equals("registeredBy"))
					headers.add("registered_by");
				else if (!selectedColumn.equals("path") && !selectedColumn.equals("createdOn") && !selectedColumn.equals("download")
						&& !selectedColumn.equals("permission") && !selectedColumn.equals("link"))
					headers.add(selectedColumn);
			}
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
					if(selectedColumns != null && selectedColumns.isEmpty()) {
						for(HpcMetadataEntry entry : combinedMetadataEntries) {
							if(!headers.contains(entry.getAttribute()))
								headers.add(entry.getAttribute());
						}
					}
					for (String header : headers) {
						boolean found = false;
						for (HpcMetadataEntry entry : combinedMetadataEntries) {
							if (header.equals(entry.getAttribute())) {
								result.add(entry.getValue());
								found = true;
								break;
							}
						}
						if(!found && !header.equals("path") && !header.equals("created_on"))
							result.add("");
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
    private void export(String filename, List<String> headers, List<List<String>> data) throws IOException {
    	

        Workbook wb = new HSSFWorkbook();
        try (FileOutputStream outputStream = new FileOutputStream(filename); ) {
	        Sheet s = wb.createSheet();
	        Row r = null;
	
	        int rownum = 0;
	        r = s.createRow(rownum++); // header row
	        int cellnum = 0;
	        for(String h: headers) {
	            r.createCell(cellnum++).setCellValue(StringUtils.defaultString(h));
	        }
	
	        for(List<String> row: data) {
	            cellnum = 0;
	            r = s.createRow(rownum++);
	            for(String cell: row) {
	                r.createCell(cellnum++).setCellValue(cell);
	            }
	        }
        
        	wb.write(outputStream);
        } finally {
            try {
            	wb.close();
            } catch (IOException e) {
            	logger.error("Error closing excel workbook {}", filename, e);
            }
        }
    }
}

 