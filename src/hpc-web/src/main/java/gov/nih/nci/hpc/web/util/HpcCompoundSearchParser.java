package gov.nih.nci.hpc.web.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCompoundQuery;

public class HpcCompoundSearchParser {

	private HpcCompoundMetadataQuery compoundQuery;
	
	private Integer counter = 1;
	
	private StringBuilder sb = new StringBuilder();

	public HpcCompoundSearchParser(HpcCompoundMetadataQuery compoundQuery) throws HpcWebException {
		if (compoundQuery == null || (compoundQuery.getQueries() == null || compoundQuery.getQueries().isEmpty()) &&
				(compoundQuery.getCompoundQueries() == null || compoundQuery.getCompoundQueries().isEmpty())
				|| compoundQuery.getOperator() == null)
			throw new HpcWebException("Invalid compoundQuery");
		this.compoundQuery = compoundQuery;
	}

	public HpcCompoundQuery parse() throws HpcWebException {

		// For each HpcMetadataQuery,
		// name the query and 
		// build the criteria string.
		Map<String, HpcMetadataQuery> queries = new HashMap<String, HpcMetadataQuery>();
		HpcCompoundQuery query = new HpcCompoundQuery();
		query.setQueries(queries);
		
		query = parseAndBuildCriteria(compoundQuery, query);
		query.setCriteria(sb.toString());
		return query;

	}
	
	private HpcCompoundQuery parseAndBuildCriteria(HpcCompoundMetadataQuery compoundQuery, HpcCompoundQuery query) {
		
		sb.append("(");
		
		boolean first = true;
		for(HpcCompoundMetadataQuery cq: compoundQuery.getCompoundQueries()) {
			if (!first)
				sb.append(" " + compoundQuery.getOperator().value() + " ");
			query = parseAndBuildCriteria(cq, query);
			first = false;
		}
		
		first = true;
		for(HpcMetadataQuery q: compoundQuery.getQueries()) {
			if(first && CollectionUtils.isNotEmpty(compoundQuery.getCompoundQueries()))
				sb.append(" " + compoundQuery.getOperator().value() + " ");
			query.getQueries().put("A" + counter.toString(), q);
			if(first)
				sb.append("A" + counter.toString());
			else
				sb.append(" " + compoundQuery.getOperator().value() + " A" + counter.toString());
			first = false;
			counter++;	
		}
		
		sb.append(")");
		return query;
	}

	public static void main(String args[]) {
		
		HpcMetadataQueryLevelFilter levelFilter = new HpcMetadataQueryLevelFilter();
		levelFilter.setLabel("Sample");
		levelFilter.setOperator(HpcMetadataQueryOperator.EQUAL);
		HpcCompoundQuery query = new HpcCompoundQuery();
		HpcCompoundQuery parsedQuery = null;
		
		Map<String, HpcMetadataQuery> queries = new HashMap<String, HpcMetadataQuery>();
		HpcMetadataQuery A1 = new HpcMetadataQuery();
		A1.setLevelFilter(levelFilter);
		A1.setAttribute("library_id");
		A1.setValue("RS-122-2101");
		A1.setOperator(HpcMetadataQueryOperator.EQUAL);

		HpcMetadataQuery A2 = new HpcMetadataQuery();
		A2.setLevelFilter(levelFilter);
		A2.setAttribute("library_name");
		A2.setValue("Custom Library Prep");
		A2.setOperator(HpcMetadataQueryOperator.EQUAL);

		HpcMetadataQuery A3 = new HpcMetadataQuery();
		A3.setLevelFilter(levelFilter);
		A3.setAttribute("sample_name");
		A3.setValue("Sample_CRISPR_HZ_Luo_3_N701");
		A3.setOperator(HpcMetadataQueryOperator.EQUAL);

		HpcMetadataQuery A4 = new HpcMetadataQuery();
		A4.setLevelFilter(levelFilter);
		A4.setAttribute("sfqc_sample_concentration");
		A4.setValue("18");
		A4.setOperator(HpcMetadataQueryOperator.EQUAL);

		HpcMetadataQuery A5 = new HpcMetadataQuery();
		A5.setLevelFilter(levelFilter);
		A5.setAttribute("source_provider");
		A5.setValue("NIH");
		A5.setOperator(HpcMetadataQueryOperator.EQUAL);

		queries.put("A1", A1);
		queries.put("A2", A2);
		queries.put("A3", A3);
		queries.put("A4", A4);
		queries.put("A5", A5);
		
		// Test1
		String criteria = "((A1 AND A2) OR (A3 AND A4)) OR A5 ";
		query.setQueries(queries);
		query.setCriteria(criteria);

		HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundSearchBuilder(query).build();
		System.out.println(compoundMetadataQuery);
		parsedQuery = new HpcCompoundSearchParser(compoundMetadataQuery).parse();
		System.out.println(query);
		System.out.println(parsedQuery);

		// Test2
		query = new HpcCompoundQuery();
		queries.put("A1", A1);
		queries.put("A2", A2);
		queries.put("A3", A3);
		query.setQueries(queries);
		criteria = "A1 AND A2 AND A3";
		query.setCriteria(criteria);
		
		compoundMetadataQuery = new HpcCompoundSearchBuilder(query).build();
		parsedQuery = new HpcCompoundSearchParser(compoundMetadataQuery).parse();
		System.out.println(query);
		System.out.println(parsedQuery);


		// Test3
		query = new HpcCompoundQuery();
		queries.put("A1", A1);
		queries.put("A2", A2);
		queries.put("A3", A3);
		queries.put("A4", A4);
		queries.put("A5", A5);
		query.setQueries(queries);
		criteria = "((A1 AND A2 AND A3) OR (A1 AND A4)) AND (A2 OR A5)";
		query.setCriteria(criteria);
		
		compoundMetadataQuery = new HpcCompoundSearchBuilder(query).build();
		parsedQuery = new HpcCompoundSearchParser(compoundMetadataQuery).parse();
		System.out.println(query);
		System.out.println(parsedQuery);
	}

}
