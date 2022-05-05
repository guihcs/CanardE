package irit.complex.subgraphs;

import java.util.ArrayList;

public class PathSubgraph extends SubgraphForOutput {
	final ArrayList<Path> paths;
	
	public PathSubgraph(Path p) {
		paths= new ArrayList<>();
		paths.add(p);
		similarity = p.getSimilarity();
	}
	
	public double getAverageSimilarity() {
		return similarity;
	}
	
	public boolean addSubgraph(Path p) {
		boolean added = false;
		if(p.toSubGraphString().equals(paths.get(0).toSubGraphString())) {
			addSimilarity(p);
			paths.add(p);
			added =  true;
		}
		return added;	
	}
	
	public void addSimilarity(Path p){
		similarity = ((similarity*paths.size())+p.getSimilarity())/(paths.size()+1);
	}
	
	public String toExtensionString() {
		return paths.get(0).toSubGraphString();
	}
	
	public String toSPARQLForm() {		
		return "SELECT distinct ?answer0 ?answer1 WHERE {\n"+
		paths.get(0).toSubGraphString() +"}";
	}
	
	public String toIntensionString() {
		return paths.get(0).toSubGraphString();
	}
	
	public Path getMainPath() {
		return paths.get(0);
	}


}
