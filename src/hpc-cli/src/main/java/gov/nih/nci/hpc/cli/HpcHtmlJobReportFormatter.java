/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.StringWriter;
import java.util.Properties;

/*
 * The MIT License
 *
 *  Copyright (c) 2015, Mahmoud Ben Hassine (mahmoud@benhassine.fr)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.job.JobReportFormatter;

/**
 * Format a report into HTML format.
 *
 * @author Mahmoud Ben Hassine (mahmoud@benhassine.fr)
 */
public class HpcHtmlJobReportFormatter implements JobReportFormatter<String> {

	/**
	 * The template engine to render reports.
	 */
	private VelocityEngine velocityEngine;

	public HpcHtmlJobReportFormatter() {
		Properties properties = new Properties();
		properties.put("resource.loader", "class");
		properties.put("class.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		velocityEngine = new VelocityEngine(properties);
		velocityEngine.init();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String formatReport(final JobReport jobReport) {
		Template template = velocityEngine.getTemplate("templates/HtmlReport.vm");
		StringWriter stringWriter = new StringWriter();
		Context context = new VelocityContext();
		context.put("report", jobReport);
		context.put("properties", jobReport.getParameters().getSystemProperties().entrySet());
		template.merge(context, stringWriter);
		return stringWriter.toString();
	}

}
