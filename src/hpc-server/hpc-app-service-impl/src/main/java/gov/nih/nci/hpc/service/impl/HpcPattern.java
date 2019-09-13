/**
 * HpcPattern.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;

/**
 * Convenient class to support string pattern matching.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcPattern {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default Constructor for Spring Dependency Injection.
	 */
	private HpcPattern() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Compile a list of patterns
	 *
	 * @param patterns      The list of patterns (strings) to compile.
	 * @param patternType   The type of pattern
	 * @param prefixPattern If set to true, the pattern will be prefixed by a single
	 *                      '/' (if not already sent like that)
	 * @return The list of compiled patterns
	 */
	public List<Pattern> compile(List<String> patterns, HpcPatternType patternType, boolean prefixPattern) {
		List<Pattern> compiledPatterns = new ArrayList<>();
		patterns.forEach(pattern -> compiledPatterns.add(Pattern
				.compile(patternType.equals(HpcPatternType.SIMPLE) ? toRegex(pattern, prefixPattern) : pattern)));

		return compiledPatterns;
	}

	/**
	 * Regex matching on a list of patterns.
	 *
	 * @param patterns A list of patterns to match.
	 * @param input    The string to match.
	 * @return true if the input matched at least one of the patterns, or false
	 *         otherwise.
	 */
	public boolean matches(List<Pattern> patterns, String input) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(input).matches()) {
				return true;
			}
		}

		return false;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert pattern from SIMPLE to REGEX.
	 *
	 * @param pattern       The SIMPLE pattern.
	 * @param prefixPattern If set to true, the pattern will be prefixed by a single
	 *                      '/' (if not already sent like that)
	 * @return The REGEX pattern.
	 */
	private String toRegex(String pattern, boolean prefixPattern) {
		// Ensure the pattern starts with '/'.
		String regex = prefixPattern && !pattern.startsWith("/") ? "/" + pattern : pattern;

		// Convert the '**' to regex.
		regex = regex.replaceAll(Pattern.quote("**"), ".*");

		// Convert the '*' to regex.
		regex = regex.replaceAll("([^\\.])\\*", "$1[^/]*");

		// Convert the '?' to regex.
		return regex.replaceAll(Pattern.quote("?"), ".");
	}
}
