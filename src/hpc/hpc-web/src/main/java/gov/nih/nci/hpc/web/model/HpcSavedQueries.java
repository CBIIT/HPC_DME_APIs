package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcSavedQueries {
	public List<HpcQuery> queries;

	public List<HpcQuery> getQueries() {
		if(queries == null)
			return new ArrayList<HpcQuery>();
		
		return queries;
	}

	public void setQueries(List<HpcQuery> queries) {
		this.queries = queries;
	}
}
