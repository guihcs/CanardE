package irit.complex.utils;

import irit.sparql.SparqlProxy;
import org.apache.jena.rdf.model.RDFNode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class CQAGenerator {

	private final String endpoint;
	private final String CQAFolder;
	private int count;
	private final double ratio;
	private final int maxCAV;

	public CQAGenerator(String endpoint, String CQAFolder){
		this.endpoint = endpoint;	
		this.CQAFolder = CQAFolder;
		count=0;
		ratio = 30;
		maxCAV = 20;
	}

	public void createCQAs() throws IOException {
		createClasses();
		createCAV();
		createProperties();
	}
	
	public void cleanCQARepository() {
		Path cqaPath = Paths.get(CQAFolder);
		try {
			//If the folder does not exist, create it
			if (Files.notExists(cqaPath)){
				Files.createDirectory(cqaPath);
			}
			//Else empty the folder
			else{
				File dir = new File(CQAFolder);
				for(File file: dir.listFiles()){
					file.delete(); 
				}
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void createClasses() throws IOException {
		String query = """
					PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
					SELECT distinct ?x WHERE{ \s
					?x a owl:Class.\s
					?y a ?x. filter(isIRI(?x))}""";

		List<Map<String, RDFNode>> result = SparqlProxy.query(endpoint, query);
		for (Map<String, RDFNode> jsonNode : result) {
			String owlClass = jsonNode.get("x").toString();
			if (interestingIRI(owlClass)) {
				//System.out.println(owlClass);
				//Create new file in designated folder with the new CQA
				PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
				String CQA = "SELECT DISTINCT ?x WHERE {  \n" +
						"?x a <" + owlClass + ">.} ";
				writer.append(CQA);
				writer.flush();
				writer.close();
				count++;
			}
		}
	}
	
	public void createProperties() throws IOException {
		String query = """
					PREFIX owl: <http://www.w3.org/2002/07/owl#> \s
					SELECT distinct ?x WHERE{ \s
					?y ?x ?z. {?x a owl:ObjectProperty.}
					  union{
					    ?x a owl:DatatypeProperty.}
					  }""";
		List<Map<String, RDFNode>> result = SparqlProxy.query(endpoint, query);
		for (Map<String, RDFNode> jsonNode : result) {
			String owlProp = jsonNode.get("x").toString();
			if (interestingIRI(owlProp)) {
				PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
				String CQA = "SELECT DISTINCT ?x ?y WHERE {  \n" +
						"?x <" + owlProp + "> ?y.} ";
				writer.append(CQA);
				writer.flush();
				writer.close();
				count++;
			}
		}
	}

	public void createCAV() throws IOException {
		//Get all "interesting" properties
		String query = """
					PREFIX owl: <http://www.w3.org/2002/07/owl#>  \s
					SELECT distinct ?x WHERE {  \s
					?x a owl:ObjectProperty.}""";
		List<Map<String, RDFNode>> result = SparqlProxy.query(endpoint, query);
		for (Map<String, RDFNode> node : result) {
			String property = node.get("x").toString();
			if (interestingIRI(property)) {
				String queryNb = "SELECT (count(distinct ?x) as ?sub) (count(distinct ?y) as ?ob) where {\n" +
						"?x <" + property + "> ?y.}";
				List<Map<String, RDFNode>> retNb = SparqlProxy.query(endpoint, queryNb);
				for (Map<String, RDFNode> nodeNb : retNb) {
					int nbSub = Integer.parseInt(nodeNb.get("sub").toString());
					int nbOb = Integer.parseInt(nodeNb.get("ob").toString());

					if (nbSub != 0 && nbOb != 0) {
						// If n_subj >> n_obj and n_obj < maxThreshold
						if ((double) nbSub / (double) nbOb > ratio && nbOb < maxCAV) {
							// get all the objects
							String queryOb = "SELECT distinct ?y where {\n" +
									"?x <" + property + "> ?y.}";
							List<Map<String, RDFNode>> retOb = SparqlProxy.query(endpoint, queryOb);
							// create n_obj CAV: ?x P oi
							for (Map<String, RDFNode> jsonNode : retOb) {
								String object = jsonNode.get("y").toString();
								PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
								String CQA = "SELECT DISTINCT ?x WHERE {\n" +
										"?x <" + property + "> <" + object + ">.} ";
								writer.append(CQA);
								writer.flush();
								writer.close();
								count++;
							}

						}

						// ELIF n_obj >> n_subj and n_subj < threshold
						else if ((double) nbSub / (double) nbOb > ratio && nbOb < maxCAV) {
							String querySub = "SELECT distinct ?x where {\n" +
									"?x <" + property + "> ?y.}";
							List<Map<String, RDFNode>> retSub = SparqlProxy.query(endpoint, querySub);
							for (Map<String, RDFNode> jsonNode : retSub) {
								String subject = jsonNode.get("x").toString();
								PrintWriter writer = new PrintWriter(CQAFolder + "/CQA" + count + ".sparql", StandardCharsets.UTF_8);
								String CQA = "SELECT DISTINCT ?x WHERE {\n" +
										"<" + subject + "> <" + property + "> ?x.} ";
								writer.append(CQA);
								writer.flush();
								writer.close();
								count++;
							}
						}
					}
				}
			}
		}

	}

	public boolean interestingIRI(String iri){
		return !(iri.contains("http://www.w3.org/2000/01/rdf-schema#") ||
				iri.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#") ||
				iri.contains("http://www.w3.org/2001/XMLSchema#") ||
				iri.contains("http://www.w3.org/2004/02/skos/core#") ||
				iri.contains("http://www.w3.org/2008/05/skos-xl#") ||
				iri.contains("http://www.w3.org/2002/07/owl#") ||
				iri.contains("http://xmlns.com/foaf/") ||
				iri.contains("http://purl.org/dc/terms/") ||
				iri.contains("http://purl.org/dc/elements/1.1/"));
	}
}
