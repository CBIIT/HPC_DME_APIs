/**
 * HpcDataBrowseRestServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataBrowseBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataBrowseRestService;

/**
 * HPC Data Browse REST Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataBrowseRestServiceImpl extends HpcRestServiceImpl
    implements HpcDataBrowseRestService {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The Data Browse Business Service instance.
  @Autowired private HpcDataBrowseBusService dataBrowseBusService = null;
  
  //The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataBrowseRestServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcDataBrowseRestService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public Response addBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest) {
    try {
      dataBrowseBusService.addBookmark(URLDecoder.decode(bookmarkName, "UTF-8"), bookmarkRequest);
    }  catch (IllegalArgumentException e) {
    	logger.info("Failed to create bookmark for " + bookmarkName, e);
    	HpcException hpce =
    	          new HpcException(
    	              "Failed to create bookmark: Invalid Bookmark Name",
    	              HpcErrorType.DATA_MANAGEMENT_ERROR);
    	      return errorResponse(hpce);
	} catch (Exception e) {
		logger.info("Failed to create bookmark " + bookmarkName, e);
    	HpcException hpce =
    	          new HpcException(
    	              "Failed to create bookmark: " + e.getMessage(),
    	              HpcErrorType.DATA_MANAGEMENT_ERROR);
    	      return errorResponse(hpce);
	}

    return createdResponse(null);
  }

  @Override
  public Response updateBookmark(String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest) {
    try {
      dataBrowseBusService.updateBookmark(
          URLDecoder.decode(bookmarkName, "UTF-8"), bookmarkRequest);
    } catch (IllegalArgumentException e) {
    	logger.info("Failed to update bookmark for " + bookmarkName, e);
    	HpcException hpce =
    	          new HpcException(
    	              "Failed to update bookmark: Invalid Bookmark Name",
    	              HpcErrorType.DATA_MANAGEMENT_ERROR);
    	      return errorResponse(hpce);
	} catch (Exception e) {
      logger.info("Failed to update bookmark " + bookmarkName, e);	
      HpcException hpce =
          new HpcException(
              "Failed to update bookmark: " + e.getMessage(),
              HpcErrorType.DATA_MANAGEMENT_ERROR);
      return errorResponse(hpce);
    } 

    return okResponse(null, false);
  }

  @Override
  public Response deleteBookmark(String bookmarkName) {
    try {
      dataBrowseBusService.deleteBookmark(URLDecoder.decode(bookmarkName, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      HpcException hpce =
          new HpcException(
              "Failed to decode bookmark name: " + e.getMessage(),
              HpcErrorType.DATA_MANAGEMENT_ERROR);
      return errorResponse(hpce);
    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response getBookmark(String bookmarkName) {
    HpcBookmarkDTO bookmark = null;
    try {
      bookmark = dataBrowseBusService.getBookmark(URLDecoder.decode(bookmarkName, "UTF-8"));

    } catch (UnsupportedEncodingException e) {
      HpcException hpce =
          new HpcException(
              "Failed to decode bookmark name: " + e.getMessage(),
              HpcErrorType.DATA_MANAGEMENT_ERROR);
      return errorResponse(hpce);
    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(bookmark.getBookmark() != null ? bookmark : null, true);
  }

  @Override
  public Response getBookmarks() {
    HpcBookmarkListDTO bookmarks = null;
    try {
      bookmarks = dataBrowseBusService.getBookmarks();

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!bookmarks.getBookmarks().isEmpty() ? bookmarks : null, true);
  }

  @PUT
  @Path("/s3")
  @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
  public Response uploadToS3(
      @QueryParam("file") String filePath,
      @QueryParam("url") String urlStr,
      @QueryParam("path") String path,
      @QueryParam("userId") String userId) {
    InputStream inputStream = null;
    HttpURLConnection connection;
    try {
      File file = new File(filePath);
      URL url = new URL(urlStr);
      inputStream = new FileInputStream(file);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      connection.setChunkedStreamingMode(2048);
      connection.setRequestProperty("path", path);
      connection.setRequestProperty("user_id", userId);
      OutputStream out = connection.getOutputStream();

      byte[] buf = new byte[1024];
      int count;

      while ((count = inputStream.read(buf)) != -1) {
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        out.write(buf, 0, count);
      }
      out.close();
      inputStream.close();

      int responseCode = connection.getResponseCode();

      if (responseCode == 200) {
        return okResponse(null, false);
      }
    } catch (Exception e) {
      return errorResponse(new HpcException(e.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR));
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return null;
  }
}
