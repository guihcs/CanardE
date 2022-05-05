package irit.labelmap;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;

import java.util.*;
import java.util.stream.Collectors;

public class LabelMap {

    private final Map<String, Map<String, Set<String>>> spm = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> spmi = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> pom = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> som = new HashMap<>();
    private final Map<String, String> typeMap = new HashMap<>();


    public LabelMap(String path) {
        load(path);
    }

    public void load(String path) {
        Model defaultModel = RDFDataMgr.loadModel(path);


        StmtIterator stmtIterator = defaultModel.listStatements();

        while (stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            String s = statement.getSubject().toString();
            String p = statement.getPredicate().toString();
            String o = statement.getObject().toString();

            typeMap.put(s, getType(statement.getSubject()));
            typeMap.put(p, getType(statement.getPredicate()));
            typeMap.put(o, getType(statement.getObject()));


            String si = s.toLowerCase();
            String pi = p.toLowerCase();
            String oi = o.toLowerCase();


            if (!spm.containsKey(o)) spm.put(o, new HashMap<>());
            if (!spm.get(o).containsKey(p)) spm.get(o).put(p, new HashSet<>());

            spm.get(o).get(p).add(s);

            if (!pom.containsKey(s)) pom.put(s, new HashMap<>());
            if (!pom.get(s).containsKey(p)) pom.get(s).put(p, new HashSet<>());

            pom.get(s).get(p).add(o);


            if (!spmi.containsKey(oi)) spmi.put(oi, new HashMap<>());
            if (!spmi.get(oi).containsKey(pi)) spmi.get(oi).put(pi, new HashSet<>());

            spmi.get(oi).get(pi).add(si);

            if (!som.containsKey(p)) spm.put(p, new HashMap<>());
            if (!spm.get(p).containsKey(s)) spm.get(p).put(s, new HashSet<>());

            spm.get(p).get(s).add(o);

        }


    }

    public Set<String> getSimilar(String v) {

        Set<String> result = new HashSet<>();

        if (spmi.containsKey(v)) {
            Map<String, Set<String>> stringSetMap = spmi.get(v);

            for (Set<String> value : stringSetMap.values()) {
                result.addAll(value);
            }


            if (stringSetMap.containsKey("http://www.w3.org/2008/05/skos-xl#literalForm")) {
                Set<String> strings = stringSetMap.get("http://www.w3.org/2008/05/skos-xl#literalForm");
                for (String string : strings) {
                    Map<String, Set<String>> stringSetMap1 = spmi.get(string);
                    if (stringSetMap1.containsKey("http://www.w3.org/2008/05/skos-xl#prefLabel")) {
                        Set<String> strings1 = stringSetMap1.get("http://www.w3.org/2008/05/skos-xl#prefLabel");
                        result.addAll(strings1);
                    }
                }
            }
        }

        return result;
    }


    public Set<String> getMatched(String v) {

        Set<String> result = new HashSet<>();

        result.addAll(pom.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2000/01/rdf-schema#seeAlso", Set.of()));
        result.addAll(pom.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2002/07/owl#sameAs", Set.of()));
        result.addAll(pom.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2004/02/skos/core#closeMatch", Set.of()));
        result.addAll(pom.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2004/02/skos/core#exactMacth", Set.of()));
        result.addAll(spm.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2000/01/rdf-schema#seeAlso", Set.of()));
        result.addAll(spm.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2002/07/owl#sameAs", Set.of()));
        result.addAll(spm.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2004/02/skos/core#closeMatch", Set.of()));
        result.addAll(spm.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2004/02/skos/core#exactMacth", Set.of()));

        return result;
    }


    public boolean exists(String v) {
        Set<String> excludedProperties = Set.of(
                "http://www.w3.org/2002/07/owl#sameAs",
                "http://www.w3.org/2004/02/skos/core#closeMatch",
                "http://www.w3.org/2004/02/skos/core#exactMatch"
        );
        int sc = 0;

        for (String s : pom.getOrDefault(v, Map.of()).keySet()) {
            if (!excludedProperties.contains(s)) sc++;
        }

        int oc = 0;
        for (String s : spm.getOrDefault(v, Map.of()).keySet()) {
            if (!excludedProperties.contains(s)) oc++;
        }
        return sc + oc > 0;
    }


    public Set<String> labels(String v) {
        Set<String> result = new HashSet<>();
        for (Set<String> value : pom.getOrDefault(v, Map.of()).values()) {
            result.addAll(value);
        }

        Set<String> orDefault = pom.getOrDefault(v, Map.of()).getOrDefault("http://www.w3.org/2008/05/skos-xl#prefLabel", Set.of());
        for (String s : orDefault) {
            Set<String> orDefault1 = pom.getOrDefault(s, Map.of()).getOrDefault("http://www.w3.org/2008/05/skos-xl#literalForm", Set.of());
            result.addAll(orDefault1);
        }

        return result.stream().filter(s -> typeMap.get(s).equals("Literal")).collect(Collectors.toSet());
    }


    public String getType(RDFNode r){
        if (r.isLiteral()) return "Literal";
        if (r.isAnon()) return "Anon";
        if (r.isResource()) return "Resource";
        if (r.isURIResource()) return "URIResource";
        return "None";
    }


    public Set<String> types(String value) {
//        SELECT DISTINCT ?type WHERE {" +
//            value + " a ?type."
//                    + "filter(isIRI(?type))}

        Set<String> orDefault = pom.getOrDefault(value, Map.of()).getOrDefault("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", Set.of());

        return orDefault.stream().filter(s -> typeMap.get(s).equals("URIResource") || typeMap.get(s).equals("Resource")).collect(Collectors.toSet());
    }


}
