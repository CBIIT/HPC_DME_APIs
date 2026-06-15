/**
 * HpcSessionController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;



/**
 * <p>
 * Session controller to return session attributes to front end
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/sessionMap")
@ResponseBody
public class HpcSessionController extends AbstractHpcController {	
	
	@RequestMapping(method = RequestMethod.GET)
	public Map<String, Object> getSessionMap(HttpSession session) {
        Map<String, Object> sessionData = new HashMap<>();
        session.getAttributeNames().asIterator().forEachRemaining(name -> sessionData.put(name, session.getAttribute(name)));
        return sessionData;
    }

}
