/**
 * HpcException.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;

/**
 * <p>
 * The HPC exception.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcException extends Exception implements java.io.Serializable {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// UID.
	private final static long serialVersionUID = 1L;

	// The error type value.
	private HpcErrorType errorType = null;

	// The request reject reason value.
	private HpcRequestRejectReason requestRejectReason = null;

	// The integrated system that is the source of the exception.
	private HpcIntegratedSystem integratedSystem = null;

	// Indicator to suppress stack trace logging.
	private boolean suppressStackTraceLogging = false;

	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcException() {
	}

	/**
	 * Constructs a new HpcException with a given message and error type.
	 *
	 * @param message   The message for the exception, normally the cause.
	 * @param errorType The type of the error, often the subsystem that is the
	 *                  source of the error.
	 */
	public HpcException(String message, HpcErrorType errorType) {
		super(message);
		setErrorType(errorType);
	}

	/**
	 * Constructs a new HpcException with a given message, error type, and
	 * integrated-system.
	 *
	 * @param message          The message for the exception.
	 * @param errorType        The type of the error, often the subsystem that is
	 *                         the source of the error.
	 * @param integratedSystem The integrated system which is the source of the
	 *                         error.
	 */
	public HpcException(String message, HpcErrorType errorType, HpcIntegratedSystem integratedSystem) {
		super(message);
		setErrorType(errorType);
		setIntegratedSystem(integratedSystem);
	}

	/**
	 * Constructs a new HpcException with a given message and a rejection reason.
	 *
	 * @param message             The message for the exception, normally the cause.
	 * @param requestRejectReason The reason code for a request rejection.
	 */
	public HpcException(String message, HpcRequestRejectReason requestRejectReason) {
		super(message);
		setErrorType(HpcErrorType.REQUEST_REJECTED);
		setRequestRejectReason(requestRejectReason);
	}

	/**
	 * Constructs a new HpcException with a given message, and a Throwable cause
	 *
	 * @param message The message for the exception.
	 * @param cause   The root cause Throwable.
	 */
	public HpcException(String message, Throwable cause) {
		super(message, cause);

		// Propagate the error type, and reject reason if the cause is a HpcException.
		if (cause instanceof HpcException) {
			setErrorType(((HpcException) cause).getErrorType());
			setRequestRejectReason(((HpcException) cause).getRequestRejectReason());
		}
	}

	/**
	 * Constructs a new HpcException with a given message, error type and a
	 * Throwable cause.
	 *
	 * @param message   The message for the exception.
	 * @param errorType The type of the error, often the subsystem that is the
	 *                  source of the error.
	 * @param cause     The root cause Throwable.
	 */
	public HpcException(String message, HpcErrorType errorType, Throwable cause) {
		super(message, cause);
		setErrorType(errorType);
	}

	/**
	 * Constructs a new HpcException with a given message, error type,
	 * integrated-system and a Throwable cause.
	 *
	 * @param message          The message for the exception.
	 * @param errorType        The type of the error, often the subsystem that is
	 *                         the source of the error.
	 * @param integratedSystem The integrated system which is the source of the
	 *                         error.
	 * @param cause            The root cause Throwable.
	 */
	public HpcException(String message, HpcErrorType errorType, HpcIntegratedSystem integratedSystem, Throwable cause) {
		super(message, cause);
		setErrorType(errorType);
		setIntegratedSystem(integratedSystem);
	}

	/**
	 * Constructs a new HpcException with a given message, error type and a
	 * Throwable cause
	 *
	 * @param message             The message for the exception.
	 * @param requestRejectReason The reason for rejecting the request.
	 * @param cause               The root cause Throwable.
	 */
	public HpcException(String message, HpcRequestRejectReason requestRejectReason, Throwable cause) {
		super(message, cause);
		setErrorType(HpcErrorType.REQUEST_REJECTED);
		setRequestRejectReason(requestRejectReason);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	@Override
	public String toString() {
		return super.toString() + "[" + errorType + "]";
	}

	/**
	 * Get the error type.
	 *
	 * @return The error type.
	 */
	public HpcErrorType getErrorType() {
		return errorType;
	}

	/**
	 * Set the error type.
	 *
	 * @param errorType The error type.
	 */
	public void setErrorType(HpcErrorType errorType) {
		this.errorType = errorType;
	}

	/**
	 * Get the request reject reason.
	 *
	 * @return The request reject reason.
	 */
	public HpcRequestRejectReason getRequestRejectReason() {
		return requestRejectReason;
	}

	/**
	 * Set the request reject reason.
	 *
	 * @param requestRejectReason The request reject reason..
	 */
	public void setRequestRejectReason(HpcRequestRejectReason requestRejectReason) {
		this.requestRejectReason = requestRejectReason;
	}

	/**
	 * Get the integrated system (if null then the source of the exception is not an
	 * integrated system).
	 *
	 * @return The integrated system.
	 */
	public HpcIntegratedSystem getIntegratedSystem() {
		return integratedSystem;
	}

	/**
	 * Set the integration system.
	 *
	 * @param integratedSystem The integrated system that is the source of the
	 *                         exception.
	 */
	public void setIntegratedSystem(HpcIntegratedSystem integratedSystem) {
		this.integratedSystem = integratedSystem;
	}

	/**
	 * Get the stack trace.
	 *
	 * @return The stack trace.
	 */
	public String getStackTraceString() {
		StringWriter writer = new StringWriter();
		printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/**
	 * Set the suppress stack trace logging indicator.
	 *
	 * @param suppressStackTraceLogging Indicator to supress stack trace.
	 * @return The exception w/ suppress stack trace indicator set.
	 */
	public HpcException withSuppressStackTraceLogging(boolean suppressStackTraceLogging) {
		this.suppressStackTraceLogging = suppressStackTraceLogging;
		return this;
	}

	/**
	 * Get the suppress stack trace logging indicator.
	 * 
	 * @return The suppress stack trace logging indicator.
	 */
	public boolean getSuppressStackTraceLogging() {
		return suppressStackTraceLogging;
	}
}
