package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.util.BasicAuthRestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
//import org.codehaus.jackson.map.ObjectMapper;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.rest.auth.DefaultHttpClientAndContext;
import org.irods.jargon.rest.auth.RestAuthUtils;
import org.irods.jargon.rest.commands.dataobject.DataObjectAvuFunctions;
import org.irods.jargon.rest.commands.dataobject.DataObjectAvuFunctionsImpl;
import org.irods.jargon.rest.configuration.RestConfiguration;
import org.irods.jargon.rest.domain.DataObjectData;
import org.irods.jargon.rest.domain.MetadataEntry;
import org.irods.jargon.rest.domain.MetadataOperationResultEntry;
import org.irods.jargon.rest.utils.DataUtils;
import org.irods.jargon.rest.utils.RestTestingProperties;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.ObjectError;

@EnableAutoConfiguration
public class IrodsClient implements HPCClient{
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
	
	IRODSFileSystem irodsFileSystem = null;
	BasicAuthRestTemplate basicAuthRestTemplate;
	public IrodsClient(String str) throws JargonException
	{
		irodsFileSystem = IRODSFileSystem.instance();
		//initialize
	}

	@Override
	public void processDataObject() {
		try {
			Map<String, String> env = System.getenv();
			System.out.println("irods.file::"+ env.get("irods.file"));
			System.out.println("fileName::"+ fileName);
			System.out.println("irodsHost::"+ irodsHost);
			
			
			basicAuthRestTemplate = new BasicAuthRestTemplate(irodsUser, irodsPassword);
				
			
			//ResponseEntity<DataObjectData> dataSetEntities = basicAuthRestTemplate
				//	.getForEntity("http://52.7.244.225:8080/irods-rest/rest/dataObject/tempZone/home/rods/Zhengwu.txt", DataObjectData.class);
			//if (dataSetEntities == null || !dataSetEntities.hasBody()) 
			//{
				//ObjectError error = new ObjectError("id",
					//	"DataObjects not found!");
				//log.info("DataObjects not found!");				  
			//}
			//else
				//System.out.print("dataSetEntities::"+dataSetEntities);
			
			
			//HttpPost httpPost = new HttpPost(sb.toString());
			//httpPost.addHeader("accept", "application/json");

			
			
			
			File localFile = new File("testRest.txt");
			// httpPost.addHeader("Content-type", "multipart/form-data");
			//FileBody fileEntity = new FileBody(localFile,
			//"application/octet-stream");
			//MultipartEntity reqEntity = new MultipartEntity(
			//HttpMultipartMode.BROWSER_COMPATIBLE);
			//reqEntity.addPart("uploadFile", fileEntity);
			//httpPost.setEntity(reqEntity);
			//HttpResponse response = clientAndContext.getHttpClient().execute(
			//httpPost, clientAndContext.getHttpContext());
			
			
			
			//POST WITH MULTIPART
			//MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			//builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			//FileBody fileBody = new FileBody(localFile); //image should be a String
			//builder.addPart("uploadFile", fileBody); 
			//System.out.print("BUILDING ENTITY::");
			//HttpEntity entity = builder.build();
			//System.out.print("POSTING REQUEST::");
			
			/*
			FileSystemResource resource = new FileSystemResource(localFile);

			MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("file", resource);
			HttpHeaders headers1 = new HttpHeaders();
			headers1.setContentType(MediaType.MULTIPART_FORM_DATA);
			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(map, headers1);			
			//HttpEntity<MultiValueMap<String, Object>> entity1 = new HttpEntity<MultiValueMap<String, Object>>(map, requestHeaders);			
			
			ResponseEntity<DataObjectData> dataSetResEntity = basicAuthRestTemplate.postForEntity("http://52.7.244.225:8080/irods-rest/rest/fileContents/tempZone/home/rods",requestEntity,DataObjectData.class);
			//ResponseEntity<DataObjectData> dataSetResEntity = basicAuthRestTemplate.exchange(new URI("http://52.7.244.225:8080/irods-rest/rest/fileContents/tempZone/home/rods/testRest.txt"), HttpMethod.POST, entity, DataObjectData.class);
						
			System.out.print("RESPONSE FROM POST::" + dataSetResEntity);
			*/
			
			
			
			//Map<String, Object> map1 = new HashMap<String, Object>();
			//List<MetadataEntry>  metadataEntries = new ArrayList<MetadataEntry>();
			//MetadataEntry metadataEntry = new MetadataEntry();
			//metadataEntry.setAttribute("MyAttt1");
			//metadataEntry.setValue("MyVal1");
			//metadataEntry.setUnit("MyUnit1");
			//metadataEntries.add(metadataEntry);
			//map1.put("metadataEntries", metadataEntries);
			//readMetadataJsonFromFile();
			//String jSonStr = "{\"metadataEntries\":[{\"attribute\":\"attr4\",\"value\":\"val4\",\"unit\":\"unit4\"}]}";
		
			Map<String, String> map1 = new HashMap<String, String>();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>(readMetadataJsonFromFile(),headers);
//			map1.put("metadataEntries", "");
			basicAuthRestTemplate.put("http://52.7.244.225:8080/irods-rest/rest/dataObject/tempZone/home/rods/Zhengwu.txt/metadata", entity);   			
			
			
			
			

			//postDataObject();
			//readAddMetadataForDataObject();
			putUploadFileJargon();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
 
		
	}
	
	
	public void readAddMetadataForDataObject() throws Exception {
		String fileName = "Zhengwu.txt";
		String expectedAttribName = "testName";
		String expectedValueName = "testVal";

		String targetIrodsObj = "http://52.7.244.225:8080/irods-rest/rest/dataObject/tempZone/home/rods";
		

		String targetIrodsDataObject = targetIrodsObj + "/" + fileName;

		IRODSAccount account = new IRODSAccount(irodsHost, 8080,
				irodsUser, irodsPassword,
				irodsUser, "tempZone",
				"");	

		DefaultHttpClientAndContext clientAndContext = setContext(account);
		
		IRODSFile targetIrodsFile = irodsFileSystem.getIRODSFileFactory(
				account).instanceIRODSFile(targetIrodsObj);
		targetIrodsFile.deleteWithForceOption();
		targetIrodsFile.mkdirs();
		
		DataTransferOperations dataTransferOperationsAO = irodsFileSystem
				.getIRODSAccessObjectFactory().getDataTransferOperations(
						account);
		dataTransferOperationsAO.putOperation(new File(fileName),
				targetIrodsFile, null, null);

		AvuData avuData = AvuData.instance(expectedAttribName,
				expectedValueName, "");
		List<AvuData> bulkAvuData = new ArrayList<AvuData>();
		bulkAvuData.add(avuData);

		RestConfiguration restConfiguration = new RestConfiguration();

		DataObjectAvuFunctions dataObjectAvuFunctionsImpl = new DataObjectAvuFunctionsImpl(
				restConfiguration, account,
				irodsFileSystem.getIRODSAccessObjectFactory());

		List<MetadataOperationResultEntry> responses = dataObjectAvuFunctionsImpl
				.addAvuMetadata(targetIrodsDataObject, bulkAvuData);

	}


 public String readMetadataJsonFromFile() {

	JSONParser parser = new JSONParser();
	try {
			Object obj = parser.parse(new FileReader("testRest.json"));	
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
	
	
	private DefaultHttpClientAndContext setContext(IRODSAccount account) {
		HttpHost targetHost = new HttpHost(irodsHost,8080, "http");		
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(irodsHost, 8080),
				new UsernamePasswordCredentials(account.getUserName(),
						account.getPassword()));
		// Create AuthCache instance
		AuthCache authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local
		// auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);

		// Add AuthCache to the execution context
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		DefaultHttpClientAndContext clientAndContext = new DefaultHttpClientAndContext();
		clientAndContext.setHost(irodsHost);
		clientAndContext.setHttpClient(httpclient);
		clientAndContext.setHttpContext(localcontext);
		return clientAndContext;
	}
	
	public void postDataObject() throws Exception {
	File localFile = new File("testRest.txt");
	
	IRODSAccount account = new IRODSAccount(irodsHost, 8080,
			irodsUser, irodsPassword,
			irodsUser, "tempZone",
			"");	
	
	IRODSAccessObjectFactory accessObjectFactory = irodsFileSystem
			.getIRODSAccessObjectFactory();

	StringBuilder sb = new StringBuilder();
	sb.append("http://52.7.244.225:");
	sb.append(8080);
	sb.append("/irods-rest/rest/fileContents/");
	sb.append(DataUtils.encodeIrodsAbsolutePath("tempZone/home/rods/testRest.txt",
			accessObjectFactory.getJargonProperties().getEncoding()));




	DefaultHttpClientAndContext clientAndContext = setContext(account);

	try {

		HttpPost httpPost = new HttpPost(sb.toString());
		httpPost.addHeader("accept", "application/json");
		// httpPost.addHeader("Content-type", "multipart/form-data");
		FileBody fileEntity = new FileBody(localFile,
				"application/octet-stream");
		MultipartEntity reqEntity = new MultipartEntity(
				HttpMultipartMode.BROWSER_COMPATIBLE);
		reqEntity.addPart("uploadFile", fileEntity);
		httpPost.setEntity(reqEntity);
		HttpResponse response = clientAndContext.getHttpClient().execute(
				httpPost, clientAndContext.getHttpContext());
		//HttpEntity entity = response.getEntity();

		//String entityData = EntityUtils.toString(entity);
		//EntityUtils.consume(entity);
		System.out.println("JSON>>>");
		//System.out.println(entityData);
		//ObjectMapper objectMapper = new ObjectMapper();
		//DataObjectData result = objectMapper.readValue(entityData,
			//	DataObjectData.class);



		//IRODSFile actual = accessObjectFactory.getIRODSFileFactory(
				//account).instanceIRODSFile("testRest.txt");


	} finally {
		// When HttpClient instance is no longer needed,
		// shut down the connection manager to ensure
		// immediate deallocation of all system resources
		clientAndContext.getHttpClient().getConnectionManager().shutdown();
	}
	}
	
	
    public void putUploadFileJargon() throws Exception {
    	// generate a local scratch file
        String testFileName = "testRest.txt";

        File localFile=new File(testFileName);
        IRODSAccount account = IRODSAccount.instance(irodsHost, 1247, irodsUser, irodsPassword,
                "/tempZone/home/rods", "tempZone", "");
        DataTransferOperations dto=irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(account);
        IRODSFile destFile=irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile("/tempZone/home/rods");
        dto.putOperation(localFile,destFile,null,null);
        
    }	

}
