package irit.output;

import irit.complex.subgraphs.SubgraphForOutput;
import irit.sparql.query.select.SparqlSelect;

import java.util.List;

public abstract class Output {
	
	protected final String sourceEndpoint;
	protected final String targetEndpoint;
	
	public Output(String sourceEndpoint, String targetEndpoint){
		this.sourceEndpoint = sourceEndpoint;
		this.targetEndpoint = targetEndpoint;
		
	}
	
	public void init(){
	}
	
	public void addToOutput(List<SubgraphForOutput> output, SparqlSelect sq){
	}
	
	public void end() {}

}
