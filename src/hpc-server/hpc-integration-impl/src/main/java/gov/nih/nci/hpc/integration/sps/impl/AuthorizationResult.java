
package gov.nih.nci.hpc.integration.sps.impl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for authorizationResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="authorizationResult"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="resultCode" type="{http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd}authorizationResultCodes" minOccurs="0"/&gt;
 *         &lt;element name="sessionToken" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="identityToken" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="authorizationResponses" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="response" type="{http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd}attribute" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authorizationResult", propOrder = {
    "message",
    "resultCode",
    "sessionToken",
    "identityToken",
    "authorizationResponses"
})
@XmlRootElement(name = "authorizationResult", namespace = "")
public class AuthorizationResult {

	@XmlElement
    protected String message;
	@XmlElement
    @XmlSchemaType(name = "string")
    protected AuthorizationResultCodes resultCode;
	@XmlElement
    protected String sessionToken;
	@XmlElement
    protected String identityToken;
	@XmlElement
    protected AuthorizationResult.AuthorizationResponses authorizationResponses;

    /**
     * Gets the value of the message property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the value of the message property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMessage(String value) {
        this.message = value;
    }

    /**
     * Gets the value of the resultCode property.
     * 
     * @return
     *     possible object is
     *     {@link AuthorizationResultCodes }
     *     
     */
    public AuthorizationResultCodes getResultCode() {
        return resultCode;
    }

    /**
     * Sets the value of the resultCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link AuthorizationResultCodes }
     *     
     */
    public void setResultCode(AuthorizationResultCodes value) {
        this.resultCode = value;
    }

    /**
     * Gets the value of the sessionToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSessionToken() {
        return sessionToken;
    }

    /**
     * Sets the value of the sessionToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSessionToken(String value) {
        this.sessionToken = value;
    }

    /**
     * Gets the value of the identityToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentityToken() {
        return identityToken;
    }

    /**
     * Sets the value of the identityToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentityToken(String value) {
        this.identityToken = value;
    }

    /**
     * Gets the value of the authorizationResponses property.
     * 
     * @return
     *     possible object is
     *     {@link AuthorizationResult.AuthorizationResponses }
     *     
     */
    public AuthorizationResult.AuthorizationResponses getAuthorizationResponses() {
        return authorizationResponses;
    }

    /**
     * Sets the value of the authorizationResponses property.
     * 
     * @param value
     *     allowed object is
     *     {@link AuthorizationResult.AuthorizationResponses }
     *     
     */
    public void setAuthorizationResponses(AuthorizationResult.AuthorizationResponses value) {
        this.authorizationResponses = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="response" type="{http://www.ca.com/siteminder/authaz/2010/04/15/authaz.xsd}attribute" maxOccurs="unbounded" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "response"
    })
    public static class AuthorizationResponses {

        protected List<Attribute> response;

        /**
         * Gets the value of the response property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the response property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getResponse().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Attribute }
         * 
         * @return response list of name value pair
         */
        public List<Attribute> getResponse() {
            if (response == null) {
                response = new ArrayList<Attribute>();
            }
            return this.response;
        }

    }

}
