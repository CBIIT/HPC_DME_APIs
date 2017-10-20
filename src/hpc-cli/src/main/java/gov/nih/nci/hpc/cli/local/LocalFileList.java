/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.util.List;

public class LocalFileList {
	private List<LocalFile> DATA;

	public List<LocalFile> getDATA() {
		return DATA;
	}

	public void setDATA(List<LocalFile> dATA) {
		DATA = dATA;
	}

}
