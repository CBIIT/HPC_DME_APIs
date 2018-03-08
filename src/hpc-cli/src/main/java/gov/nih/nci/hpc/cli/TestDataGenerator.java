/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestDataGenerator {

	private static final String DATASET_PART1 = "Dataset,/FNL_SF_Archive/ProjectX1/";
	private static final String DATASET_PART_SEQUENCE_1 = "Dataset";
	private static final String DATASET_PART_SEQUENCE_2 = "Dataset";
	private static final String DATASET_PART_SEQUENCE_3 = "Dataset Description";
	private static final String DATASET_PART2 = ",,konkapv,FNLCR,FNLCR,2/15/2010,FNLCR,PHI Not Present,PII Not Present,Not Encrypted,Not Compressed,CCR,The dataset is missing BAM file.,FastQ,FlowCell ID 1,Run_ID 1,2/10/2010,Illumina-MiSeq,Exome,Library ID 1,Illumina TrueSeq,Illumina TrueSeq,Illumina TrueSeq,Raw ,125bp";

	private static final String DATAFILE_PART1 = "/FNL_SF_Archive/ProjectX1/";
	private static final String DATAFILE_PART_SEQUENCE_1 = "Dataset2";
	private static final String DATAFILE_PART_SEQUENCE_2 = "/";
	private static final String DATAFILE_PART_SEQUENCE_3 = "";
	private static final String DATAFILE_PART_SEQUENCE_4 = "data file description";
	private static final String DATAFILE_PART2 = "samples/output.dat,konkapv,FNLCR,FNLCR,12/24/2013,FNLCR,PHI Not Present,PII Not Present,Not Encrypted,Not Compressed,CCR,The dataset is missing BAM file.,FastQ,FlowCell ID 1,Run_ID 1,2/20/2010,Illumina-MiSeq,Exome,Library ID 1,Illumina TrueSeq,Illumina TrueSeq,Illumina TrueSeq,Raw ,125bp,custom value1";

	public TestDataGenerator() {

	}

	private void generateInputFile(String recCount, String filePath, String type, String start) {
		try {

			String content = "This is the content to write into file";

			File file = new File(filePath);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			int count = Integer.parseInt(recCount);
			int startIndex = Integer.parseInt(start);
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = startIndex; i < count; i++) {
				if (type.equals("collection"))
					content = DATASET_PART1 + DATASET_PART_SEQUENCE_1 + i + "," + DATASET_PART_SEQUENCE_2 + i + ","
							+ DATASET_PART_SEQUENCE_3 + i + "," + DATASET_PART2;
				else
					content = DATAFILE_PART1 + DATAFILE_PART_SEQUENCE_1 + DATAFILE_PART_SEQUENCE_2 + i + ","
							+ DATAFILE_PART_SEQUENCE_3 + i + "," + DATAFILE_PART_SEQUENCE_4 + i + "," + DATAFILE_PART2;
				bw.write(content);
				bw.newLine();
				bw.flush();
			}
			bw.close();

			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TestDataGenerator().generateInputFile(args[0], args[1], args[2], args[3]);
	}
}
