package irit.resource;

import irit.complex.subgraphs.Triple;
import irit.dataset.DatasetManager;
import irit.similarity.EmbeddingManager;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class IRI extends Resource {
    private final Set<String> labels;
    private final Set<Triple> triples;
    private final Set<IRI> types;
    private boolean labelsGot;
    private boolean triplesRetrieved;
    private final Pattern pattern = Pattern.compile("[+{}.?*^]");

    public IRI(String iri) {
        super(iri);
        labels = ConcurrentHashMap.newKeySet();
        triples = ConcurrentHashMap.newKeySet();
        types = ConcurrentHashMap.newKeySet();
        labelsGot = false;
        triplesRetrieved = false;
    }

    public void retrieveLabels(String endpointUrl) {
        if (!labelsGot) {
            addLabel(value.replaceAll("[<>]", ""));


            String substring = value.substring(1);
            substring = substring.substring(0, substring.length()-1);
            Set<String> labels = DatasetManager.getInstance().labelMaps.get(endpointUrl).labels(substring);


            for (String s : labels) {
                Resource res = new Resource(s);
                if (!res.isIRI()) {
                    addLabel(s);
                }

            }
            labelsGot = true;
        }

    }

    public void retrieveTypes(String endpointUrl) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {

        Set<String> lmTypes = DatasetManager.getInstance().labelMaps.get(endpointUrl).types(value.replaceAll("[<>]", ""));

        for (String s : lmTypes) {
            types.add(new IRI("<" + s + ">"));
        }
        for (IRI type : types) {
            type.retrieveLabels(endpointUrl);
        }
    }

    public IRI findMostSimilarType(String endpointUrl, HashSet<String> targetLabels, double threshold) {
        if (getTypes().isEmpty()) {
            try {
                retrieveTypes(endpointUrl);
            } catch (SparqlQueryMalFormedException | SparqlEndpointUnreachableException e) {
                e.printStackTrace();
            }
        }
        double scoreTypeMax = -1;
        IRI finalType = null;
        for (IRI type : getTypes()) {
            double scoreType;
            type.retrieveLabels(endpointUrl);
            scoreType = similarity(type.getLabels(), targetLabels, threshold);
            if (scoreTypeMax < scoreType) {
                scoreTypeMax = scoreType;
                finalType = type;
            }
        }
        return finalType;
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

    public void addLabel(String label) {
        labels.add(label.trim());
    }


    public void findExistingMatches(final String sourceEndpoint, final String targetEndpoint) {

        ArrayList<IRI> allMatches = new ArrayList<>();

        allMatches.add(this);

        String s1 = value.replaceAll("\\$", "");
        Set<String> matched1 = DatasetManager.getInstance().labelMaps.get(sourceEndpoint).getMatched(s1);
        Set<String> matched2 = DatasetManager.getInstance().labelMaps.get(targetEndpoint).getMatched(s1);

        for (String s : matched1) {
            Resource res = new Resource(s);
            if (res.isIRI()) {
                allMatches.add(new IRI("<" + s + ">"));
            }
        }

        for (String s : matched2) {
            Resource res = new Resource(s);
            if (res.isIRI()) {
                allMatches.add(new IRI("<" + s + ">"));
            }
        }

        /*Check if a match is in the target dataset*/
        for (IRI match : allMatches) {
            if ( DatasetManager.getInstance().labelMaps.get(targetEndpoint).exists(match.toString())) {
                similarIRIs.add(match);
            }
        }

    }

    public void findSimilarResource(String targetEndpoint) {
        if (labels.isEmpty()) {
            retrieveLabels(targetEndpoint);
        }
        Set<String> nml = new HashSet<>();

        for (String rawLab : labels) {

            String label = pattern.matcher(rawLab).replaceAll("");
            Set<String> similar = DatasetManager.getInstance().labelMaps.get(targetEndpoint).getSimilar(label.toLowerCase());
            nml.addAll(similar);

        }

        for (String s : nml) {
            similarIRIs.add(new IRI("<" + s + ">"));
        }

    }


    public Set<String> getLabels() {
        return labels;
    }

    public Set<Triple> getTriples() {
        return triples;
    }

    public Set<IRI> getTypes() {
        return types;
    }

    public String toStrippedString() {
        return value.replaceAll("<", "").replaceAll(">", "");
    }


    public boolean isTriplesRetrieved() {
        return triplesRetrieved;
    }

    public void setTriplesRetrieved(boolean triplesRetrieved) {
        this.triplesRetrieved = triplesRetrieved;
    }


}
