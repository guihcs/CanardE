package irit.sparql.query.select;

import irit.resource.IRI;
import irit.sparql.query.SparqlQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class SparqlSelect extends SparqlQuery {
    private String select;
    private final ArrayList<String> selectFocus;

    public SparqlSelect(String query) {
        super(query);
        mainQuery = mainQuery.trim().replaceAll("SELECT", "select").replaceAll("WHERE", "where").replaceAll("\n", " ");
        selectFocus = new ArrayList<>();
        Pattern pattern = Pattern.compile("""
				select[ \t
				distncDISTNC]+(\\?[A-Za-z\\d_-]+)[ \t
				]+(\\?*[A-Za-z\\d_-]*[ \t
				]*)where[ \t
				]*\\{(.+)}[ \t
				]*$""");
        Matcher matcher = pattern.matcher(mainQuery);
        while (matcher.find()) {
            selectFocus.add(matcher.group(1).trim());
            if (!matcher.group(2).trim().isEmpty()) {
                selectFocus.add(matcher.group(2).trim());
            }
            where = matcher.group(3).trim();
            setAggregate();
		}
        Pattern pattern2 = Pattern.compile("""
				select([ \t
				distncDISTNC]+\\?[A-Za-z\\d_-]+[ \t
				]+\\?*[A-Za-z\\d_-]*[ \t
				]*)where""");
        Matcher matcher2 = pattern2.matcher(mainQuery);
        if (matcher2.find()) {
            select = matcher2.group(1);
        }


    }

    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public void setAggregate() {
    }

    public String toString() {
        return mainQuery;
    }

    public String toSubgraphForm() {

        String ret = where;
        if (selectFocus.size() > 1) {
            int i = 0;
            for (String sf : selectFocus) {
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + " ", "\\?answer" + i + " ");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\.", "\\?answer" + i + ".");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "}", "\\?answer" + i + "}");
                ret = ret.replaceAll(sf.replaceAll("\\?", "\\\\?") + "\\)", "\\?answer" + i + ")");
                i++;
            }
        } else {
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + " ", "\\?answer ");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\.", "\\?answer.");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "}", "\\?answer}");
            ret = ret.replaceAll(selectFocus.get(0).replaceAll("\\?", "\\\\?") + "\\)", "\\?answer)");
        }
        return ret.replaceAll("\n", " ").replaceAll("\"", "\\\"");
    }

    public ArrayList<String> getSelectFocus() {
        return selectFocus;
    }

    public int getFocusLength() {
        return selectFocus.size();
    }



    public HashSet<String> getLabels(){
        HashSet<String> queryLabels = new HashSet<>();

        for (Map.Entry<String, IRI> iri : getIRIList().entrySet()){
            queryLabels.addAll(iri.getValue().getLabels());
        }
        return queryLabels;
    }
}
