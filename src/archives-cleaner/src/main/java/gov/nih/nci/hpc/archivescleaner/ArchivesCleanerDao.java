package gov.nih.nci.hpc.archivescleaner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.swing.text.html.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArchivesCleanerDao {

  private static final String SQL_TEMPLATE_PARAM_$TARGETED_DOCS = "${targeted-docs}";


  private static final String SQL_SELECT_TEMPLATE_COLLECTIONS =
"SELECT\n"
+ "  substring(t1.coll_name from strpos(t1.coll_name, t2.pathname)) dme_path,\n"
+ "  t1.coll_id,\n"
+ "  substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)) coll_simple_name,\n"
+ "  t1.coll_name irods_path\n"
+ "FROM r_coll_main t1,\n"
+ "  ( SELECT \"BASE_PATH\" pathname\n"
+ "    FROM \"HPC_DATA_MANAGEMENT_CONFIGURATION\"\n"
+ "    WHERE \"DOC\" IN (" + SQL_TEMPLATE_PARAM_$TARGETED_DOCS + ")) t2\n"
+ "WHERE strpos(t1.coll_name, '/trash/home/') = 0\n"
+ "  AND strpos(t1.coll_name, t2.pathname) > 0\n"
+ "  AND strpos(substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname) + 1)), '/') = 0\n"
+ "  AND substring(t1.coll_name from (strpos(t1.coll_name, t2.pathname) + char_length(t2.pathname)) for 1) = '/'\n"
+ "ORDER BY 1";


  private static final String SQL_SELECT_TEMPLATE_DATA_OBJECTS =
"SELECT (aux1.dme_coll_path || '/' || t1.data_name) dme_path,\n"
+ "       t1.data_id, \n"
+ "       t1.data_name,\n"
+ "\t   (aux1.coll_name || '/' || t1.data_name) irods_path\n"
+ "FROM r_data_main t1,\n"
+ "     (SELECT s1.coll_id,\n"
+ "\t         s1.coll_name,\n"
+ "\t         (substring(coll_name from strpos(s1.coll_name, s2.bpath))) dme_coll_path\n"
+ "      FROM r_coll_main s1,\n"
+ "           (SELECT \"BASE_PATH\" bpath\n"
+ "            FROM \"HPC_DATA_MANAGEMENT_CONFIGURATION\"\n"
+ "            WHERE \"DOC\" IN (" + SQL_TEMPLATE_PARAM_$TARGETED_DOCS + ")) s2\n"
+ "      WHERE s1.coll_name LIKE ('%' || s2.bpath)\n"
+ "        AND strpos(s1.coll_name, '/trash/home/') = 0) aux1\n"
+ "WHERE t1.coll_id = aux1.coll_id\n"
+ "ORDER BY 1\n";


  private final JdbcTemplate jdbcTemplate;

  private List<String> defaultTargetedDocs;


  @Value("${targeted.docs.default}")
  private String defaultTargetedDocsConfigVal;

  private String defaultTargetedDocsAsSqlInOperand;

  private Optional<String> appTargetedDocsAsSqlInOperand = Optional.empty();
  private Optional<List<String>> targetedDocs = Optional.empty();


  @Autowired
  public ArchivesCleanerDao(JdbcTemplate pJdbcTemplate) {
    this.jdbcTemplate = pJdbcTemplate;
  }


  public List<String> runQueryForCollections() {
    final List<String> dmeCollections = this.jdbcTemplate.query(
      getParametrizedSqlSelectForCollections(),
      (resultSet, i) -> { return resultSet.getString(1); });
    return dmeCollections;
  }


  public List<String> runQueryForDataObjects() {
    final List<String> dmeCollections = this.jdbcTemplate.query(
      getParametrizedSqlSelectForDataObjects(),
      (resultSet, i) -> { return resultSet.getString(1); });
    return dmeCollections;
  }


  public void runSimpleCountQuery() {
    final String selectQuery = "SELECT COUNT(*) FROM \"HPC_DATA_MANAGEMENT_CONFIGURATION\"";
    Integer count = this.jdbcTemplate.queryForObject(selectQuery, java.lang.Integer.class, null);
    System.out.println("Executing the following query:");
    System.out.println("[");
    System.out.println("     " + selectQuery);
    System.out.println("]");
    System.out.println("**************************************************");
    System.out.println("Result of query: ");
    System.out.println("[");
    System.out.println("     " + count);
    System.out.println("]");
  }


  public String getDefaultTargetedDocsAsSqlInOperand() {
    if (null == this.defaultTargetedDocsAsSqlInOperand) {
      applyTargetedDocsState();
    }
    return this.defaultTargetedDocsAsSqlInOperand;
  }


  public List<String> getDefaultTargetedDocs() {
    if (null == this.defaultTargetedDocs) {
      applyTargetedDocsState();
    }
    return this.defaultTargetedDocs;
  }


  public Optional<List<String>> getTargetedDocs() {
    return targetedDocs;
  }


  public void setTargetedDocs(Optional<List<String>> pDocs) {
    this.targetedDocs = pDocs;

    if (pDocs.isPresent()) {
      StringBuilder sb = new StringBuilder();
      pDocs.get().forEach(someDoc -> {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append("'").append(someDoc).append("'");
      });
      this.appTargetedDocsAsSqlInOperand = Optional.of(sb.toString());
    } else {
      this.appTargetedDocsAsSqlInOperand = Optional.empty();
    }
  }


  private void applyTargetedDocsState() {
    final String[] docs = this.defaultTargetedDocsConfigVal.split("\\s+");
    final StringBuilder sb = new StringBuilder();
    for (String someDoc : docs) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("'").append(someDoc).append("'");
    }
    this.defaultTargetedDocsAsSqlInOperand = sb.toString();
    this.defaultTargetedDocs = Arrays.asList(docs);
  }


  private String getApplicableDocsAsSqlInOperand() {
    return this.appTargetedDocsAsSqlInOperand.orElse(
      getDefaultTargetedDocsAsSqlInOperand());
  }


  private String getParametrizedSqlSelectForCollections() {
    return SQL_SELECT_TEMPLATE_COLLECTIONS.replace(
      SQL_TEMPLATE_PARAM_$TARGETED_DOCS, getApplicableDocsAsSqlInOperand());
  }


  private String getParametrizedSqlSelectForDataObjects() {
    return SQL_SELECT_TEMPLATE_DATA_OBJECTS.replace(
      SQL_TEMPLATE_PARAM_$TARGETED_DOCS, getApplicableDocsAsSqlInOperand());
  }

}
