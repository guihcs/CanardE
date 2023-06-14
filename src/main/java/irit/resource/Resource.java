package irit.resource;

import com.fasterxml.jackson.databind.JsonNode;
import irit.complex.answer.QueryTemplate;
import irit.dataset.DatasetManager;
import irit.sparql.SparqlProxy;
import irit.sparql.exceptions.IncompleteSubstitutionException;
import org.apache.jena.rdf.model.RDFNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {

    protected final String value;
    public final HashSet<IRI> similarIRIs;
    private final Pattern pattern = Pattern.compile("[a-z][/:#]");

    public Resource(String val) {
        Pattern inp = Pattern.compile("\\\\");
        value = inp.matcher(val).replaceAll("");
        similarIRIs = new HashSet<>();
    }

    public boolean isIRI() {
        Matcher matcher = pattern.matcher(value);
        return !value.contains(" ") && matcher.find();
    }

    QueryTemplate similar = new QueryTemplate("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX skos-xl: <http://www.w3.org/2008/05/skos-xl#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "\n" +
            "\n" +
            "SELECT DISTINCT ?x WHERE {\n" +
            "{?x ?z ?label.}\n" +
            "UNION {?x skos-xl:prefLabel ?z.\n" +
            "?z skos-xl:literalForm ?label.}\n" +
            "filter (regex(?label, \"^{{labelValue}}$\",\"i\")).\n" +
            "}");

    public String getSimilarQuery(String endpoint,Map<String, String> substitution){
        String query = "";
        try {
            query = similar.substitute(substitution);
        } catch (IncompleteSubstitutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return query;
    }

    public void findSimilarResource(String targetEndpoint) {
        /**Label search (heavy on large KBs)*/
        Map<String, String> substitution = new HashMap<>();
        substitution.put("labelValue", value);
        if (value.length()>1){
            substitution.put("LabelValue", value.substring(0, 1).toUpperCase() + value.substring(1));
        }
        else{
            substitution.put("LabelValue", "\""+value.toUpperCase()+"\"");
        }
        String query = getSimilarQuery(targetEndpoint,substitution);
        //System.out.println(query);

        List<Map<String, RDFNode>> ret = SparqlProxy.query(targetEndpoint, query);

        Iterator<Map<String, RDFNode>> retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").toString().replaceAll("\"", "");
            //System.out.println(s);
            similarIRIs.add(new IRI("<"+s+">"));
        }

        substitution.put("labelValue", "\""+value.substring(0, 1).toUpperCase() + value.substring(1)+"\"@en");
        query = getSimilarQuery(targetEndpoint,substitution);
        //System.out.println(query);

        ret = SparqlProxy.query(targetEndpoint, query);

        retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").toString().replaceAll("\"", "");
            //System.out.println(s);
            similarIRIs.add(new IRI("<"+s+">"));
        }

        substitution.put("labelValue", "\""+value.substring(0, 1).toUpperCase() + value.substring(1)+"\"");
        query = getSimilarQuery(targetEndpoint,substitution);
        //System.out.println(query);

        ret = SparqlProxy.query(targetEndpoint, query);

        retIterator = ret.iterator();
        while (retIterator.hasNext()) {
            String s = retIterator.next().get("x").toString().replaceAll("\"", "");
            //System.out.println(s);
            similarIRIs.add(new IRI("<"+s+">"));
        }

    }

    public String toValueString() {
        if (!(isIRI())) {
            return "\"" + value + "\"";
        } else {
            return toString();
        }
    }

    public String toString() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return value.equals(((Resource) obj).value);
        } else {
            return false;
        }
    }

    public HashSet<IRI> getSimilarIRIs() {
        return similarIRIs;
    }


    public String getValue() {
        return value;
    }
}
