package gov.nih.nci.hpc.web.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Source;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementDocListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementTreeDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcBookmark;

public class HpcClientUtil {

	public static WebClient getWebClient(String url, String hpcCertPath, String hpcCertPassword) {
		WebClient client = WebClient.create(url, Collections.singletonList(new JacksonJsonProvider()));
		WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart", "true");
		WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(60000000);
		WebClient.getConfig(client).getHttpConduit().getClient().setConnectionTimeout(60000000);
		HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();

		TLSClientParameters params = conduit.getTlsClientParameters();
		if (params == null) {
			params = new TLSClientParameters();
			conduit.setTlsClientParameters(params);
		}

		params.setTrustManagers(new TrustManager[] { new TrustAllX509TrustManager() });
		conduit.setTlsClientParameters(params);

		params.setDisableCNCheck(true);
		return client;
	}

	public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL)
			throws HpcWebException {

		WebClient client = HpcClientUtil.getWebClient(hpcServerURL, null, null);
		String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
		client.header("Authorization", "Basic " + token);
		Response restResponse = client.get();
		try {

			if (restResponse.getStatus() != 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Authentication failed: " + exception.getMessage());
			}
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser;
			parser = factory.createParser((InputStream) restResponse.getEntity());
			HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
			return dto.getToken();
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e1.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e.getMessage());
		}
	}

	public static HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL, String doc,
			String hpcCertPath, String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcModelURL + "/" + doc, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcDataManagementModelDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		}
	}

	public static HpcDataManagementDocListDTO getDOCs(String token, String hpcModelURL, String hpcCertPath,
			String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcModelURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOCs due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcDataManagementDocListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOCs due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOCs due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOCs due to: " + e.getMessage());
		}
	}

	public static HpcDataManagementTreeDTO getDOCTree(String token, String serviceURL, String docName, boolean list,
			String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(serviceURL + "/" + docName, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			// System.out.println("restResponse.getStatus():"
			// +restResponse.getStatus());
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcDataManagementTreeDTO tree = parser.readValueAs(HpcDataManagementTreeDTO.class);
				return tree;
			} else {
				throw new HpcWebException("Collection not found!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get tree for: " + docName + " due to: " + e.getMessage());
		}
	}

	public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL, String path, boolean list,
			String hpcCertPath, String hpcCertPassword) {
		return getCollection(token, hpcCollectionlURL, path, false, list, hpcCertPath, hpcCertPassword);
	}

	public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL, String path,
			boolean children, boolean list, String hpcCertPath, String hpcCertPassword) {
		try {
			String serviceURL = hpcCollectionlURL;
			if (children)
				serviceURL = serviceURL + path + "/children";
			else if (list)
				serviceURL = serviceURL + path + "?list=true";
			else
				serviceURL = serviceURL + path + "?list=false";

			WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			// System.out.println("restResponse.getStatus():"
			// +restResponse.getStatus());
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
				return collections;
			} else {
				throw new HpcWebException("Collection not found!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Collection for: " + path + " due to: " + e.getMessage());
		}
	}

	public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path, boolean list,
			String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(
					hpcDatafileURL + "/" + path + (list ? "?list=true" : "?list=false"), hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			// System.out.println("restResponse.getStatus():"
			// +restResponse.getStatus());
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcDataObjectListDTO datafiles = parser.readValueAs(HpcDataObjectListDTO.class);
				return datafiles;
			} else {
				throw new HpcWebException("Collection not found!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Collection for: " + path + " due to: " + e.getMessage());
		}
	}

	public static HpcUserListDTO getUsers(String token, String hpcUserURL, String userId, String firstName,
			String lastName, String doc, String hpcCertPath, String hpcCertPassword) {
		try {
			boolean first = true;
			String paramsURL = "";
			if (userId != null && userId.trim().length() > 0) {
				paramsURL = "?nciUserId=" + URLEncoder.encode(userId);
				first = false;
			}
			if (firstName != null && firstName.trim().length() > 0) {
				if (first) {
					paramsURL = "?firstNamePattern=" + URLEncoder.encode(firstName);
					first = false;
				} else
					paramsURL = paramsURL + "&firstNamePattern=" + URLEncoder.encode(firstName);
			}
			if (lastName != null && lastName.trim().length() > 0) {
				if (first) {
					paramsURL = "?lastNamePattern=" + URLEncoder.encode(lastName);
					first = false;
				} else
					paramsURL = paramsURL + "&lastNamePattern=" + URLEncoder.encode(lastName);
			}

			if (doc != null && doc.trim().length() > 0) {
				if (first) {
					paramsURL = "?doc=" + URLEncoder.encode(doc);
					first = false;
				} else
					paramsURL = paramsURL + "&doc=" + URLEncoder.encode(doc);
			}

			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + paramsURL, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcUserListDTO users = parser.readValueAs(HpcUserListDTO.class);
				return users;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Users due to: " + e.getMessage());
		}
		return null;
	}

	public static HpcUserDTO getUser(String token, String hpcUserURL, String hpcCertPath, String hpcCertPassword) {
		try {

			WebClient client = HpcClientUtil.getWebClient(hpcUserURL, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				return parser.readValueAs(HpcUserDTO.class);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to get user: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get User due to: " + e.getMessage());
		}
	}

	public static HpcUserDTO getUserByAdmin(String token, String hpcUserURL, String userId, String hpcCertPath,
			String hpcCertPassword) {
		try {

			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + userId, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				return parser.readValueAs(HpcUserDTO.class);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to get user: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get User due to: " + e.getMessage());
		}
	}

	public static boolean createUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO, String userId,
			String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + userId, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("PUT", userDTO);
			if (restResponse.getStatus() == 201) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to create user: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to create User due to: " + e.getMessage());
		}
	}

	public static boolean createBookmark(String token, String hpcBookmarkURL, HpcBookmarkRequestDTO hpcBookmark,
			String hpcBookmarkName, String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcBookmarkURL + "/" + URLEncoder.encode(hpcBookmarkName, "UTF-8"), hpcCertPath,
					hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("PUT", hpcBookmark);
			if (restResponse.getStatus() == 201) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to create bookmark: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to create bookmark due to: " + e.getMessage());
		}
	}

	public static boolean deleteBookmark(String token, String hpcBookmarkURL, 
			String hpcBookmarkName, String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcBookmarkURL + "/" + hpcBookmarkName, hpcCertPath,
					hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.delete();
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to delete bookmark: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to delete bookmark due to: " + e.getMessage());
		}
	}

	public static HpcBookmarkListDTO getBookmarks(String token, String hpcBookmarkURL, String hpcCertPath,
			String hpcCertPassword) {
		WebClient client = HpcClientUtil.getWebClient(hpcBookmarkURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Bookmarks due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcBookmarkListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Bookmarks due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Bookmarks due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Bookmarks due to: " + e.getMessage());
		}
	}

	public static HpcGroupMembersResponseDTO createGroup(String token, String hpcUserURL,
			HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath, String hpcCertPassword) {
		HpcGroupMembersResponseDTO response = null;
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + URLEncoder.encode(groupName), hpcCertPath,
					hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("PUT", groupDTO);
			if (restResponse.getStatus() == 201) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to create group: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to create group due to: " + e.getMessage());
		}
		return response;
	}

	public static HpcGroupMembersResponseDTO updateGroup(String token, String hpcUserURL,
			HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath, String hpcCertPassword) {
		HpcGroupMembersResponseDTO response = null;
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + groupName, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);
			Response restResponse = client.invoke("POST", groupDTO);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to update group: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to update group due to: " + e.getMessage());
		}
		return response;
	}

	public static boolean deleteGroup(String token, String hpcUserURL, String groupName, String hpcCertPath,
			String hpcCertPassword) {
		HpcGroupMembersResponseDTO response = null;
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + groupName, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);
			Response restResponse = client.invoke("DELETE", null);
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to delete group: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to delete group due to: " + e.getMessage());
		}
	}

	public static boolean updateCollection(String token, String hpcCollectionURL,
			HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcCollectionURL + path, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("PUT", collectionDTO);
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to update collection: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to update collection due to: " + e.getMessage());
		}
	}

	public static boolean updateDatafile(String token, String hpcDatafileURL, HpcDataObjectRegistrationDTO datafileDTO,
			String path, String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL + path, hpcCertPath, hpcCertPassword);
			client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
			List<Attachment> atts = new LinkedList<Attachment>();
			atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
					datafileDTO));

			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.put(new MultipartBody(atts));
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to update data file: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to update data file due to: " + e.getMessage());
		}
	}

	public static boolean deleteDatafile(String token, String hpcDatafileURL,
			String path, String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL + path, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.delete();
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException(exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException(e.getMessage());
		}
	}
	public static boolean updateUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO, String userId,
			String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(hpcUserURL + "/" + userId, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("POST", userDTO);
			if (restResponse.getStatus() == 200) {
				return true;
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new HpcWebException("Failed to update user: " + exception.getMessage());
			}
		} catch (HpcWebException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to update user due to: " + e.getMessage());
		}
	}

	public static HpcGroupListDTO getGroups(String token, String hpcGroupURL, String groupName, String hpcCertPath,
			String hpcCertPassword) {
		try {
			String paramsURL = "";
			if (groupName != null && groupName.trim().length() > 0) {
				paramsURL = "?groupPattern=" + URLEncoder.encode(groupName);
			}

			WebClient client = HpcClientUtil.getWebClient(hpcGroupURL + paramsURL, hpcCertPath, hpcCertPassword);
			client.header("Authorization", "Bearer " + token);

			Response restResponse = client.invoke("GET", null);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcGroupListDTO groups = parser.readValueAs(HpcGroupListDTO.class);
				return groups;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Users due to: " + e.getMessage());
		}
		return null;
	}

	public static HpcNamedCompoundMetadataQueryDTO getQuery(String token, String hpcQueryURL, String queryName,
			String hpcCertPath, String hpcCertPassword) {

		String serviceURL = hpcQueryURL + "/" + queryName;
		WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + queryName + " due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNamedCompoundMetadataQueryDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		}
	}

	public static HpcNamedCompoundMetadataQueryListDTO getSavedSearches(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {
		WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();
		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNamedCompoundMetadataQueryListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
		}
	}

	public static HpcNotificationDeliveryReceiptListDTO getNotificationReceipts(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();
		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNotificationDeliveryReceiptListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		}
	}

	public static HpcEntityPermissionsDTO getPermissions(String token, String hpcServiceURL, String hpcCertPath,
			String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcServiceURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();
		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcEntityPermissionsDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
		}
	}

	public static HpcUserPermissionDTO getPermissionForUser(String token, String path, String userId,
			String hpcServiceURL, String hpcCertPath, String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcServiceURL + path + "/acl/user/" + userId, hpcCertPath,
				hpcCertPassword);

		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();
		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get permission due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcUserPermissionDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get permission due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get permission due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get permission due to: " + e.getMessage());
		}
	}

	public static HpcNotificationSubscriptionListDTO getUserNotifications(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNotificationSubscriptionListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		}
	}

	public static HpcMetadataAttributesListDTO getMetadataAttrNames(String token, String hpcMetadataAttrsURL,
			String hpcCertPath, String hpcCertPassword) {

		String url = hpcMetadataAttrsURL;

		WebClient client = HpcClientUtil.getWebClient(url, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + token);

		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcMetadataAttributesListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
		}
	}

	public static RestTemplate getRestTemplate(String hpcCertPath, String hpcCertPassword) {

		RestTemplate restTemplate = null;
		FileInputStream fis = null;
		try {
			if (hpcCertPath != null && hpcCertPassword != null) {
				fis = new java.io.FileInputStream(hpcCertPath);
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(fis, hpcCertPassword.toCharArray());
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keyStore, hpcCertPassword.toCharArray());
				KeyManager[] keyManagers = kmf.getKeyManagers();

				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(keyStore);
				TrustManager[] trustManagers = tmf.getTrustManagers();

				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagers, trustManagers, null);

				CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier())
						.setSSLContext(sslContext).build();

				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
				requestFactory.setHttpClient(httpClient);
				restTemplate = new RestTemplate(requestFactory);
			} else {
				@SuppressWarnings("deprecation")
				SSLContextBuilder builder = new SSLContextBuilder();
				builder.loadTrustMaterial(null, new TrustStrategy() {
					@Override
					public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
						return true;
					}
				});

				SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
						SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslSF).build();
				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
						httpClient);
				restTemplate = new RestTemplate(requestFactory);
			}
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			messageConverters.add(new FormHttpMessageConverter());
			messageConverters.add(new SourceHttpMessageConverter<Source>());
			messageConverters.add(new StringHttpMessageConverter());
			messageConverters.add(new MappingJackson2HttpMessageConverter());
			messageConverters.add(new MultipartAwareFormHttpMessageConverter());
			restTemplate.setMessageConverters(messageConverters);

			restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return restTemplate;
	}

	private static void enableSSL() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		}
	}

	public static String encode(String strVal) {
		if (strVal == null)
			return null;
		else if (strVal.indexOf("/") == -1)
			return strVal;
		else {
			StringBuffer encodedStr = new StringBuffer();
			StringTokenizer tokens = new StringTokenizer(strVal, "/");
			while (tokens.hasMoreTokens()) {
				String token = tokens.nextToken();
				encodedStr.append(URLEncoder.encode(token));
				if (tokens.hasMoreTokens())
					encodedStr.append("/");
			}
			return encodedStr.toString();
		}
	}
}
