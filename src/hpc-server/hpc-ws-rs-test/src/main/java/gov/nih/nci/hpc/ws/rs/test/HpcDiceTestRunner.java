/**
 * HpcDiceTestRunner.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * <p>
 * HPC DICE Tests runner.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDiceTestRunner 
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Date formatter to format report timestamp.
	static private DateFormat tsDateFormat = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
	
	// Date formatter to format report run time.
	static private DateFormat runDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
	
	// The test script base dir.
	static private String testBaseDir = null;
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor is disabled.
     * 
     */
    private HpcDiceTestRunner() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    public static void main(String [] args)
	{
    	if(args.length != 1) {
    	   System.err.println("Test scripts base directory not provided");
    	   System.exit(1);
    	}
    	
    	// Keep the scripts base directory.
    	testBaseDir = args[0];
    	
    	// Create a report file.
    	Date runDate = Calendar.getInstance().getTime();
    	File reportFile = new File("AutoTestReport." + tsDateFormat.format(runDate) + ".html");
    	
    	// Build the report header and table.
    	StringBuilder reportBuilder = new StringBuilder();
    	reportBuilder.append("<h1>HPC-DM Test Report</h1>");
    	reportBuilder.append("<p>Date: " + runDateFormat.format(runDate) + "</p>");
    	
    	// Run the tests.
    	try {
    		 System.out.println("Running bookmarks test...");
    	     reportBuilder.append(runTest("Bookmarks", "test-bookmarks"));
    	     
    		 System.out.println("Running connection test...");
    	     reportBuilder.append(runTest("Connection", "test-connection"));
    	     
    		 System.out.println("Running delete test...");
    	     reportBuilder.append(runTest("Delete", "test-delete"));
    	     
    		 System.out.println("Running disable-authentication test...");
    	     reportBuilder.append(runTest("Disable Authentication", "test-disable-authentication"));
    	     
    		 System.out.println("Running download-data-file test...");
    	     reportBuilder.append(runTest("Download Data File", "test-download-data-file"));
    	     
    		 System.out.println("Running hpc-authentication test...");
    	     reportBuilder.append(runTest("Authentication", "test-hpc-authentication"));
    	     
    		 System.out.println("Running miscellaneous test...");
    	     reportBuilder.append(runTest("Miscellaneous", "test-miscellaneous"));
    	     
    		 System.out.println("Running named-queries test...");
    	     reportBuilder.append(runTest("Named Queries", "test-named-queries"));
    	     
    		 System.out.println("Running notifications test...");
    	     reportBuilder.append(runTest("Notifications", "test-notifications"));
    	     
    		 System.out.println("Running permission test...");
    	     reportBuilder.append(runTest("Permission", "test-permission"));
    	     
    		 System.out.println("Running query test...");
    	     reportBuilder.append(runTest("Query", "test-query"));
    	     
    		 System.out.println("Running register test...");
    	     reportBuilder.append(runTest("Register", "test-register"));
    	     
       		 System.out.println("Running register-user test...");
    	     reportBuilder.append(runTest("Register User", "test-register-user"));
    	     
       		 System.out.println("Running search-group test...");
    	     reportBuilder.append(runTest("Search Group", "test-search-group"));
    	     
       		 System.out.println("Running search-user test...");
    	     reportBuilder.append(runTest("Search User", "test-search-user"));
    	     
       		 System.out.println("Running user-groups test...");
    	     reportBuilder.append(runTest("User Groups", "test-user-groups"));
		
		     FileUtils.writeStringToFile(reportFile, reportBuilder.toString(), Charset.defaultCharset());
		     
		} catch(Exception e) {
			    System.err.println("Failed to run automated test" + e.getMessage());
		}
	}
    
    private static String runTest(String testName, String testScript) throws Exception
    {
    	File scriptDirectory = new File(testBaseDir + "/" + testScript);
    	if(!scriptDirectory.isDirectory()) {
    	   throw new IOException("Directory doesn't exist: " + scriptDirectory);
    	}
    	
    	Process process = new ProcessBuilder("dxtest").directory(scriptDirectory).start();
    	process.waitFor();
    	
    	// Parse the summary test file.
    	File testSummaryFile = new File(testBaseDir + "/" + testScript + "/autotest-output/test_summary.txt");
    	List<String> linesList = FileUtils.readLines(testSummaryFile, Charset.defaultCharset());
    	String[] linesArray = new String[linesList.size()];
        linesArray = linesList.toArray(linesArray);

        String testSummaryLine = linesArray[0].replace("Test count: ", "").replaceAll(" test failures: ", "").replaceAll(" error output mismatches: ", "");
        String[] testSummary = new String[3];
        testSummary = testSummaryLine.split(";");
        
        int count = Integer.valueOf(testSummary[0]);
        int failures = Integer.valueOf(testSummary[1]);
        int errorMismatch = Integer.valueOf(testSummary[2]);
        
    	// Create a summary HTML for this test
    	StringBuilder testResultsBuilder = new StringBuilder();
    	
    	testResultsBuilder.append("<h2>Test: " + testName + "</h2>");
    	testResultsBuilder.append("<table border=\"1\">");
    	testResultsBuilder.append("<thead>");
    	testResultsBuilder.append("<tr style=\"text-align: center;\">");
    	testResultsBuilder.append("<td>Count</td>");
    	testResultsBuilder.append("<td>Failures</td>");
    	testResultsBuilder.append("<td>Error Mismatch</td>");
    	testResultsBuilder.append("</tr>");
    	testResultsBuilder.append("</thead>");
    	testResultsBuilder.append("<tbody>");
    	testResultsBuilder.append("<tr>");
    	testResultsBuilder.append("<td>" + count + "</td>");
    	testResultsBuilder.append("<td>" + failures + "</td>");
    	testResultsBuilder.append("<td>" + errorMismatch + "</td>");
    	testResultsBuilder.append("</tr>");
    	testResultsBuilder.append("</tbody>");
    	testResultsBuilder.append("</table>");
    	testResultsBuilder.append("<br>&nbsp;</br>");
    	
    	int failuresStartIndex = 2;
    	int failuresLastIndex = 0;
    	if(failures > 0) {
    	   failuresLastIndex = failuresStartIndex + failures - 1;
    	   testResultsBuilder.append("<table border=\"1\">");
    	   testResultsBuilder.append("<thead>");
    	   testResultsBuilder.append("<tr style=\"text-align: center;\">");
    	   testResultsBuilder.append("<td>Failures</td>");
    	   testResultsBuilder.append("</tr>");
    	   testResultsBuilder.append("</thead>");
    	   testResultsBuilder.append("<tbody>");
    	   for(int i = failuresStartIndex; i <= failuresLastIndex; i++) {
    	       testResultsBuilder.append("<tr>");    	
    	       testResultsBuilder.append("<td>" + linesArray[i] + "</td>");
    	       testResultsBuilder.append("</tr>");
    	   }
    	   testResultsBuilder.append("</tbody>");
    	   testResultsBuilder.append("</table>");
    	   testResultsBuilder.append("<br>&nbsp;</br>");
    	}
    	
    	if(errorMismatch > 0) {
           int errorMismatchStartIndex = failuresLastIndex + 2;
           int errorMismatchLastIndex = errorMismatchStartIndex + errorMismatch - 1;
    	   testResultsBuilder.append("<table border=\"1\">");
    	   testResultsBuilder.append("<thead>");
    	   testResultsBuilder.append("<tr style=\"text-align: center;\">");
    	   testResultsBuilder.append("<td>Error Mismatch</td>");
    	   testResultsBuilder.append("</tr>");
    	   testResultsBuilder.append("</thead>");
     	   testResultsBuilder.append("<tbody>");
     	   for(int i = errorMismatchStartIndex; i <= errorMismatchLastIndex; i++) {
    	       testResultsBuilder.append("<tr>");    	
    	       testResultsBuilder.append("<td>" + linesArray[i] + "</td>");
    	       testResultsBuilder.append("</tr>");
     	   }
    	   testResultsBuilder.append("</tbody>");
    	   testResultsBuilder.append("</table>");
    	   testResultsBuilder.append("<p>&nbsp;</p>");
    	}
    	
    	return testResultsBuilder.toString();
    }
}

 
