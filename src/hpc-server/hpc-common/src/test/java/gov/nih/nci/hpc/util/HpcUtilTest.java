/**
 * HpcUtilTest.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.util;

import static gov.nih.nci.hpc.util.HpcUtil.exec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HpcUtil exec() tests.
 */
public class HpcUtilTest {

	@TempDir
	Path tempDir;

	@Test
	public void testExecReturnsOutput() throws HpcException {
		assertEquals("hello\n", exec(Arrays.asList("echo", "hello"), null, null, null));
	}

	@Test
	public void testExecPassesArgumentsAsIs() throws HpcException {
		Path marker = tempDir.resolve("marker");
		List<String> arguments = Arrays.asList("a b", "x; touch " + marker, "$(touch " + marker + ")",
				"`touch " + marker + "`", "'quoted'", "\"double\"", "*", "a|b&c>d");

		List<String> command = new ArrayList<>(Arrays.asList("printf", "%s\\n"));
		command.addAll(arguments);

		assertEquals(String.join("\n", arguments) + "\n", exec(command, null, null, null));
		assertFalse(Files.exists(marker));
	}

	@Test
	public void testExecFailureThrowsWithErrorOutput() {
		HpcException e = assertThrows(HpcException.class,
				() -> exec(Arrays.asList("ls", tempDir.resolve("does-not-exist").toString()), null, null, null));
		assertEquals(HpcErrorType.UNEXPECTED_ERROR, e.getErrorType());
		assertTrue(e.getMessage().contains("does-not-exist"));
	}

	@Test
	public void testExecMissingProgramThrows() {
		HpcException e = assertThrows(HpcException.class,
				() -> exec(Arrays.asList(tempDir.resolve("no-such-program").toString()), null, null, null));
		assertEquals(HpcErrorType.UNEXPECTED_ERROR, e.getErrorType());
	}

	@Test
	public void testExecEnvironmentReplacesInheritedEnvironment() throws HpcException {
		assertEquals("HPC_TEST=a b;c\n",
				exec(Arrays.asList("/usr/bin/env"), null, Map.of("HPC_TEST", "a b;c"), null));
	}

	@Test
	public void testExecWorkingDirectory() throws Exception {
		assertEquals(tempDir.toRealPath().toString() + "\n",
				exec(Arrays.asList("pwd", "-P"), null, null, tempDir.toFile()));
	}

	@Test
	public void testExecLargeOutputDoesNotBlock() throws HpcException {
		String output = exec(Arrays.asList("sh", "-c", "head -c 300000 /dev/zero >&2; head -c 300000 /dev/zero"),
				null, null, null);
		assertEquals(300000, output.length());
	}

	@Test
	public void testExecEmptyArgument() throws HpcException {
		assertEquals("[]", exec(Arrays.asList("printf", "[%s]", ""), null, null, null));
	}

	@Test
	public void testExecInvalidCommand() {
		assertThrows(HpcException.class, () -> exec(null, null, null, null));
		assertThrows(HpcException.class, () -> exec(Collections.emptyList(), null, null, null));
		assertThrows(HpcException.class, () -> exec(Arrays.asList("echo", null), null, null, null));
		assertThrows(HpcException.class, () -> exec(Arrays.asList(""), null, null, null));
	}

	@Test
	public void testExecInterruptTerminatesCommand() throws Exception {
		Path pidFile = tempDir.resolve("pid");
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		AtomicBoolean interruptStatus = new AtomicBoolean();
		Thread caller = new Thread(() -> {
			try {
				exec(Arrays.asList("sh", "-c", "echo $$ > \"$0\"; exec sleep 60", pidFile.toString()), null, null,
						null);
			} catch (Throwable t) {
				thrown.set(t);
			}
			interruptStatus.set(Thread.currentThread().isInterrupted());
		});
		caller.start();

		long deadline = System.currentTimeMillis() + 10000;
		while (!Files.exists(pidFile) || Files.readString(pidFile).isBlank()) {
			assertTrue(System.currentTimeMillis() < deadline, "command did not start");
			Thread.sleep(50);
		}
		ProcessHandle command = ProcessHandle.of(Long.parseLong(Files.readString(pidFile).trim())).orElseThrow();

		caller.interrupt();
		caller.join(5000);

		assertFalse(caller.isAlive(), "exec() did not return after interrupt");
		assertTrue(thrown.get() instanceof HpcException);
		assertTrue(interruptStatus.get());
		command.onExit().get(5, TimeUnit.SECONDS);
		assertFalse(command.isAlive());
	}
}
