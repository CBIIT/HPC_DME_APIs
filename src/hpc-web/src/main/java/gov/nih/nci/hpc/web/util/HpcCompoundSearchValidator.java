package gov.nih.nci.hpc.web.util;

import java.util.Map;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCompoundQuery;

public class HpcCompoundSearchValidator {
	private Map<String, HpcMetadataQuery> queries;
	private String compoundQuery;

	public HpcCompoundSearchValidator(Map<String, HpcMetadataQuery> queries, String compoundQuery)
			throws HpcWebException {
		if (compoundQuery == null || compoundQuery.trim().length() == 0 || queries == null || queries.size() == 0)
			throw new HpcWebException("Invalid Search criteria");
		this.compoundQuery = compoundQuery;
		this.queries = queries;
	}

	public void validate() throws HpcWebException {
		validateStructure();
	}

	private void validateStructure() throws HpcWebException {
		validateParanthesis();
		validateOperators();
	}

	private void validateParanthesis() throws HpcWebException {
		int start = 0;
		int end = 0;
		for (int i = 0; i < compoundQuery.length(); i++) {
			char ch = compoundQuery.charAt(i);
			if (ch == '(')
				start++;
			else if (ch == ')')
				end++;
		}
		if (start != end)
			throw new HpcWebException("Invalid advanced search criteria. Paranthesis is missing");
	}

	private void validateOperators() throws HpcWebException {
		if (compoundQuery.indexOf(HpcCompoundQuery.CONDITION_AND) == -1
				&& compoundQuery.indexOf(HpcCompoundQuery.CONDITION_OR) == -1)
			throw new HpcWebException("Not a valid advanced search. Operator missing");
		validateAND(compoundQuery);
		validateOR(compoundQuery);
	}

	private void validateAND(String queryStr) {
		if (queryStr.indexOf(HpcCompoundQuery.CONDITION_AND) > 0) {
			String part1 = queryStr.substring(0, queryStr.indexOf(HpcCompoundQuery.CONDITION_AND));
			String part2 = queryStr.substring(queryStr.indexOf(HpcCompoundQuery.CONDITION_AND) + 3, queryStr.length());
			if (part1.trim().length() == 0 || part2.trim().length() == 0)
				throw new HpcWebException("Invalid use of criteria operator AND");
			else
				validateAND(part2);
		}
	}

	private void validateOR(String queryStr) {
		if (queryStr.indexOf(HpcCompoundQuery.CONDITION_OR) > 0) {
			String part1 = queryStr.substring(0, queryStr.indexOf(HpcCompoundQuery.CONDITION_OR));
			String part2 = queryStr.substring(queryStr.indexOf(HpcCompoundQuery.CONDITION_OR) + 3, queryStr.length());
			if (part1.trim().length() == 0 || part2.trim().length() == 0)
				throw new HpcWebException("Invalid use of criteria operator OR");
			else
				validateAND(part2);
		}
	}

}
