package irit.complex.subgraphs;

public class InstantiatedSubgraph implements Comparable<InstantiatedSubgraph>{
	double similarity;
	
	public double getSimilarity() {
		return similarity;
	}

	@Override
	public int compareTo(InstantiatedSubgraph s) {
		
		return Double.compare(s.getSimilarity(), getSimilarity());
	}

}

	
