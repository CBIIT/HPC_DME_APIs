/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

public class HpcBatchException extends RuntimeException {

	public HpcBatchException() {
		super();
	}

	public HpcBatchException(String message) {
		super(message);
	}

	public HpcBatchException(String message, Throwable e) {
		super(message, e);
	}

	public HpcBatchException(Throwable e) {
		super(e);
	}
}
