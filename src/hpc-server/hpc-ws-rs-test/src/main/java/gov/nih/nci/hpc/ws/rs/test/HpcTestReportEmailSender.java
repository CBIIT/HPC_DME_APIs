/**
 * HpcTestReportEmailSender.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import java.nio.charset.Charset;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * <p>
 * HPC test report email sender.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcTestReportEmailSender 
{     
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Mail Sender.
	JavaMailSender mailSender = null;
	
	  // The logger instance.
	  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//  
	
	public HpcTestReportEmailSender()
	{
		JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
		mailSenderImpl.setHost("mailfwd.nih.gov");
		mailSenderImpl.setPort(25);
		Properties props = new Properties();
		props.setProperty("mail.smtp.auth", "false");
		props.setProperty("mail.smtp.starttls.enable", "false");
		mailSenderImpl.setJavaMailProperties(props);
		
		mailSender = mailSenderImpl;
	}

	//---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//  
    
    public void sendTestReport(String testResult, String testTitle, String testReport, String reportEmailAddress)
    {
    	
        try {
             mailSender.send((mimeMessage) ->  
            	             {
            	            	 mimeMessage.setRecipient(Message.RecipientType.TO,
            	                                          new InternetAddress(reportEmailAddress));
            	                 mimeMessage.setSubject("HPC-DM Automated Test " + testResult + " (" + testTitle + ")");
            	                 mimeMessage.setText(testReport, Charset.defaultCharset().name(), "html");
            	             });
             
        } catch(MailException e) {
                logger.error("Failed to send an email: " + e.getMessage());
        }
    }
}

 