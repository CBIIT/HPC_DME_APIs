package gov.nih.nci.hpc.ws.rs.test.sso;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rs.security.saml.SAMLConstants;
import org.apache.cxf.rs.security.saml.SamlFormOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlHeaderOutInterceptor;

import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.xml.security.exceptions.Base64DecodingException;

/**
 * An Interceptor to "retrieve" a SAML Token, i.e. create one and set it on the message context so
 * that the Saml*Interceptors can write it out in a particular way.
 */
public class SamlRetrievalInterceptor extends AbstractPhaseInterceptor<Message> {

  static {
    WSSConfig.init();
  }

  protected SamlRetrievalInterceptor() {
    super(Phase.WRITE);
    addBefore(SamlFormOutInterceptor.class.getName());
    addBefore(SamlHeaderOutInterceptor.class.getName());
  }

  @Override
  public void handleMessage(Message message) throws Fault {

    try {

      Path path = Paths.get("src/test/resources/saml-response-idp.txt");
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(true);
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

      byte[] decodedBytes =
          org.apache.xml.security.utils.Base64.decode(new String(Files.readAllBytes(path)));
      Document doc = null;
      try (ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes)) {
        doc = dBuilder.parse(bis);
      }

      NodeList nList = doc.getElementsByTagName("saml:Assertion");
      Element token = (Element) nList.item(0);
      message.put(SAMLConstants.SAML_TOKEN_ELEMENT, token);
    } catch (SAXException|IOException|ParserConfigurationException|Base64DecodingException e) {
      e.printStackTrace();
    }
  }
}
