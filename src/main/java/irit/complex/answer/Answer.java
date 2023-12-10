package irit.complex.answer;

import irit.complex.subgraphs.InstantiatedSubgraph;
import irit.sparql.query.select.SparqlSelect;

import java.util.HashSet;
import java.util.Set;

public abstract class Answer {

    public Answer() {

    }

    public void getSimilarIRIs(String targetEndpoint) throws Exception {
    }

    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) {
    }

    public void retrieveIRILabels(String endpointURL) {
    }

    public Set<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, String targetEndpoint, double similarityThreshold) {
        return new HashSet<>();
    }

    public boolean hasMatch() {
        return false;
    }


}
