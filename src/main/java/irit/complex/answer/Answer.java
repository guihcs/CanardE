package irit.complex.answer;

import irit.complex.subgraphs.InstantiatedSubgraph;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import irit.sparql.query.select.SparqlSelect;

import java.util.HashSet;

public abstract class Answer {
	final HashSet<String> goodTriples ;
	
	public Answer(){
		goodTriples = new HashSet<>();
		
	}
	
	public void getSimilarIRIs(String targetEndpoint) {}
	
	public void getExistingMatches(String sourceEndpoint, String targetEndpoint) {}
	
	public void retrieveIRILabels(String endpointURL) {}
	
	public HashSet<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, String targetEndpoint, double similarityThreshold) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
		return new HashSet<>();
	}

	public boolean hasMatch(){ 
		return false;
	}
	
	public String printMatchedEquivalents() {
		return "";
	}

}
