/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.globus;

import java.util.List;

public class GlobusFileList {
	private List<GlobusFile> DATA;

	public List<GlobusFile> getDATA() {
		return DATA;
	}

	public void setDATA(List<GlobusFile> dATA) {
		DATA = dATA;
	}

}
