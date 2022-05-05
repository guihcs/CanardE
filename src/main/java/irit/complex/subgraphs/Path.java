package irit.complex.subgraphs;

import irit.resource.IRI;
import irit.resource.Resource;
import irit.similarity.EmbeddingManager;
import irit.sparql.SparqlProxy;
import org.apache.jena.rdf.model.RDFNode;

import java.util.*;

public class Path extends InstantiatedSubgraph {
    ArrayList<IRI> properties;
    final ArrayList<Resource> entities;
    final ArrayList<IRI> types;
    double similarity;
    double typeSimilarity;
    final ArrayList<Boolean> inverse;

    public Path(Resource x, Resource y, String sparqlEndpoint, int length, ArrayList<Boolean> inverse) {
        properties = new ArrayList<>();
        entities = new ArrayList<>();
        types = new ArrayList<>();
        similarity = 0;
        this.inverse = inverse;


        findPathWithLength(x, y, sparqlEndpoint, length);
    }

    private void findPathWithLength(Resource x, Resource y, String sparqlEndpoint, int length) {


        String query;
        StringBuilder queryBody = new StringBuilder();
        ArrayList<String> variables = new ArrayList<>();

        if (!x.isIRI()) {
            variables.add("?x");
        } else {
            variables.add(x.toString());
        }


        for (int i = 1; i <= length - 1; i++) {
            variables.add("?v" + i);
        }

        if (!y.isIRI()) {
            variables.add("?y");
        } else {
            variables.add(y.toString());
        }

        for (int i = 1; i <= length; i++) {
            if (inverse.get(i - 1)) {
                queryBody.append(variables.get(i)).append(" ?p").append(i).append(" ").append(variables.get(i - 1)).append(". \n");
            } else {
                queryBody.append(variables.get(i - 1)).append(" ?p").append(i).append(" ").append(variables.get(i)).append(". \n");
            }

        }

        if (!x.isIRI()) {
            queryBody.append("   filter (regex(?x, \"^").append(x).append("$\",\"i\"))\n");
        }

        if (!y.isIRI()) {
            queryBody.append("   filter (regex(?y, \"^").append(y).append("$\",\"i\"))\n");
        }

        query = "SELECT DISTINCT * WHERE { " + queryBody + " }  LIMIT 1";


        List<Map<String, RDFNode>> result = SparqlProxy.query(sparqlEndpoint, query);
        Iterator<Map<String, RDFNode>> retIteratorTarg = result.iterator();

        if (retIteratorTarg.hasNext()) {
            Map<String, RDFNode> next = retIteratorTarg.next();
            if (next.containsKey("x")) {
                entities.add(new Resource(next.get("x").toString()));
            } else {
                entities.add(x);
            }
            int i = 1;
            boolean stop = false;
            while (i <= length && !stop) {
                String p = next.get("p" + i).toString();
                Resource res = new Resource(p);
                switch (p) {
                    case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                            "http://www.w3.org/2002/07/owl#sameAs",
                            "http://www.w3.org/2004/02/skos/core#exactMatch",
                            "http://www.w3.org/2004/02/skos/core#closeMatch",
                            "http://dbpedia.org/ontology/wikiPageWikiLink" -> stop = true;
                }

                if (res.isIRI()) {
                    properties.add(new IRI("<" + p + ">"));
                }
                i++;
            }
            if (stop) {
                properties = new ArrayList<>();
            }
            if (length >= 2 && !stop) {
                for (int j = 1; j <= length - 1; j++) {
                    String v = next.get("v" + j).toString();
                    Resource res = new Resource(v);
                    if (res.isIRI()) {
                        entities.add(new IRI("<" + v + ">"));
                    } else {
                        entities.add(res);
                    }
                }
            }
            if (next.containsKey("y")) {
                entities.add(new Resource(next.get("y").toString()));
            } else {
                entities.add(y);
            }

        }


    }

    public void compareLabel(HashSet<String> targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        similarity = 0;
        for (IRI prop : properties) {
            prop.retrieveLabels(targetEndpoint);
            similarity += similarity(prop.getLabels(), targetLabels, threshold);
        }

        for (int i = 0; i < entities.size(); i++) {
            Resource ent = entities.get(i);
            if (ent instanceof IRI) {
                IRI type = types.get(i);
                if (type != null) {
                    double scoreType = similarity(type.getLabels(), targetLabels, threshold);
                    if (scoreType > typeThreshold) {
                        typeSimilarity += scoreType;
                    } else {
                        types.set(i, null);
                    }
                }
            }
        }
        if (pathFound()) {
            similarity += 0.5;//if path
        }

        getSimilarity();
    }

    public double similarity(Set<String> labels1, HashSet<String> labels2, double threshold){
        double score = 0;
        for(String l1 : labels1){
            for(String l2: labels2){
                double sim = EmbeddingManager.getSim(l1, l2);
                sim = sim < threshold ? 0 : sim;
                score += sim;
            }
        }
        return score;
    }

    public boolean pathFound() {
        return !properties.isEmpty();
    }

    //version all entities
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            ret.append(entities.get(i)).append(" ").append(properties.get(i)).append(" ").append(entities.get(i + 1)).append(".  ");
        }
        return getSimilarity() + " <-> " + ret;
    }

    public String toSubGraphString() {
        StringBuilder ret = new StringBuilder();
        ArrayList<String> variables = new ArrayList<>();
        variables.add("?answer0");
        for (int i = 1; i <= properties.size() - 1; i++) {
            variables.add("?v" + i);
        }
        variables.add("?answer1");

        for (int i = 0; i < properties.size(); i++) {
            String xStr = variables.get(i);
            String yStr = variables.get(i + 1);

            if (types.get(i) != null) {
                ret.append(xStr).append(" a ").append(types.get(i)).append(".  ");
            }
            if (inverse.get(i)) {
                ret.append(yStr).append(" ").append(properties.get(i)).append(" ").append(xStr).append(".  ");
            } else {
                ret.append(xStr).append(" ").append(properties.get(i)).append(" ").append(yStr).append(".  ");
            }
        }
        if (types.get(properties.size()) != null) {
            ret.append(variables.get(properties.size())).append(" a ").append(types.get(properties.size())).append(".  ");
        }
        return ret.toString();
    }

    public double getSimilarity() {
        return similarity + typeSimilarity;
    }

    public void getMostSimilarTypes(String endpointUrl, HashSet<String> targetLabels, double threshold) {
        for (Resource r : entities) {
            if (r instanceof IRI) {
                IRI type = ((IRI) r).findMostSimilarType(endpointUrl, targetLabels, threshold);
                types.add(type);
            } else {
                types.add(null);
            }
        }
    }

    public ArrayList<IRI> getProperties() {
        return properties;
    }

    public ArrayList<IRI> getTypes() {
        return types;
    }

    public ArrayList<Boolean> getInverse() {
        return inverse;
    }
}
