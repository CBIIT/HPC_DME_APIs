package Register.Pojo;

/*
    This is the equivalent of HpcStreamingUploadSource in domain-types/dataTransfer
 */
public class S3StreamingUploadPojo {

    SourceLocationPojo sourceLocation;
    S3AccountPojo account;
    public SourceLocationPojo getSourceLocation() {
      return sourceLocation;
    }
    public void setSourceLocation(SourceLocationPojo sourceLocation) {
      this.sourceLocation = sourceLocation;
    }
    public S3AccountPojo getAccount() {
      return account;
    }
    public void setAccount(S3AccountPojo account) {
      this.account = account;
    }


/*

<xsd:complexType name="HpcStreamingUploadSource">
		<xsd:sequence>
			<xsd:element name="sourceLocation"
				type="hpc-domain-datatransfer:HpcFileLocation" />
			<xsd:element name="account"
				type="hpc-domain-datatransfer:HpcS3Account" />
			<xsd:element name="accessToken" type="xsd:string" />
			<xsd:element name="sourceURL" type="xsd:string" />
			<xsd:element name="sourceSize" type="xsd:long" minOccurs="0" />
			<xsd:element name="sourceInputStream" type="hpc-domain-datatransfer:HpcInputStream" />
		</xsd:sequence>
	</xsd:complexType>
 */
}
