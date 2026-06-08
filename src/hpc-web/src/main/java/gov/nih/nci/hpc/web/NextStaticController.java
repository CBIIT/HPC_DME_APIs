/**
 * NextStaticController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * Controller to forward extensionless frontend routes to their .html equivalents.
 * This is needed for Next.js static export App Router to work properly behind Tomcat.
 * </p>
 * 
 * <p>
 * When Next.js static export is used, it creates .html files and .txt RSC payload files.
 * The client-side router expects extensionless routes (e.g., /usage) not /usage.html.
 * This controller forwards extensionless routes to their .html files while allowing
 * Next.js to load the RSC payloads (.txt files) separately.
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
@Controller
public class NextStaticController {

    /**
     * Forward extensionless nested routes to their .html equivalents.
     * 
     * Excludes:
     * - API routes (/api/*)
     * - Next.js internal routes (/_next/*)
     * - Static assets (/static/*, /public/*)
     * - Files with extensions
     * 
     * @param request the HTTP request
     * @return forward to the .html file or null to use default handling
     */
    @GetMapping(value = "/**/{path:[^\\.]*}")
    public String forwardToHtml(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        // Remove context path from URI for comparison
        String pathWithoutContext = uri;
        if (contextPath != null && !contextPath.isEmpty() && !contextPath.equals("/")) {
            if (uri.startsWith(contextPath)) {
                pathWithoutContext = uri.substring(contextPath.length());
            }
        }
        
        // Exclude API routes
        if (pathWithoutContext.startsWith("/api/")) {
            return null;
        }
        
        // Exclude Next.js internal routes
        if (pathWithoutContext.startsWith("/_next/")) {
            return null;
        }
        
        // Exclude static asset routes
        if (pathWithoutContext.startsWith("/static/") || 
            pathWithoutContext.startsWith("/public/") ||
            pathWithoutContext.startsWith("/css/") ||
            pathWithoutContext.startsWith("/js/") ||
            pathWithoutContext.startsWith("/img/") ||
            pathWithoutContext.startsWith("/fonts/") ||
            pathWithoutContext.startsWith("/swagger-ui/")) {
            return null;
        }
        
        // Exclude files with extensions and special files
        if (pathWithoutContext.contains(".") ||
            pathWithoutContext.equals("/favicon.ico") ||
            pathWithoutContext.equals("/robots.txt") ||
            pathWithoutContext.equals("/sitemap.xml")) {
            return null;
        }
        
        // Forward to .html file
        return "forward:" + uri + ".html";
    }
}
