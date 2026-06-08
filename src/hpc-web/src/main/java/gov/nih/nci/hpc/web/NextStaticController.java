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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping({"/", "/global", "/usage"})
    public String forwardKnownPages(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "forward:" + uri + ".html";
    }

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
    public String forwardExtensionlessPages(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        String pathWithoutContext = uri;
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath) && uri.startsWith(contextPath)) {
            pathWithoutContext = uri.substring(contextPath.length());
        }

        // Do not rewrite API, swagger, Next internals, or static folders.
        if (pathWithoutContext.startsWith("/api/")
                || pathWithoutContext.startsWith("/swagger-ui")
                || pathWithoutContext.startsWith("/_next/")
                || pathWithoutContext.startsWith("/static/")
                || pathWithoutContext.startsWith("/public/")
                || pathWithoutContext.startsWith("/css/")
                || pathWithoutContext.startsWith("/js/")
                || pathWithoutContext.startsWith("/img/")
                || pathWithoutContext.startsWith("/fonts/")) {
            return null;
        }

        // Skip files and extension-based resources (.txt, .js, .css, .ico, etc.).
        if (pathWithoutContext.contains(".")) {
            return null;
        }

        // Restrict catch-all rewrite to known exported pages to avoid accidental 404 forwards.
        if (!"/global".equals(pathWithoutContext) && !"/usage".equals(pathWithoutContext) && !"/".equals(pathWithoutContext)) {
            return null;
        }

        return "forward:" + uri + ".html";
    }
}