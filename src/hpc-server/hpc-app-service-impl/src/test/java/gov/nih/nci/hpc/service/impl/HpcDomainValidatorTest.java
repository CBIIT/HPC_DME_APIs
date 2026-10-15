package gov.nih.nci.hpc.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;

public class HpcDomainValidatorTest {

	@Test
	public void testIsValidAsperaAccount() {
		assertTrue(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "aspera.example.com")).getValid());
		assertTrue(HpcDomainValidator.isValidAsperaAccount(asperaAccount("first.last@nih.gov", "10.0.0.1")).getValid());
		assertTrue(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user_1-a", "[2001:db8::1]")).getValid());

		assertFalse(HpcDomainValidator.isValidAsperaAccount(null).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("", "host")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("-user", "host")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user name", "host")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user;id", "host")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "-host")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "host:/dir")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "host name")).getValid());
		assertFalse(HpcDomainValidator.isValidAsperaAccount(asperaAccount("user", "host$(id)")).getValid());
	}

	@Test
	public void testIsValidAsperaDestinationLocation() {
		assertTrue(isValidAsperaDestinationLocation("/dest dir", "sub dir/file name.txt", false));
		assertTrue(isValidAsperaDestinationLocation("dest", "file.txt", false));
		assertTrue(isValidAsperaDestinationLocation("/dest", "collection", true));
		assertTrue(isValidAsperaDestinationLocation("/dest", "collection/", true));

		assertFalse(isValidAsperaDestinationLocation("", "file.txt", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "", false));
		assertFalse(isValidAsperaDestinationLocation("-dest", "file.txt", false));
		assertFalse(isValidAsperaDestinationLocation("/dest\n", "file.txt", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "file\u0000.txt", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "dir/-file", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "dir/", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "dir/.", false));
		assertFalse(isValidAsperaDestinationLocation("/dest", "dir/..", false));
		assertFalse(isValidAsperaDestinationLocation("-dest", "collection", true));
		assertFalse(isValidAsperaDestinationLocation("/dest", "collection\r", true));
	}

	@Test
	public void testIsValidCallerObjectId() {
		assertTrue(HpcDomainValidator.isValidCallerObjectId(null).getValid());
		assertTrue(HpcDomainValidator.isValidCallerObjectId("").getValid());
		assertTrue(HpcDomainValidator.isValidCallerObjectId("/project/sample 1/file.txt").getValid());
		assertTrue(HpcDomainValidator.isValidCallerObjectId("a..b/c").getValid());

		assertFalse(HpcDomainValidator.isValidCallerObjectId("../file.txt").getValid());
		assertFalse(HpcDomainValidator.isValidCallerObjectId("/project/../file.txt").getValid());
		assertFalse(HpcDomainValidator.isValidCallerObjectId("/project/..").getValid());
		assertFalse(HpcDomainValidator.isValidCallerObjectId("/project/file\n.txt").getValid());
	}

	@Test
	public void testIsValidGroupName() {
		assertTrue(HpcDomainValidator.isValidGroupName("hpc_group"));
		assertTrue(HpcDomainValidator.isValidGroupName("NCI-HPC.group"));

		assertFalse(HpcDomainValidator.isValidGroupName(null));
		assertFalse(HpcDomainValidator.isValidGroupName(""));
		assertFalse(HpcDomainValidator.isValidGroupName("-R"));
		assertFalse(HpcDomainValidator.isValidGroupName("owner:group"));
		assertFalse(HpcDomainValidator.isValidGroupName("../group"));
		assertFalse(HpcDomainValidator.isValidGroupName("group\n"));
	}

	private HpcAsperaAccount asperaAccount(String user, String host) {
		HpcAsperaAccount account = new HpcAsperaAccount();
		account.setUser(user);
		account.setHost(host);
		account.setPassword("password");
		return account;
	}

	private boolean isValidAsperaDestinationLocation(String fileContainerId, String fileId, boolean bulkDownload) {
		HpcFileLocation location = new HpcFileLocation();
		location.setFileContainerId(fileContainerId);
		location.setFileId(fileId);
		return HpcDomainValidator.isValidAsperaDestinationLocation(location, bulkDownload).getValid();
	}
}
