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
	
	// Report mail sender
	static private HpcTestReportEmailSender mailSender = new HpcTestReportEmailSender();
	
	// An indicator whether the test failed.
	static private boolean testFailed = false;
	
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
    	if(args.length != 2) {
    	   System.err.println("Usage: mvn exec:java -Dexec.arg\"<dice-test-scripts-home-dir> <reports-dir>\"");
    	   System.exit(1);
    	}
    	
    	// Keep the scripts base directory.
    	String testScriptBaseDir = args[0];
    	String testReportBaseDir = args[1];
    	
    	// Create a report file.
    	Date runDate = Calendar.getInstance().getTime();
    	File reportFile = new File(testReportBaseDir + "/AutoTestReport." + tsDateFormat.format(runDate) + ".html");
    	
    	// Build the report header and table.
    	StringBuilder reportBuilder = new StringBuilder();
    	reportBuilder.append("<h1>HPC-DM Test Report</h1>");
    	reportBuilder.append("<p>Date: " + runDateFormat.format(runDate) + "</p>");
    	
    	// Run the tests.
    	try {
    		 System.out.println("Running bookmarks test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Bookmarks", "test-bookmarks", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running connection test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Connection", "test-connection", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);

    		 System.out.println("Running delete test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Delete", "test-delete", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running disable-authentication test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Disable Authentication", "test-disable-authentication",
                                                             testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running download-data-file test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Download Data File", "test-download-data-file", 
                                                             testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running hpc-authentication test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Authentication", "test-hpc-authentication", 
                                                             testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running miscellaneous test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Miscellaneous", "test-miscellaneous", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running named-queries test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Named Queries", "test-named-queries", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running notifications test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Notifications", "test-notifications", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running permission test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Permission", "test-permission", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running query test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Query", "test-query", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
    		 System.out.println("Running register test...");
    		 FileUtils.writeStringToFile(reportFile, runTest("Register", "test-register", testScriptBaseDir), 
    				                     Charset.defaultCharset(), true);
    	     
       		 System.out.println("Running register-user test...");
       		 FileUtils.writeStringToFile(reportFile, runTest("Register User", "test-register-user", testScriptBaseDir), 
       				                     Charset.defaultCharset(), true);
    	     
       		 System.out.println("Running search-group test...");
       		 FileUtils.writeStringToFile(reportFile, runTest("Search Group", "test-search-group", testScriptBaseDir), 
       				                     Charset.defaultCharset(), true);
    	     
       		 System.out.println("Running search-user test...");
       		 FileUtils.writeStringToFile(reportFile, runTest("Search User", "test-search-user", testScriptBaseDir), 
       				                     Charset.defaultCharset(), true);
    	     
       		 System.out.println("Running user-groups test...");
       		 FileUtils.writeStringToFile(reportFile, runTest("User Groups", "test-user-groups", testScriptBaseDir), 
       				                     Charset.defaultCharset(), true);
       		 
		} catch(Exception e) {
			    testFailed = true;
			    System.err.println("Failed to run automated test" + e.getMessage());
		}
    	
   	    // Notify admin if the test failed.
     	if(testFailed) {
     	   String report = "";
     	   try {
     	        report = FileUtils.readFileToString(reportFile, Charset.defaultCharset());
     	        
     	  } catch(Exception e) {
			      System.err.println("Failed to read report file" + e);
     	  }
     	   
     	  mailSender.sendTestReport(reportFile.getName(), report);
     	}
	}
    
    private static String runTest(String testName, String testScript, 
    		                      String testScriptBaseDir) 
    		                     throws Exception
    {
    	File scriptDirectory = new File(testScriptBaseDir + "/" + testScript);
    	if(!scriptDirectory.isDirectory()) {
    	   throw new IOException("Directory doesn't exist: " + scriptDirectory);
    	}
    	
    	Process process = new ProcessBuilder("dxtest").directory(scriptDirectory).start();
    	process.waitFor();
    	
    	// Parse the summary test file.
    	File testSummaryFile = new File(testScriptBaseDir + "/" + testScript + "/autotest-output/test_summary.txt");
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
    	   testFailed = true;
    	   failuresLastIndex = failuresStartIndex + failures - 1;
    	   testResultsBuilder.append("<table border=\"1\">");
    	   testResultsBuilder.append("<thead>");
    	   testResultsBuilder.append("<tr style=\"text-align: center;\">");
    	   testResultsBuilder.append("<td>Failed Test</td>");
    	   testResultsBuilder.append("<td>Actual Output</td>");
    	   testResultsBuilder.append("<td>Correct Output</td>");
    	   testResultsBuilder.append("</tr>");
    	   testResultsBuilder.append("</thead>");
    	   testResultsBuilder.append("<tbody>");
    	   for(int i = failuresStartIndex; i <= failuresLastIndex; i++) {
    		   String testCaseAbsolutePath = linesArray[i];
    		   String testCase = testCaseAbsolutePath.substring(testCaseAbsolutePath.lastIndexOf('/') + 1, 
    				                                            testCaseAbsolutePath.length());
    		   File actualOutputFile = new File(testCaseAbsolutePath + "/autotest-output/output.txt");
    		   File correctOutputFile = new File(testCaseAbsolutePath + "/correct-output.txt");
    				   
    	       testResultsBuilder.append("<tr>");    	
    	       testResultsBuilder.append("<td>" + testCase + "</td>");
    	       testResultsBuilder.append("<td>" + FileUtils.readFileToString(actualOutputFile, 
    	    		                                                         Charset.defaultCharset()) + 
    	    		                     "</td>");
    	       testResultsBuilder.append("<td>" + FileUtils.readFileToString(correctOutputFile, 
                                                                             Charset.defaultCharset()) +
    	    		                     "</td>");
    	       testResultsBuilder.append("</tr>");
    	   }
    	   testResultsBuilder.append("</tbody>");
    	   testResultsBuilder.append("</table>");
    	   testResultsBuilder.append("<br>&nbsp;</br>");
    	}
    	
    	if(errorMismatch > 0) {
    	   testFailed = true;
           int errorMismatchStartIndex = failuresLastIndex + 2;
           int errorMismatchLastIndex = errorMismatchStartIndex + errorMismatch - 1;
    	   testResultsBuilder.append("<table border=\"1\">");
    	   testResultsBuilder.append("<thead>");
    	   testResultsBuilder.append("<tr style=\"text-align: center;\">");
    	   testResultsBuilder.append("<td>Error Mismatch Test</td>");
    	   testResultsBuilder.append("<td>Actual Error</td>");
    	   testResultsBuilder.append("<td>Expected Error</td>");
    	   testResultsBuilder.append("</tr>");
    	   testResultsBuilder.append("</thead>");
     	   testResultsBuilder.append("<tbody>");
     	   for(int i = errorMismatchStartIndex; i <= errorMismatchLastIndex; i++) {
    		   String testCaseAbsolutePath = linesArray[i];
    		   String testCase = testCaseAbsolutePath.substring(testCaseAbsolutePath.lastIndexOf('/') + 1, 
    				                                            testCaseAbsolutePath.length());
    		   File actualErrorFile = new File(testCaseAbsolutePath + "/autotest-output/error_output.txt");
    		   File expectedErrorFile = new File(testCaseAbsolutePath + "/expected-errors.txt");
    				   
    	       testResultsBuilder.append("<tr>");    	
    	       testResultsBuilder.append("<td>" + testCase + "</td>");
    	       testResultsBuilder.append("<td>" + FileUtils.readFileToString(actualErrorFile, 
    	    		                                                         Charset.defaultCharset()) + 
    	    		                     "</td>");
    	       testResultsBuilder.append("<td>" + FileUtils.readFileToString(expectedErrorFile, 
                                                                             Charset.defaultCharset()) +
    	    		                     "</td>");
    	       testResultsBuilder.append("</tr>");
     	   }
    	   testResultsBuilder.append("</tbody>");
    	   testResultsBuilder.append("</table>");
    	   testResultsBuilder.append("<p>&nbsp;</p>");
    	}
    	
    	return testResultsBuilder.toString();
    }
}

 
