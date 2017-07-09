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
    
    public void sendTestReport(String testTitle, String testReport)
    {
        try {
             mailSender.send((mimeMessage) ->  
            	             {
            	            	 mimeMessage.setRecipient(Message.RecipientType.TO,
            	                                          new InternetAddress("rosenbergea@nih.gov"));
            	                 mimeMessage.setSubject("HPC-DM Automated Test Failed (" + testTitle + ")");
            	                 mimeMessage.setText(testReport, Charset.defaultCharset().name(), "html");
            	             });
             
        } catch(MailException e) {
                System.err.println("Failed to send an email: " + e.getMessage());
        }
    }
}

 