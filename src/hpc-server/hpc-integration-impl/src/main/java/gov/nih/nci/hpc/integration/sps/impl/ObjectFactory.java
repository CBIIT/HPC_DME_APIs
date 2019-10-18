
package gov.nih.nci.hpc.integration.sps.impl;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.ca.siteminder.authaz._2010._04._15.authaz package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Attribute_QNAME = new QName("http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", "attribute");
    private final static QName _AuthorizationResult_QNAME = new QName("http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", "authorizationResult");
    private final static QName _Authorize_QNAME = new QName("http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", "authorize");
    private final static QName _AuthorizeResponse_QNAME = new QName("http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", "authorizeResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ca.siteminder.authaz._2010._04._15.authaz
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AuthorizationResult }
     * @return AuthorizationResult
     */
    public AuthorizationResult createAuthorizationResult() {
        return new AuthorizationResult();
    }

    /**
     * Create an instance of {@link Attribute }
     * @return Attribute
     */
    public Attribute createAttribute() {
        return new Attribute();
    }

    /**
     * Create an instance of {@link Authorize }
     * @return Authorize
     */
    public Authorize createAuthorize() {
        return new Authorize();
    }

    /**
     * Create an instance of {@link AuthorizeResponse }
     * @return AuthorizeResponse
     */
    public AuthorizeResponse createAuthorizeResponse() {
        return new AuthorizeResponse();
    }

    /**
     * Create an instance of {@link AuthorizationResult.AuthorizationResponses }
     * @return AuthorizationResponses
     */
    public AuthorizationResult.AuthorizationResponses createAuthorizationResultAuthorizationResponses() {
        return new AuthorizationResult.AuthorizationResponses();
    }
    
    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Attribute }{@code >}}
     * @return JAXBElement of type Attribute
     */
    @XmlElementDecl(namespace = "http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", name = "attribute")
    public JAXBElement<Attribute> createAttribute(Attribute value) {
        return new JAXBElement<Attribute>(_Attribute_QNAME, Attribute.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthorizationResult }{@code >}}
     * @return JAXBElement of type AuthorizationResult
     */
    @XmlElementDecl(namespace = "http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", name = "authorizationResult")
    public JAXBElement<AuthorizationResult> createAuthorizationResult(AuthorizationResult value) {
        return new JAXBElement<AuthorizationResult>(_AuthorizationResult_QNAME, AuthorizationResult.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Authorize }{@code >}}
     * @return JAXBElement of type Authorize
     */
    @XmlElementDecl(namespace = "http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", name = "authorize")
    public JAXBElement<Authorize> createAuthorize(Authorize value) {
        return new JAXBElement<Authorize>(_Authorize_QNAME, Authorize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthorizeResponse }{@code >}}
     * @return JAXBElement of type AuthorizeResponse
     */
    @XmlElementDecl(namespace = "http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd", name = "authorizeResponse")
    public JAXBElement<AuthorizeResponse> createAuthorizeResponse(AuthorizeResponse value) {
        return new JAXBElement<AuthorizeResponse>(_AuthorizeResponse_QNAME, AuthorizeResponse.class, null, value);
    }
 

}
