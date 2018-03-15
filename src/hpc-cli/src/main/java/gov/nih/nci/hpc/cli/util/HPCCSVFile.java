package gov.nih.nci.hpc.cli.util;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import gov.nih.nci.hpc.cli.domain.HPCBatchCollection;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCCSVFile {
	//CSV file header
    private static final String [] FILE_HEADER_MAPPING = {"Collection Type","Collection name","Project Type","Internal Project ID","Collection Description","Parent Collection Path (Logical Path)","Source Lab PI","Lab /   Branch Name","DOC of the PI","Original Creation Date","Registrar Name","Registrar DOC","PHI Content","PII Content","Data Encryption Status","Data Compression Status","Funding Organization","Comments","Format of the Dataset Files","FlowCell ID","Run_ID","Run Date","Sequencing Platform","Sequencing Application Type","Library ID","Library Name","Library Type","Library Protocol","Read Type","Read Length"};

	private static final String COLLECTION_TYPE = "Collection Type";
	private static final String COLLECTION_NAME = "Collection name";
	private static final String PROJECT_TYPE = "Project Type";
	private static final String INTERNAL_PROJECT_ID = "Internal Project ID";
	private static final String COLLECTION_DESCRIPTION = "Collection Description";
	private static final String PARENT_COLLECTION_PATH = "Parent Collection Path (Logical Path)";
	private static final String SOURCE_LAB_PI = "Source Lab PI";
	private static final String LAB_BRANCH_NAME = "Lab /   Branch Name";
	private static final String DOC_OF_THE_PI = "DOC of the PI";
	private static final String ORIGINAL_CREATION_DATE = "Original Creation Date";
	private static final String REGISTRAR_NAME = "Registrar Name";
	private static final String REGISTRAR_DOC = "Registrar DOC";
	private static final String PHI_CONTENT = "PHI Content";
	private static final String PII_CONTENT = "PII Content";
	private static final String DATA_ENCRYPTION_STATUS = "Data Encryption Status";
	private static final String DATA_COMPRESSION_STATUS = "Data Compression Status";
	private static final String FUNDING_ORGANIZATION = "Funding Organization";
	private static final String COMMENTS = "Comments";
	private static final String FORMAT_OF_THE_DATASET_FILES = "Format of the Dataset Files";
	private static final String FLOWCELL_ID = "FlowCell ID";
	private static final String RUN_ID = "Run_ID";
	private static final String RUN_DATE = "Run Date";
	private static final String SEQUENCING_PLATFORM = "Sequencing Platform";
	private static final String SEQUENCING_APPLICATION_TYPE = "Sequencing Application Type";
	private static final String LIBRARY_ID = "Library ID";
	private static final String LIBRARY_NAME = "Library Name";
	private static final String LIBRARY_TYPE = "Library Type";
	private static final String LIBRARY_PROTOCOL = "Library Protocol";
	private static final String READ_TYPE = "Read Type";
	private static final String READ_LENGTH = "Read Length";
	
	public String parseBatchFile(String fileName) {
		readCsvFile(fileName);
		return "Batch Upload Successful";
	}


	
	private void readCsvFile(String fileName) {

		FileReader fileReader = null;
		
		CSVParser csvFileParser = null;
		
		//Create the CSVFormat object with the header mapping
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
     
        try {
        	
        	//Create a new list of student to be filled by CSV file data 
        	List<HPCBatchCollection> collections = new ArrayList();
            
            //initialize FileReader object
            fileReader = new FileReader(fileName);
           
            //initialize CSVParser object
            csvFileParser = new CSVParser(fileReader, csvFileFormat);
            
            Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
    
            //Get a list of CSV file records
            List<CSVRecord> csvRecords = csvFileParser.getRecords(); 
            
            //Read the CSV file records starting from the second record to skip the header
            for (int i = 1; i < csvRecords.size(); i++) {
            	CSVRecord record = csvRecords.get(i);
            	  String[] fields=new String[record.size()];

            	List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
              	for (Entry<String, Integer> entry : headersMap.entrySet()) {
            		System.out.println("Key : " + entry.getKey());
            		System.out.println("VALUE : " + record.get( entry.getKey()));            		
            			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
            			hpcMetadataEntry.setAttribute(entry.getKey());
            			hpcMetadataEntry.setValue(record.get( entry.getKey()));
            			listOfhpcCollection.add(hpcMetadataEntry);
            		}            		              	  
            	  
//            	//Create a new student object and fill his data
//            	HPCBatchCollection batchCollection = new HPCBatchCollection();
//            	batchCollection.setCollectionType(record.get(COLLECTION_TYPE));
//            	batchCollection.setCollectionName(record.get(COLLECTION_NAME));
//            	batchCollection.setProjectType(record.get(PROJECT_TYPE));
//            	batchCollection.setInternalProjectID(record.get(INTERNAL_PROJECT_ID));
//            	batchCollection.setCollectionDescription(record.get(COLLECTION_DESCRIPTION));
//            	batchCollection.setParentCollectionPath(record.get(PARENT_COLLECTION_PATH));
//            	batchCollection.setSourceLabPI(record.get(SOURCE_LAB_PI));
//            	batchCollection.setLabBranchName(record.get(LAB_BRANCH_NAME));
//            	batchCollection.setDocPI(record.get(DOC_OF_THE_PI));
//            	batchCollection.setOriginalCreationDate(record.get(ORIGINAL_CREATION_DATE));
//            	batchCollection.setRegistrarName(record.get(REGISTRAR_NAME));
//            	batchCollection.setRegistrarDOC(record.get(REGISTRAR_DOC));
//            	batchCollection.setPhiContent(record.get(PHI_CONTENT));
//            	batchCollection.setPiiContent(record.get(PII_CONTENT));
//            	batchCollection.setDataEncryptionStatus(record.get(DATA_ENCRYPTION_STATUS));
//            	batchCollection.setDataCompressionStatus(record.get(DATA_COMPRESSION_STATUS));
//            	batchCollection.setFundingOrganization(record.get(FUNDING_ORGANIZATION));
//            	batchCollection.setComments(record.get(COMMENTS));
//            	batchCollection.setDatasetFilesFormat(record.get(FORMAT_OF_THE_DATASET_FILES));
//            	batchCollection.setFlowCellID(record.get(FLOWCELL_ID));
//            	batchCollection.setRunID(record.get(RUN_ID));
//            	batchCollection.setRunDate(record.get(RUN_DATE));
//            	batchCollection.setSequencingPlatform(record.get(SEQUENCING_PLATFORM));
//            	batchCollection.setSequencingApplicationType(record.get(SEQUENCING_APPLICATION_TYPE));
//            	batchCollection.setLibraryID(record.get(LIBRARY_ID));
//            	batchCollection.setLibraryName(record.get(LIBRARY_NAME));
//            	batchCollection.setLibraryType(record.get(LIBRARY_TYPE));
//            	batchCollection.setLibraryProtocol(record.get(LIBRARY_PROTOCOL));
//            	batchCollection.setReadType(record.get(READ_TYPE));
//            	batchCollection.setReadLength(record.get(READ_LENGTH));
//            	collections.add(batchCollection);	

    			HpcFileLocation source = new HpcFileLocation();
    			source.setFileContainerId("mahinarra#mahinarraEP2");
    			String filePath = "~/share.txt";
    			source.setFileId(filePath);
    			              	
              	
				HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationRequestDTO();
				hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);
				hpcDataObjectRegistrationDTO.setSource(source);
				
				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				List <MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				//headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<HpcDataObjectRegistrationRequestDTO> entity = new HttpEntity<HpcDataObjectRegistrationRequestDTO>(hpcDataObjectRegistrationDTO, headers);
				//System.out.println("Adding Metadata to .."+ hpcServerURL+"/"+hpcCollection+targetCollection);

				ResponseEntity<HpcExceptionDTO> response = restTemplate.exchange("http://localhost:7737/hpc-server/dataObject/tempZone/home/rods/"+getAttributeValueByName("File name",hpcDataObjectRegistrationDTO), HttpMethod.PUT,entity , HpcExceptionDTO.class);
              	
			}
        } 
        catch (Exception e) {
        	System.out.println("Error in CsvFileReader !!!");
        } finally {
            try {
                fileReader.close();
                csvFileParser.close();
            } catch (IOException e) {
            	System.out.println("Error while closing fileReader/csvFileParser !!!");
            }
        }

	}
	
	private List<HpcMetadataEntry> getListOfMetadataElements(HPCBatchCollection batchCollection) throws Exception {
		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		for(PropertyDescriptor propertyDescriptor : 
		    Introspector.getBeanInfo(batchCollection.getClass()).getPropertyDescriptors()){

		    System.out.println(propertyDescriptor.getReadMethod().invoke(batchCollection, null));
		}
		
		return listOfhpcCollection;
	}
	
	
	private String getAttributeValueByName(String collectionType,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO) {
		for (HpcMetadataEntry hpcMetadataEntry : hpcDataObjectRegistrationDTO.getMetadataEntries()) {
			if (collectionType.equalsIgnoreCase(hpcMetadataEntry.getAttribute()))
				return hpcMetadataEntry.getValue();
		}
		return null;
	}
}
