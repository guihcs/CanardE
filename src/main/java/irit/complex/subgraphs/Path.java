package irit.complex.subgraphs;

import irit.dataset.DatasetManager;
import irit.labelmap.PathResult;
import irit.resource.IRI;
import irit.resource.Resource;
import irit.similarity.EmbeddingManager;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.*;

public class Path extends InstantiatedSubgraph {
    final ArrayList<Resource> entities;
    final ArrayList<IRI> types;
    List<Boolean> inverse;
    List<IRI> properties;
    double similarity;
    double typeSimilarity;


    public Path(Resource x, Resource y, String sparqlEndpoint, int maxLength) {
        properties = new ArrayList<>();
        entities = new ArrayList<>();
        types = new ArrayList<>();
        similarity = 0;
        inverse = new ArrayList<>();
        findPathMaxLength(x, y, sparqlEndpoint, maxLength, false);
    }

    public Path(Resource x, Resource y, String sparqlEndpoint, int maxLength, boolean biDirectional) {
        properties = new ArrayList<>();
        entities = new ArrayList<>();
        types = new ArrayList<>();
        similarity = 0;
        inverse = new ArrayList<>();
        findPathMaxLength(x, y, sparqlEndpoint, maxLength, biDirectional);
    }

    private void findPathMaxLength(Resource x, Resource y, String sparqlEndpoint, int length, boolean biDirectional) {

        String value1 = x.getValue();
        if (value1.startsWith("<") && value1.endsWith(">"))
            value1 = value1.substring(1, value1.length() - 1);

        String value2 = y.getValue();

        if (value2.startsWith("<") && value2.endsWith(">"))
            value2 = value2.substring(1, value2.length() - 1);


        PathResult result = DatasetManager.getInstance().labelMaps.get(sparqlEndpoint).pathBetween(value1, value2, length, biDirectional);
        if (result.getMap().isEmpty()) {
            return;
        }
        Map<String, String> next = result.getMap().getFirst();

        boolean stop = false;

        for (int i = 1; i < next.size() - 1 && !stop; i++) {
            if (!next.containsKey("p" + i)) {
                break;
            }
            String p = next.get("p" + i);
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
        }

        if (stop) {
            properties = new ArrayList<>();
        } else if (length >= 2) {

            for (int i = 1; i < next.size() + 1; i++) {
                if (!next.containsKey("v" + i)) {
                    break;
                }
                String v = next.get("v" + i);
                Resource res = new Resource(v);
                entities.add(res.isIRI() ? new IRI("<" + v + ">") : res);
            }


        }



        inverse = result.getInverseList();
    }


    public void compareLabel(Set<String> targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        similarity = 0;
        for (IRI prop : properties) {
            prop.retrieveLabels(targetEndpoint);
            similarity += EmbeddingManager.similarity(prop.getLabels(), targetLabels, threshold);
        }

        for (int i = 0; i < entities.size(); i++) {
            Resource ent = entities.get(i);
            if (ent instanceof IRI) {
                IRI type = types.get(i);
                if (type != null) {
                    double scoreType = EmbeddingManager.similarity(type.getLabels(), targetLabels, threshold);
                    if (scoreType > typeThreshold) {
                        typeSimilarity += scoreType;
                    } else {
                        types.set(i, null);
                    }
                }
            }
        }
        if (pathFound()) {
            similarity += 0.5;
        }

    }


    public void compareLabel(INDArray targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        similarity = 0;
        for (IRI prop : properties) {
            prop.retrieveLabels(targetEndpoint);
            similarity += EmbeddingManager.similarity(prop.getLabels(), targetLabels, threshold);
        }

        for (int i = 0; i < entities.size(); i++) {
            Resource ent = entities.get(i);
            if (ent instanceof IRI) {
                IRI type = types.get(i);
                if (type != null) {
                    double scoreType = EmbeddingManager.similarity(type.getLabels(), targetLabels, threshold);
                    if (scoreType > typeThreshold) {
                        typeSimilarity += scoreType;
                    } else {
                        types.set(i, null);
                    }
                }
            }
        }
        if (pathFound()) {
            similarity += 0.5;
        }

    }

    public void compareLabelEmb(INDArray targetLabels, double threshold, String targetEndpoint, double typeThreshold) {
        similarity = 0;

        List<String> propLabels = new ArrayList<>();

        for (IRI prop : properties) {
            prop.retrieveLabels(targetEndpoint);
            propLabels.addAll(prop.getLabels());
        }

        INDArray propEmb = EmbeddingManager.embLabels(propLabels);

        List<String> entLabels = new ArrayList<>();

        for (int i = 0; i < entities.size(); i++) {
            Resource ent = entities.get(i);
            if (ent instanceof IRI) {
                IRI type = types.get(i);
                if (type != null) {
                    entLabels.addAll(type.getLabels());
                    INDArray typeEmb = EmbeddingManager.embLabels(type.getLabels());
                    double scoreType = EmbeddingManager.similarity(typeEmb, targetLabels, threshold);
                    if (scoreType <= typeThreshold) {
                        types.set(i, null);
                    }
                }
            }
        }

        INDArray entEmb = EmbeddingManager.embLabels(entLabels);

        similarity = EmbeddingManager.similarity(propEmb.add(entEmb).div(2), targetLabels, threshold);

        if (pathFound()) {
            similarity += 0.5;
        }

    }




    public boolean pathFound() {
        return !properties.isEmpty();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            ret.append(entities.get(i)).append(" ").append(properties.get(i)).append(" ");
            if (i + 1 < entities.size()) {
                ret.append(entities.get(i + 1)).append(".  ");
            }
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
            if (inverse.size() > i && inverse.get(i)) {
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

    public void getMostSimilarTypes(String endpointUrl, Set<String> targetLabels, double threshold) {
        for (Resource r : entities) {
            if (r instanceof IRI iri) {
                IRI type = iri.findMostSimilarType(endpointUrl, targetLabels, threshold);
                types.add(type);
            } else {
                types.add(null);
            }
        }
    }

    public void getMostSimilarTypes(String endpointUrl, INDArray targetLabels, double threshold) {
        for (Resource r : entities) {
            if (r instanceof IRI iri) {
                IRI type = iri.findMostSimilarType(endpointUrl, targetLabels, threshold);
                types.add(type);
            } else {
                types.add(null);
            }
        }
    }

    public List<IRI> getProperties() {
        return properties;
    }

    public List<IRI> getTypes() {
        return types;
    }

    public List<Boolean> getInverse() {
        return inverse;
    }
}
