package irit.output;

import fr.inrialpes.exmo.align.impl.edoal.*;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;
import fr.inrialpes.exmo.ontowrap.BasicOntology;
import irit.complex.subgraphs.Path;
import irit.complex.subgraphs.PathSubgraph;
import irit.complex.subgraphs.SubgraphForOutput;
import irit.complex.subgraphs.TripleSubgraph;
import irit.complex.utils.SPARQLNode;
import irit.output.Output;
import irit.sparql.query.select.SparqlSelect;
import org.semanticweb.owl.align.AlignmentException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EDOALOutput extends Output {
	private EDOALAlignment alignment;
	private final String outputEDOALfile;

	public EDOALOutput(String source, String target, String file){
		super(source, target);
		outputEDOALfile = file;
	}

	/***EDOAL ALIGNMENTS**/
	public void init(){
		alignment = new EDOALAlignment();
		BasicOntology o1= new BasicOntology();
		try {
			o1.setURI(new URI(sourceEndpoint));
			o1.setFormalism("owl");
			o1.setFormURI(new URI("http://www.w3.org/TR/owl-guide/"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		BasicOntology o2= new BasicOntology();
		try {
			o2.setURI(new URI(targetEndpoint));
			o2.setFormalism("owl");
			o2.setFormURI(new URI("http://www.w3.org/TR/owl-guide/"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		try {
			alignment.init(o1,o2);
		} catch (AlignmentException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addToOutput(List<SubgraphForOutput> output, SparqlSelect sq){
		//Source sub-graph
		Expression sourceExpr = subgraphFormToEDOALEntity(sq.toSubgraphForm(),sq.getSelectFocus());

		//2-transform source and target query subgraphs into edoal entities
		for (SubgraphForOutput s: output){
			if (s.getSimilarity() >= 0){
				//System.out.println(s.toIntensionString());
				Expression targetExpr = null;
				double score = s.getSimilarity();
				if(score>1.0){
					score=1.0;
				}
				if(s instanceof TripleSubgraph) {
					// If common part is the subject or object, always express the extension
					if (s.toIntensionString().contains("somePredicate")){ 
						targetExpr= subgraphFormToEDOALEntity(s.toExtensionString(),sq.getSelectFocus());
					}
					// If common part is the predicate 
					else if (s.toIntensionString().contains("someObject")||s.toIntensionString().contains("someSubject")){
						// and if the predicate similarity is higher than the object/subject similarity --> Intension
						if(((TripleSubgraph)s).predicateHasMaxSim()) {
							targetExpr= subgraphFormToEDOALEntity(s.toIntensionString(),sq.getSelectFocus());
						}					
						// else --> extension
						else {
							targetExpr= subgraphFormToEDOALEntity(s.toExtensionString(),sq.getSelectFocus());
						}		

					}
					else {
						targetExpr= subgraphFormToEDOALEntity(s.toExtensionString(),sq.getSelectFocus());
					}
				}

				// same for PathSubgraph
				else if (s instanceof PathSubgraph) {
					targetExpr= subgraphFormToEDOALProperty(s);
				}



				try {
					//System.out.println(sourceExpr+"  "+targetExpr);
					if(sourceExpr != null & targetExpr !=null) {
						alignment.addAlignCell(sourceExpr, targetExpr,"Equivalence", score);
					}					
				} catch (AlignmentException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void end(){
		try {
			Files.createDirectories(Paths.get(outputEDOALfile).getParent());
			PrintWriter writer = new PrintWriter(outputEDOALfile, StandardCharsets.UTF_8);
			RDFRendererVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.flush();
			writer.close();
		} catch (AlignmentException | IOException e) {
			e.printStackTrace();
		}
	}

	public Expression subgraphFormToEDOALEntity(String s, ArrayList<String> focus){
		Expression expr = null;
		ArrayList<String> subgraphs = setOfUNIONSubgraphs(s);
		ArrayList<String> minusSubgraphs = setOfMINUSSubgraphs(s);
		//CLASS expressions
		if (focus.size()==1){	
			//System.out.println("it's a class");
			ClassExpression theclassExpr=null;
			ClassExpression classExprM = null;
			ArrayList<ClassExpression> classExpr = new ArrayList<>();
			for(String sub: subgraphs) {
				classExpr.add(subgraphFormToEDOALClass(sub, "?answer"));
			}
			if(classExpr.size()==1) {
				theclassExpr=classExpr.get(0);
			}
			else {
				theclassExpr = new ClassConstruction(Constructor.OR,classExpr);
			}

			ArrayList<ClassExpression> classExprMSet = new ArrayList<>();
			for(String mSub : minusSubgraphs) {
				classExprMSet.add(subgraphFormToEDOALClass(mSub, "?answer"));				
			}
			if(classExprMSet.size()==1) {
				classExprM=classExprMSet.get(0);
			}
			else {
				classExprM = new ClassConstruction(Constructor.OR,classExprMSet);
			}

			if(classExprMSet.size()>0) {
				ArrayList<ClassExpression> classExprMNotfinal = new ArrayList<>();
				classExprMNotfinal.add(classExprM);
				ClassExpression minusExpr = new ClassConstruction(Constructor.NOT,classExprMNotfinal);
				ArrayList<ClassExpression> classExprFinal = new ArrayList<>();
				classExprFinal.add(theclassExpr);
				classExprFinal.add(minusExpr);
				theclassExpr = new ClassConstruction(Constructor.AND, classExprFinal);				
			}

			expr=theclassExpr;

		}
		//PROPERTY expression
		else if (focus.size()==2){
			RelationExpression theRelExpr = null;
			RelationExpression relM =null;
			ArrayList<RelationExpression> relExprSet = new ArrayList<>();
			for(String sub: subgraphs) {
				relExprSet.add(subgraphFormToEDOALProperty(sub, "?answer0", "?answer1"));
			}
			if(relExprSet.size()==1) {
				theRelExpr=relExprSet.get(0);
			}
			else {
				theRelExpr = new RelationConstruction(Constructor.OR,relExprSet);
			}
			ArrayList<RelationExpression> relExprMSet = new ArrayList<>();
			for(String mSub : minusSubgraphs) {
				relExprMSet.add(subgraphFormToEDOALProperty(mSub, "?answer0", "?answer1"));				
			}
			if(relExprMSet.size()==1) {
				relM=relExprMSet.get(0);
			}
			else {
				relM = new RelationConstruction(Constructor.OR,relExprMSet);
			}

			if(relExprMSet.size()>0) {
				ArrayList<RelationExpression> relExprMNotfinal = new ArrayList<>();
				relExprMNotfinal.add(relM);
				RelationExpression minusExpr = new RelationConstruction(Constructor.NOT,relExprMNotfinal);
				ArrayList<RelationExpression> relExprFinalSet = new ArrayList<>();
				relExprFinalSet.add(theRelExpr);
				relExprFinalSet.add(minusExpr);
				theRelExpr = new RelationConstruction(Constructor.AND, relExprFinalSet);				
			}

			expr=theRelExpr;
		}
		else{
			System.err.println("Trying to find a class expression with more than 1 variable as select");
		}
		//

		return expr;		
	}

	public ArrayList<String> setOfUNIONSubgraphs(String s) {
		s=s.replaceAll("[\n\t ]+", " ");
		s=s.replaceAll("[\n\t ]+\\.", "\\.");
		s=s.replaceAll("\\\\*\\{", "\\\\\\{");
		s=s.replaceAll("\\\\*}", "\\\\\\}");
		s=s.replaceAll("minus", "MINUS");
		s=s.replaceAll("MINUS *\\\\\\{([^\\\\}]+)\\\\}","");
		s=s.replaceAll("union", "UNION");
		ArrayList<String> res = new ArrayList<>();
		Pattern pattern1 = Pattern.compile("\\\\\\{([^\\\\}]+)\\\\} *UNION");
		Matcher matcher1 = pattern1.matcher(s);
		if (matcher1.find()){
			res.add(matcher1.group(1).trim());
			s=s.replaceFirst("\\\\\\{"+matcher1.group(1).replaceAll("\\?","\\\\\\?")+"\\\\}", "");
			Pattern pattern2 = Pattern.compile("UNION *\\\\\\{([^\\\\}]+)\\\\}");
			Matcher matcher2 = pattern2.matcher(s);
			while (matcher2.find()){
				res.add(matcher2.group(1));
				//System.out.println(matcher2.group());
				String mgroup = matcher2.group();
				mgroup=mgroup.replaceAll("\\\\*\\{", "\\\\\\{");
				mgroup=mgroup.replaceAll("\\\\*}", "\\\\\\}");
				mgroup = mgroup.replaceAll("\\?","\\\\\\?");
				mgroup=mgroup.replaceAll("\\.", "\\\\\\.");
				
				s=s.replaceAll("\\\\*\\{", "\\{");
				s=s.replaceAll("\\\\*}", "\\}");
				s=s.replaceAll(mgroup, "");
			}
			if(!s.trim().equals("")) {
				//System.out.println(s);
				for (int i =0; i< res.size();i++) {
					res.set(i, res.get(i)+s);
					//System.out.println(i+" "+res.get(i));
				}
			}
		}
		else {
			res.add(s);
		}
		return res;
	}

	public ArrayList<String> setOfMINUSSubgraphs(String s) {
		s=s.replaceAll("[\n\t ]+", " ");
		s=s.replaceAll("[\n\t ]+\\.", "\\.");
		s=s.replaceAll("\\\\*\\{", "\\\\\\{");
		s=s.replaceAll("\\\\*}", "\\\\\\}");
		s=s.replaceAll("minus", "MINUS");
		ArrayList<String> res = new ArrayList<>();
		Pattern pattern1 = Pattern.compile("MINUS *\\\\\\{([^\\\\}]+)\\\\}");
		Matcher matcher1 = pattern1.matcher(s);
		while (matcher1.find()){
			res.add(matcher1.group(1).trim());
		}		
		return res;
	}


	//OLD VERSION
	public ClassExpression subgraphFormToEDOALClass(String s, String focus){
		ClassExpression expr = null;
		s=s.replaceAll("[\n\t ]+", " ");
		s=s.replaceAll("[\n\t ]+\\.", "\\.");
		s=s.replaceAll("\\\\*\\{", "\\\\\\{");
		s=s.replaceAll("\\\\*}", "\\\\\\}");
		focus = focus.replaceAll("\\?", "\\\\\\?");
		try {
			ArrayList<ClassExpression> expressions = new ArrayList<>();

			//focus predicate ?y.
			Pattern pattern1 = Pattern.compile(focus+" <([^ ]+)>\\+? (\\?[A-Za-z\\d_-]+)\\.");
			Matcher matcher1 = pattern1.matcher(s);
			while (matcher1.find()){
				s=s.replaceAll(matcher1.group().replaceAll("\\?","\\\\\\?"), "");
				RelationId pred = new RelationId(new URI(matcher1.group(1).trim()));
				ClassExpression newExpression = subgraphFormToEDOALClass(s,matcher1.group(2).trim());

				if(newExpression == null){ //It's a CAE
					expressions.add(new ClassOccurenceRestriction(pred, Comparator.GREATER, 0));
				}
				else{ //It's a CAT
					expressions.add(new ClassDomainRestriction(pred,false,newExpression));
				}
			}
			//?y predicate focus
			Pattern pattern2 = Pattern.compile("(\\?[A-Za-z\\d_-]+) <([^>]+)>\\+? "+focus+"\\.");
			Matcher matcher2 = pattern2.matcher(s);
			while (matcher2.find()){
				s=s.replaceAll(matcher2.group().replaceAll("\\?","\\\\\\?"), "");
				RelationId predId = new RelationId(new URI(matcher2.group(2).trim()));
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				setPredId.add(predId);
				RelationConstruction pred = new RelationConstruction(Constructor.INVERSE,setPredId);
				ClassExpression newExpression = subgraphFormToEDOALClass(s,matcher2.group(1).trim());

				if(newExpression == null){ //It's a CIAE
					//System.out.println("CIAE: pred="+predId);
					expressions.add(new ClassOccurenceRestriction(pred, Comparator.GREATER, 0));
				}
				else{ //It's a CIAT
					//System.out.println("CIAT: pred="+predId+" class="+newExpression.toString());
					expressions.add(new ClassDomainRestriction(pred,false,newExpression));
				}
			}
			//focus a Class
			Pattern pattern3 = Pattern.compile(focus+" (a)?(<http://www\\.w3\\.org/1999/02/22-rdf-syntax-ns#type>)? <([^>]+)>\\.");
			Matcher matcher3 = pattern3.matcher(s);
			while (matcher3.find()){ //It's a Class URI
				s=s.replaceAll(matcher3.group().replaceAll("\\?","\\\\\\?"), "");
				ClassId classId = new ClassId(new URI(matcher3.group(3).trim()));
				expressions.add(classId);
			}

			//focus predicate Instance
			Pattern pattern4 = Pattern.compile(focus+" <([^>]+)>\\+? <([^>]+)>\\.");
			Matcher matcher4 = pattern4.matcher(s);
			while (matcher4.find()){ //It's a CAV (instance)
				s=s.replaceAll(matcher4.group().replaceAll("\\?","\\\\\\?"), "");
				RelationId pred = new RelationId(new URI(matcher4.group(1).trim()));
				InstanceId inst = new InstanceId(new URI(matcher4.group(2).trim()));
				expressions.add(new ClassValueRestriction(pred,Comparator.EQUAL, inst));
			}

			//Instance predicate focus
			Pattern pattern5 = Pattern.compile("<([^>]+)> <([^>]+)>\\+? "+focus+"\\.");
			Matcher matcher5 = pattern5.matcher(s);
			while (matcher5.find()){ //It's a CIAV (instance)
				s=s.replaceAll(matcher5.group().replaceAll("\\?","\\\\\\?"), "");
				RelationId predId = new RelationId(new URI(matcher5.group(2).trim()));
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				setPredId.add(predId);
				RelationConstruction pred = new RelationConstruction(Constructor.INVERSE,setPredId);
				InstanceId inst = new InstanceId(new URI(matcher5.group(1).trim()));
				expressions.add(new ClassValueRestriction(pred,Comparator.EQUAL, inst));
			}

			//focus predicate LitteralValue
			Pattern pattern6 = Pattern.compile(focus+" <([^>]+)>\\+? \"([^\"]+)\"\\.");
			Matcher matcher6 = pattern6.matcher(s);
			while (matcher6.find()){ //It's a CAV (value)
				s=s.replaceAll(matcher6.group().replaceAll("\\?","\\\\\\?"), "");
				RelationId pred = new RelationId(new URI(matcher6.group(1).trim()));
				Value value = new Value(matcher6.group(2).trim());
				expressions.add(new ClassValueRestriction(pred,Comparator.EQUAL, value));
			}

			//focus predicate SetOfValues
			Pattern pattern7 = Pattern.compile(focus+" <([^>]+)>\\+? \\\\\\{([^\\\\}]+)\\\\}\\.");
			Matcher matcher7 = pattern7.matcher(s);
			while (matcher7.find()){ //It's a U(CAV)
				s=s.replaceAll(matcher7.group().replaceAll("\\?","\\\\\\?"), "");
				ArrayList<ClassExpression> setOfCAV = new ArrayList<>();
				RelationId pred = new RelationId(new URI(matcher7.group(1).trim()));
				String[] values = matcher7.group(2).trim().split(",");
				for(String v: values){
					if(v.contains(">") && v.contains(">")){
						InstanceId value = new InstanceId(new URI(v.replaceAll("[<> ]", "")));
						setOfCAV.add(new ClassValueRestriction(pred,Comparator.EQUAL, value));
					}
					else{
						Value value = new Value(v);
						setOfCAV.add(new ClassValueRestriction(pred,Comparator.EQUAL, value));
					}
				} 
				expressions.add(new ClassConstruction(Constructor.OR,setOfCAV));
			}

			//SetOfValues predicate focus
			Pattern pattern8 = Pattern.compile("\\\\\\{([^\\\\}]+)\\\\} <([^>]+)>\\+? "+focus+"\\.");
			Matcher matcher8 = pattern8.matcher(s);
			while (matcher8.find()){ //It's a U(CIAV)
				s=s.replaceAll(matcher8.group().replaceAll("\\?","\\\\\\?"), "");
				ArrayList<ClassExpression> setOfCAV = new ArrayList<>();
				RelationId predId = new RelationId(new URI(matcher8.group(2).trim()));
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				setPredId.add(predId);
				RelationConstruction pred = new RelationConstruction(Constructor.INVERSE,setPredId);
				String[] values = matcher8.group(1).trim().split(",");
				for(String v: values){
					InstanceId value = new InstanceId(new URI(v.replaceAll("[<> ]", "")));
					setOfCAV.add(new ClassValueRestriction(pred,Comparator.EQUAL, value));
				} 
				expressions.add(new ClassConstruction(Constructor.OR,setOfCAV));
			}

			//focus setOfpredicate ?y
			Pattern pattern9 = Pattern.compile(focus+" \\\\\\{([^\\\\}]+)\\\\} (\\?[A-Za-z\\d_-]+)\\.");
			Matcher matcher9 = pattern9.matcher(s);
			while (matcher9.find()){ //It's a U(CAE/T)
				s=s.replace(matcher9.group(), "");
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				String[] preds = matcher9.group(1).trim().split(",");
				for(String predUri: preds){
					RelationId pred = new RelationId(new URI(predUri.replaceAll("[<> ]", "")));
					setPredId.add(pred);
				} 
				ClassExpression newExpression = subgraphFormToEDOALClass(s,matcher9.group(2).trim());
				RelationConstruction relConstr = new RelationConstruction(Constructor.OR,setPredId);
				if(newExpression == null){ //It's a U(CAE)
					expressions.add(new ClassOccurenceRestriction(relConstr, Comparator.GREATER, 0));
				}
				else{ //It's a U(CAT)
					expressions.add(new ClassDomainRestriction(relConstr,false,newExpression));
				}
			}

			//?y setOfpredicate focus
			Pattern pattern10 = Pattern.compile("(\\?[A-Za-z\\d_-]+) \\\\\\{([^\\\\}]+)\\\\} "+focus+"\\.");
			Matcher matcher10 = pattern10.matcher(s);
			while (matcher10.find()){ //It's a U(CAE/T)
				s=s.replace(matcher10.group(), "");
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				String[] preds = matcher10.group(2).trim().split(",");
				for(String predUri: preds){
					RelationId predInv = new RelationId(new URI(predUri.replaceAll("[<> ]", "")));
					ArrayList<RelationExpression> setPredIdInv= new ArrayList<>();
					setPredIdInv.add(predInv);
					RelationConstruction pred = new RelationConstruction(Constructor.INVERSE,setPredIdInv);
					setPredId.add(pred);
				} 
				ClassExpression newExpression = subgraphFormToEDOALClass(s,matcher10.group(1).trim());
				RelationConstruction relConstr = new RelationConstruction(Constructor.OR,setPredId);
				if(newExpression == null){ //It's a U(CIAE)
					expressions.add(new ClassOccurenceRestriction(relConstr, Comparator.GREATER, 0));
				}
				else{ //It's a U(CIAT)
					expressions.add(new ClassDomainRestriction(relConstr,false,newExpression));
				}
			}

			//focus setOfpredicate Value(instance)
			Pattern pattern11 = Pattern.compile(focus+" \\\\\\{([^\\\\}]+)\\\\} <([^>]+)>\\.");
			Matcher matcher11 = pattern11.matcher(s);
			while (matcher11.find()){ //It's a C(UA)V
				s=s.replace(matcher11.group(), "");
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				String[] preds = matcher11.group(1).trim().split(",");
				for(String predUri: preds){
					RelationId pred = new RelationId(new URI(predUri.replaceAll("[<> ]", "")));
					setPredId.add(pred);
				} 
				RelationConstruction relConstr = new RelationConstruction(Constructor.OR,setPredId);
				InstanceId inst = new InstanceId(new URI(matcher11.group(2).trim()));
				expressions.add(new ClassValueRestriction(relConstr,Comparator.EQUAL, inst));
			}


			//Value setOfpredicate focus
			Pattern pattern12 = Pattern.compile("<([^>]+)> \\\\\\{([^\\\\}]+)\\\\} "+focus+"\\.");
			Matcher matcher12= pattern12.matcher(s);
			while (matcher12.find()){ //It's a C(UIA)V
				s=s.replace(matcher12.group(), "");
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				String[] preds = matcher12.group(2).trim().split(",");
				for(String predUri: preds){
					RelationId predInv = new RelationId(new URI(predUri.replaceAll("[<> ]", "")));
					ArrayList<RelationExpression> setPredIdInv= new ArrayList<>();
					setPredIdInv.add(predInv);
					RelationConstruction pred = new RelationConstruction(Constructor.INVERSE,setPredIdInv);
					setPredId.add(pred);
				} 
				RelationConstruction relConstr = new RelationConstruction(Constructor.OR,setPredId);
				InstanceId inst = new InstanceId(new URI(matcher12.group(1).trim()));
				expressions.add(new ClassValueRestriction(relConstr,Comparator.EQUAL, inst));
			}

			//focus setOfpredicate Value(literal)
			Pattern pattern13 = Pattern.compile(focus+" \\\\\\{([^\\\\}]+)\\\\} \"([^\"]+)\"\\.");
			Matcher matcher13 = pattern13.matcher(s);
			while (matcher13.find()){ //It's a C(UA)V
				s=s.replaceAll(matcher13.group().replaceAll("\\?","\\\\\\?"), "");
				ArrayList<RelationExpression> setPredId= new ArrayList<>();
				String[] preds = matcher13.group(1).trim().split(",");
				for(String predUri: preds){
					RelationId pred = new RelationId(new URI(predUri.replaceAll("[<> ]", "")));
					setPredId.add(pred);
				} 
				RelationConstruction relConstr = new RelationConstruction(Constructor.OR,setPredId);
				Value val = new Value(matcher13.group(2).trim());
				expressions.add(new ClassValueRestriction(relConstr,Comparator.EQUAL, val));
			}


			if (expressions.size() == 1){
				expr=expressions.get(0);
			}
			else if (expressions.size() > 1){
				expr = new ClassConstruction(Constructor.AND,expressions);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return expr;
	}


	public RelationExpression subgraphFormToEDOALProperty(String s, String focus1, String focus2){
		s=s.replaceAll("[\n\t ]+", " ");
		s=s.replaceAll("[\n\t ]+\\.", "\\.");
		s=s.replaceAll("\\\\*\\{", "\\\\\\{");
		s=s.replaceAll("\\\\*}", "\\\\\\}");
		s=s.replaceAll("\\+", "");
		String sCopy = s;
		HashMap<String, SPARQLNode> nodes = new HashMap<>();


		Pattern pattern1 = Pattern.compile("(\\?[A-Za-z\\d_-]+) <[^>]+> (\\?[A-Za-z\\d_-]+)\\.");
		Matcher matcher1 = pattern1.matcher(s);
		while (matcher1.find()){ 
			s=s.replaceAll(matcher1.group().replaceAll("\\?","\\\\\\?"), "");
			String n1= matcher1.group(1);
			String n2 = matcher1.group(2);
			String triple = matcher1.group();

			SPARQLNode node1 = new SPARQLNode(n1);
			SPARQLNode node2 = new SPARQLNode(n2);
			if(nodes.containsKey(n1)) {
				node1 = nodes.get(n1);
			}
			else {
				nodes.put(n1, node1);
			}
			if(nodes.containsKey(n2)) {
				node2 = nodes.get(n2);
			}	
			else {
				nodes.put(n2, node2);
			}
			node1.addNeighbour(node2, triple);
			node2.addNeighbour(node1, triple);
		}
		
		//Parcours de graphe
		ArrayList<SPARQLNode> nodesToVisit = new ArrayList<>();
		nodesToVisit.add(nodes.get(focus1));
		boolean pathFound = false;
		
		while(!nodesToVisit.isEmpty() && !pathFound && nodesToVisit.get(0) != null) {
			SPARQLNode currNode = nodesToVisit.get(0);
			currNode.explore();
			if(currNode.hasNeighbor(focus2)) {
				nodes.get(focus2).setPredecessor(currNode);
				pathFound = true;
				nodesToVisit.remove(0);
			}
			else {
				nodesToVisit.remove(0);
				for(SPARQLNode newNode: currNode.getNeighbors().values()) {
					if(!newNode.isExplored()) {
						newNode.setPredecessor(currNode);
						nodesToVisit.add(newNode);
					}
				}
			}
		}
		
		
		ArrayList <String> properties = new ArrayList<>() ;
		ArrayList<Boolean> inverse= new ArrayList<>();
		ArrayList<ClassExpression> types = new ArrayList<>();
		if (pathFound) {
		//start from nodes.get(focus2)
			SPARQLNode currNode = nodes.get(focus2);
			String predName = currNode.getPredecessor().getName();
			boolean endOfPath = false;
			while (!endOfPath) {
				String triple = currNode.getTriple(predName);				
				//get property and inverse
				Pattern pattern2 = Pattern.compile("(\\?[A-Za-z\\d_-]+) <([^>]+)> (\\?[A-Za-z\\d_-]+)\\.");
				Matcher matcher2 = pattern2.matcher(triple);
				if (matcher2.find()) {
					sCopy = sCopy.replaceAll(matcher2.group().replaceAll("\\?","\\\\\\?"), "");
					properties.add(0,matcher2.group(2));
					
					if(matcher2.group(1).equals(currNode.getName())) {
						inverse.add(0,true);
					}
					else {
						inverse.add(0,false);
					}
				}
				//Get ClassExpressions
				types.add(0, subgraphFormToEDOALClass(sCopy,currNode.getName()));
				if(predName.equals(focus1)) {
					endOfPath = true;
				}
				else {
					//Go to following
					currNode = nodes.get(predName);
					predName = currNode.getPredecessor().getName();
				}
			}
			types.add(0, subgraphFormToEDOALClass(sCopy,focus1));
		}	
		else { // IF path not found (e.g. in a minus)
			types.add(subgraphFormToEDOALClass(s,focus1));
			types.add(subgraphFormToEDOALClass(s,focus2));
		}
	
		return subgraphFormToEDOALProperty(properties, inverse, types);
	}

	
	public RelationExpression subgraphFormToEDOALProperty(ArrayList<String> properties, ArrayList<Boolean> inverse, ArrayList<ClassExpression> types) {
		RelationExpression expr= null;
		ArrayList<RelationExpression> setRelComp = new ArrayList<>();
			try {
				for (int i = 0; i < inverse.size(); i++) {
					RelationExpression rel = null;
					ArrayList<RelationExpression> setRelAnd = new ArrayList<>();
					if(types.get(i) != null) {
						setRelAnd.add(new RelationDomainRestriction(types.get(i)));
					}
					if(i+1 == properties.size() && types.get(i+1) != null ) {
						setRelAnd.add(new RelationCoDomainRestriction(types.get(i+1)));
					}
					if(inverse.get(i)) {
						ArrayList<RelationExpression> setRelInv = new ArrayList<>();
						setRelInv.add(new RelationId(new URI(properties.get(i))));
						rel = new RelationConstruction(Constructor.INVERSE,setRelInv);
					}   
					else {
						rel = new RelationId(new URI(properties.get(i)));
					}
					setRelAnd.add(rel);
					if(setRelAnd.size() > 1) {
						setRelComp.add(new RelationConstruction(Constructor.AND,setRelAnd));
					}
					else {
						setRelComp.add(rel);
					}
				}
				
				//only for a domain/range restriction
				if(properties.isEmpty() && types.size()==2) {
					ArrayList<RelationExpression> setRelAnd = new ArrayList<>();
					if(types.get(0)!=null) {
						setRelAnd.add(new RelationDomainRestriction(types.get(0)));
					}
					if(types.get(1)!=null) {
						setRelAnd.add(new RelationCoDomainRestriction(types.get(1)));
					}
					if(setRelAnd.size() > 1) {
						setRelComp.add(new RelationConstruction(Constructor.AND,setRelAnd));
					}
					else if (setRelAnd.size() ==1) {
						setRelComp.add(setRelAnd.get(0));
					}
					
					
				}
				

				if(setRelComp.size()==1) {
					expr=setRelComp.get(0);
				}
				else{
					expr = new RelationConstruction(Constructor.COMP,setRelComp);
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		return expr;
		
	}
	public RelationExpression subgraphFormToEDOALProperty(SubgraphForOutput s){
		ArrayList <String> properties = new ArrayList<>() ;
		ArrayList<Boolean> inverse= new ArrayList<>();
		ArrayList<ClassExpression> types = new ArrayList<>();
		if (s instanceof PathSubgraph) {
			try {

				Path p = ((PathSubgraph)s).getMainPath(); 
				inverse=p.getInverse();
			
				for (int i = 0; i < p.getProperties().size(); i++) {
					properties.add(p.getProperties().get(i).toStrippedString());
					if(p.getTypes().get(i) != null) {
						types.add(new ClassId(new URI(p.getTypes().get(i).toStrippedString())));
					}
					else {
						types.add(null);
					}
				}
				
				if(p.getTypes().get(p.getProperties().size()) != null) {
					types.add(new ClassId(new URI(p.getTypes().get(p.getProperties().size()).toStrippedString())));
				}
				else {
					types.add(null);
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		return subgraphFormToEDOALProperty(properties,inverse,types);
	}
}
