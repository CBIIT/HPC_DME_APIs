package gov.nih.nci.hpc.web.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.xjc.runtime.JAXBToStringStyle;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;

public class HpcCompoundQuery {
	public static final String START_PARANTHESIS = "(";
	public static final String END_PARANTHESIS = ")";
	public static final String CONDITION_OR = "OR";
	public static final String CONDITION_AND = "AND";

	private String criteria;
	private Map<String, HpcMetadataQuery> queries;
	private Map<String, HpcCompoundMetadataQuery> compoundQueries;

	public String getCriteria() {
		return criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

	public Map<String, HpcMetadataQuery> getQueries() {
		if (queries == null)
			queries = new HashMap<String, HpcMetadataQuery>();
		return queries;
	}

	public void setQueries(Map<String, HpcMetadataQuery> queries) {
		this.queries = queries;
	}

	public Map<String, HpcCompoundMetadataQuery> getCompoundQueries() {
		if (compoundQueries == null)
			compoundQueries = new HashMap<String, HpcCompoundMetadataQuery>();
		return compoundQueries;
	}

	public void setCompoundQueries(Map<String, HpcCompoundMetadataQuery> compoundQueries) {
		this.compoundQueries = compoundQueries;
	}
	
	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, JAXBToStringStyle.SIMPLE_STYLE);
    }
}
