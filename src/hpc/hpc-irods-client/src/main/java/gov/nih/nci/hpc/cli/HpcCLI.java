package gov.nih.nci.hpc.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import gov.nih.nci.hpc.cli.IrodsClient;
import gov.nih.nci.hpc.cli.util.BasicAuthRestTemplate;

@SpringBootApplication
@EnableAutoConfiguration
public class HpcCLI implements CommandLineRunner {
	@Value("${irods.server.host}")
	private String irodsHost;
	@Value("${irods.server.port}")
	private String irodsHttpPort;
	@Value("${irods.server.user}")
	private String irodsUser;
	@Value("${irods.server.password}")
	private String irodsPassword;
	@Value("${irods.file}")
	private String fileName;
	@Value("${irods.operation}")
	private String operation;
	
    private static final Logger log = LoggerFactory.getLogger(HpcCLI.class);
	BasicAuthRestTemplate basicAuthRestTemplate;
	IRODSFileSystem irodsFileSystem = null;
	
    public static void main(String args[]) {
        SpringApplication.run(HpcCLI.class);
    }
		
    @Override
    public void run(String[] strings) throws Exception {
    	
    	irodsFileSystem = IRODSFileSystem.instance();
	 	
    	//validate arguments
    	
    	//Irods
    	
    	//HPCClient hpcClient = new IrodsClient("irods");
    	//hpcClient.processDataObject();
    	processDataObject();
   }
    

	public void processDataObject() {
		try {
			
			basicAuthRestTemplate = new BasicAuthRestTemplate(irodsUser, irodsPassword);
			log.info("operation::::" + operation);
			
			if("CREATE".equalsIgnoreCase(operation))
			{
				log.info("ADD A NEW FILE");
				putUploadFileJargon();
			}
			if("ADDMETADATA".equalsIgnoreCase(operation))
			{
				log.info("ADDING METADATA");
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> entity = new HttpEntity<String>(readMetadataJsonFromFile(),headers);
				basicAuthRestTemplate.put("http://52.7.244.225:8080/irods-rest/rest/dataObject/tempZone/home/rods/"+fileName+"/metadata", entity);
			}

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
	}
 
    public void putUploadFileJargon() throws Exception {
        //String testFileName = "testRest.txt";

        File localFile=new File(fileName);
        IRODSAccount account = IRODSAccount.instance(irodsHost, 1247, irodsUser, irodsPassword,
                "/tempZone/home/rods", "tempZone", "");
        DataTransferOperations dto=irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(account);
        IRODSFile destFile=irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile("/tempZone/home/rods");
        dto.putOperation(localFile,destFile,null,null);
        
    }
    
	 public String readMetadataJsonFromFile() {

			JSONParser parser = new JSONParser();
			String jsonFileName = fileName.substring(0,fileName.lastIndexOf("."));
			System.out.println("jsonFileName::"+jsonFileName);
			try {
					Object obj = parser.parse(new FileReader(jsonFileName+".json"));	
					JSONObject jsonObject = (JSONObject) obj;
					System.out.println(jsonObject.toJSONString());
					return jsonObject.toJSONString();
			
			} catch (FileNotFoundException e) 
			{
					e.printStackTrace();
			} catch (IOException e) 
			{
					e.printStackTrace();
			} catch (ParseException e) 
			{
					e.printStackTrace();
			}	
				return "";
		}
	 
	 
    
}