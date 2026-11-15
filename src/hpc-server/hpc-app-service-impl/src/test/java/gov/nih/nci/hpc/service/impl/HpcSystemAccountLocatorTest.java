/**
 * HpcSystemAccountLocatorTest.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

import gov.nih.nci.hpc.dao.HpcGlobusTransferTaskDAO;
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Unit tests for {@link HpcSystemAccountLocator} refresh of credentials from the AWS Secrets
 * Manager.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@ExtendWith(MockitoExtension.class)
public class HpcSystemAccountLocatorTest {

	private static final String AWS_SECRET_NAME = "aws-secret";
	private static final String AWS_SECRET = "{\"AccessKeyId\":\"AKIA1\",\"SecretAccessKey\":\"aws-secret-key\"}";
	private static final String TRINO_SECRET = "{\"hpc.dao.trino.username\":\"trino-user\","
			+ "\"hpc.dao.trino.password\":\"trino-pass\",\"hpc.dao.trino.host\":\"trino-host\"}";

	@Mock
	private HpcSystemAccountDAO systemAccountDAO = null;

	@Mock
	private HpcGlobusTransferTaskDAO globusTransferDAO = null;

	@Mock
	private HpcDataManagementConfigurationLocator dataMgmtConfigLocator = null;

	@Mock
	private BasicDataSource trinoDataSource = null;

	@InjectMocks
	private HpcSystemAccountLocator systemAccountLocator = null;

	private AWSSecretsManager secretsManager = null;
	private MockedStatic<AWSSecretsManagerClientBuilder> secretsManagerBuilderStatic = null;

	@BeforeEach
	void setUp() throws HpcException {
		ReflectionTestUtils.setField(systemAccountLocator, "useSecretsManager", true);
		ReflectionTestUtils.setField(systemAccountLocator, "secretName", AWS_SECRET_NAME);
		ReflectionTestUtils.setField(systemAccountLocator, "region", "us-east-1");
		ReflectionTestUtils.setField(systemAccountLocator, "trinoAvailable", true);

		HpcIntegratedSystemAccount awsAccount = new HpcIntegratedSystemAccount();
		awsAccount.setUsername("AKIA1");
		awsAccount.setPassword("aws-secret-key");
		lenient().when(systemAccountDAO.getSystemAccount(HpcIntegratedSystem.AWS)).thenReturn(List.of(awsAccount));

		secretsManager = mock(AWSSecretsManager.class);
		AWSSecretsManagerClientBuilder builder = mock(AWSSecretsManagerClientBuilder.class, RETURNS_SELF);
		lenient().when(builder.build()).thenReturn(secretsManager);
		secretsManagerBuilderStatic = mockStatic(AWSSecretsManagerClientBuilder.class);
		secretsManagerBuilderStatic.when(AWSSecretsManagerClientBuilder::standard).thenReturn(builder);

		stubSecret(AWS_SECRET_NAME, AWS_SECRET);
	}

	@AfterEach
	void tearDown() {
		secretsManagerBuilderStatic.close();
	}

	@Test
	void testReloadRefreshesTrinoCredentialsAndRestartsPool() throws Exception {
		stubSecret("trino", TRINO_SECRET);
		when(trinoDataSource.getUsername()).thenReturn("old-user");

		systemAccountLocator.reload();

		verify(trinoDataSource).setUsername("trino-user");
		verify(trinoDataSource).setPassword("trino-pass");
		verify(trinoDataSource).restart();
	}

	@Test
	void testReloadDoesNotRestartPoolWhenTrinoCredentialsUnchanged() throws Exception {
		stubSecret("trino", TRINO_SECRET);
		when(trinoDataSource.getUsername()).thenReturn("trino-user");
		when(trinoDataSource.getPassword()).thenReturn("trino-pass");

		systemAccountLocator.reload();

		verify(trinoDataSource, never()).setPassword(any());
		verify(trinoDataSource, never()).restart();
	}

	@Test
	void testReloadSkipsTrinoWhenTrinoNotAvailable() throws Exception {
		ReflectionTestUtils.setField(systemAccountLocator, "trinoAvailable", false);

		systemAccountLocator.reload();

		verify(secretsManager, never()).getSecretValue(argThat(r -> r != null && "trino".equals(r.getSecretId())));
		verify(trinoDataSource, never()).restart();
	}

	@Test
	void testReloadFailsWhenTrinoPasswordMissing() throws Exception {
		stubSecret("trino", "{\"hpc.dao.trino.username\":\"trino-user\"}");

		assertThrows(HpcException.class, () -> systemAccountLocator.reload());
		verify(trinoDataSource, never()).restart();
	}

	@Test
	void testReloadSkipsSecretsManagerWhenDisabled() throws Exception {
		ReflectionTestUtils.setField(systemAccountLocator, "useSecretsManager", false);

		systemAccountLocator.reload();

		verify(secretsManager, never()).getSecretValue(any());
		verify(trinoDataSource, never()).restart();
	}

	private void stubSecret(String secretId, String secretString) {
		lenient().when(secretsManager.getSecretValue(argThat((GetSecretValueRequest r) -> r != null
				&& secretId.equals(r.getSecretId()))))
				.thenReturn(new GetSecretValueResult().withSecretString(secretString));
	}
}
