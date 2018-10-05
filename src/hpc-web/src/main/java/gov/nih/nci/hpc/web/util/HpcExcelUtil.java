package gov.nih.nci.hpc.web.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.format.CellDateFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.web.HpcWebException;

public class HpcExcelUtil {
	public static final String METADATA_SHEET = "Metadata";
	public static final String TOKENS_SHEET = "Tokens";

	public static HpcBulkMetadataEntries parseBulkMatadataEntries(MultipartFile metadataFile) throws HpcWebException{
		HpcBulkMetadataEntries entries = null;
		if(metadataFile == null || metadataFile.getName().isEmpty() || metadataFile.getOriginalFilename().isEmpty())
			return null;
		
		try {
			Sheet tokenSheet = getWorkbookSheet(metadataFile, TOKENS_SHEET);
			Map<String, String> tokens = getTokensMap(tokenSheet);
			Sheet metadataSheet = getWorkbookSheet(metadataFile, METADATA_SHEET);
			Map<String, Map<String, String>> metadataMap = getMetadataMap(metadataSheet);
			entries = buildHpcBulkMetadataEntries(metadataMap, tokens);
		} catch (IOException e) {
			throw new HpcWebException(e);
		}

		return entries;
	}

	private static HpcBulkMetadataEntries buildHpcBulkMetadataEntries(Map<String, Map<String, String>> metadataMap,
			Map<String, String> tokens) {
		HpcBulkMetadataEntries entries = new HpcBulkMetadataEntries();
		List<HpcBulkMetadataEntry> pathMetadataEntries = new ArrayList<HpcBulkMetadataEntry>();
		if (metadataMap == null || metadataMap.isEmpty())
			return null;

		Iterator<String> iterator = metadataMap.keySet().iterator();
		while (iterator.hasNext()) {
			HpcBulkMetadataEntry metadataEntry = new HpcBulkMetadataEntry();
			String path = iterator.next();
			Map<String, String> metadata = metadataMap.get(path);
			path = replaceTokens(path, tokens);
			metadataEntry.setPath(path);
			if (metadata != null && !metadata.isEmpty())
				metadataEntry.getMetadataEntries().addAll(buildMetadataEntries(metadata));
			pathMetadataEntries.add(metadataEntry);
		}
		entries.getPathMetadataEntries().addAll(pathMetadataEntries);
		return entries;
	}

	private static List<HpcMetadataEntry> buildMetadataEntries(Map<String, String> metadata) {
		List<HpcMetadataEntry> entries = new ArrayList<HpcMetadataEntry>();
		Iterator<String> iterator = metadata.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			String value = metadata.get(key);
			HpcMetadataEntry entry = new HpcMetadataEntry();
			entry.setAttribute(key);
			entry.setValue(value);
			entries.add(entry);
		}
		return entries;
	}

	private static Sheet getWorkbookSheet(MultipartFile metadataFile, String sheetName) throws IOException {
		Workbook workbook = new XSSFWorkbook(metadataFile.getInputStream());
		Sheet dataSheet = workbook.getSheet(sheetName);
		return dataSheet;
	}

	private static List<String> getHeader(Sheet metadataSheet) throws HpcWebException {
		List<String> header = new ArrayList<String>();
		Row firstRow = metadataSheet.getRow(metadataSheet.getFirstRowNum());
		List<String> attrNames = new ArrayList<String>();
		Iterator<Cell> cellIterator = firstRow.iterator();
		while (cellIterator.hasNext()) {
			Cell currentCell = cellIterator.next();
			String cellValue = currentCell.getStringCellValue();
			if (cellValue == null || cellValue.isEmpty())
				throw new HpcWebException("Invalid header column value");
			header.add(cellValue);
		}
		if (!header.contains("path") && !header.contains("Path"))
			throw new HpcWebException("Path header column is missing");
		return header;
	}

	private static Map<String, Map<String, String>> getMetadataMap(Sheet metadataSheet) {
		Map<String, Map<String, String>> metdataSheetMap = new HashMap<String, Map<String, String>>();
		Iterator<Row> iterator = metadataSheet.iterator();

		// Read 1st row which is header row with attribute names
		List<String> attrNames = getHeader(metadataSheet);
		// Read all rows (skip 1st) and construct metadata map
		// Skip cells exceeding header size
		while (iterator.hasNext()) {
			String path = null;
			Row currentRow = iterator.next();
			if (currentRow.getRowNum() == 0)
				continue;
			// Skip header row
			int counter = 0;
			Map<String, String> rowMetadata = new HashMap<String, String>();
			
			for(String attrName : attrNames)
			{
				Cell currentCell = currentRow.getCell(counter);
				counter++;
				if(currentCell == null)
					continue;
				if (attrName.equalsIgnoreCase("path"))
				{
					path = currentCell.getStringCellValue();
					continue;
				}
				if(currentCell.getCellType().equals(CellType.NUMERIC))
				{
					double dv = currentCell.getNumericCellValue();
					if (HSSFDateUtil.isCellDateFormatted(currentCell)) {
					    Date date = HSSFDateUtil.getJavaDate(dv);
					    String df = currentCell.getCellStyle().getDataFormatString();
					    String strValue = new CellDateFormatter(df).format(date);
					    rowMetadata.put(attrName, strValue);
					}else {
						rowMetadata.put(attrName, (new Double(dv).toString()));	
					}
					
				}
				else
				{
					if(currentCell.getStringCellValue() != null && !currentCell.getStringCellValue().isEmpty())
						rowMetadata.put(attrName, currentCell.getStringCellValue());
				}
				
			}
			
			metdataSheetMap.put(path, rowMetadata);
		}

		return metdataSheetMap;
	}

	/**
	 * Read token and value as name, value pair. Ignore empty token or value rows
	 * 
	 * @param tokenSheet
	 * @return Tokens name value pair
	 */
	private static Map<String, String> getTokensMap(Sheet tokenSheet) {
		Map<String, String> tokens = new HashMap<String, String>();

		Iterator<Row> iterator = tokenSheet.iterator();

		while (iterator.hasNext()) {

			Row currentRow = iterator.next();
			Iterator<Cell> cellIterator = currentRow.iterator();
			int count = 0;
			String token = null;
			String tokenValue = null;
			while (cellIterator.hasNext()) {

				Cell currentCell = cellIterator.next();
				String cellValue = currentCell.getStringCellValue();
				if (count == 0)
					token = cellValue;
				else
					tokenValue = cellValue;
				count++;
				// Read two column values. Tokens should have name, value pair. Nothing more
				// than that
				if (count == 2)
					break;

			}
			if (token != null && !token.isEmpty() && tokenValue != null && !tokenValue.isEmpty())
				tokens.put(token, tokenValue);
		}
		return tokens;
	}

	public static String replaceTokens(String text, Map<String, String> replacements) {
		Pattern pattern = Pattern.compile("\\{(.+?)\\}");
		Matcher matcher = pattern.matcher(text);
		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String replacement = replacements.get(matcher.group(1));
			if (replacement != null) {
				matcher.appendReplacement(buffer, "");
				buffer.append(replacement);
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}
}
