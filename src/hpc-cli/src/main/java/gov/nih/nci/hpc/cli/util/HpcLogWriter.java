/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class HpcLogWriter {

	private static HpcLogWriter writer = new HpcLogWriter();

	public synchronized void WriteLog(String logFile, String entry) {

		BufferedWriter bw = null;

		try {

			bw = new BufferedWriter(new FileWriter(logFile, true));
			bw.append(entry);
			bw.newLine();

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				bw.close();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

	public static HpcLogWriter getInstance() {

		return writer;

	}

}
