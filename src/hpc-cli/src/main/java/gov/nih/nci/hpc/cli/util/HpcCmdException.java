/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

public class HpcCmdException extends RuntimeException {

	public HpcCmdException() {
		super();
	}

	public HpcCmdException(String message) {
		super(message);
	}

	public HpcCmdException(String message, Throwable e) {
		super(message, e);
	}

	public HpcCmdException(Throwable e) {
		super(e);
	}
}
