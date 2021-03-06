package irit.complex.answer;

import irit.complex.subgraphs.InstantiatedSubgraph;
import irit.complex.subgraphs.Triple;
import irit.complex.subgraphs.TripleType;
import irit.resource.IRI;
import irit.resource.Resource;
import irit.similarity.EmbeddingManager;
import irit.sparql.SparqlProxy;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import irit.sparql.query.select.SparqlSelect;
import org.apache.jena.rdf.model.RDFNode;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class SingleAnswer extends Answer {
    final Resource res;
    final int numberMaxOfExploredAnswers;


    public SingleAnswer(Resource r) {
        super();
        if (r.isIRI()) {
            res = new IRI("<" + r + ">");
        } else {
            res = r;
        }
        numberMaxOfExploredAnswers = 20;
    }

    public void retrieveIRILabels(String endpointURL) {
        if (res instanceof IRI) {
            ((IRI) res).retrieveLabels(endpointURL);
        }
    }

    public void getSimilarIRIs(String targetEndpoint) {
        if (res.getSimilarIRIs().isEmpty()) {
            res.findSimilarResource(targetEndpoint);
        }
    }

    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) {
        if (res instanceof IRI) {
            ((IRI) res).findExistingMatches(sourceEndpoint, targetEndpoint);
        }
    }

    public HashSet<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, String targetEndpoint, double similarityThreshold) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {

        HashSet<String> queryLabels = query.getLabels();

        INDArray zeros = Nd4j.zeros(DataType.DOUBLE, EmbeddingManager.embshape);

        for (String queryLabel : queryLabels) {
            zeros = zeros.add(EmbeddingManager.get(queryLabel));
        }

        zeros = zeros.div(queryLabels.size());



        double maxSim = -1;
        Triple bestTriple = new Triple();
        HashSet<InstantiatedSubgraph> goodTriples = new HashSet<>();

        int count = 0;
        for (IRI iri : res.getSimilarIRIs()) {
            if (count < numberMaxOfExploredAnswers) {

                count++;
                double localMaxSim = -1;
                retrieveAllTriples(iri, targetEndpoint);

                for (Triple t : iri.getTriples()) {

                    double similarity = 0;
//                    t.retrieveIRILabels(targetEndpoint);
//                    t.retrieveTypes(targetEndpoint);
//                    similarity += t.compareLabel(queryLabels, similarityThreshold, targetEndpoint);
                    similarity += t.compareSim(zeros, similarityThreshold);

                    if (similarity > maxSim) {
                        maxSim = similarity;
                        bestTriple = t;
                    }

                    if (similarity > localMaxSim) {
                        localMaxSim = similarity;
                    }

                    if (similarity >= 0.6) {
                        goodTriples.add(t);
                    }
                }

            }
        }

        if (goodTriples.isEmpty() && !bestTriple.isNullTriple()) {
            goodTriples.add(bestTriple);
        }

        return goodTriples;
    }

    public void retrieveAllTriples(IRI iri, String targetEndpoint) {
        if (!iri.isTriplesRetrieved()) {
            getSubjectTriples(iri, targetEndpoint);
            getObjectTriples(iri, targetEndpoint);
            getPredicateTriples(iri, targetEndpoint);
            iri.setTriplesRetrieved(true);
        }
    }


    private void getPredicateTriples(IRI iri, String targetEndpoint) {
        String query = "SELECT ?subject ?object WHERE {" +
                "?subject " + iri.getValue() + " ?object."
                + "} LIMIT 500";

        List<Map<String, RDFNode>> result = SparqlProxy.query(targetEndpoint, query);


        for (Map<String, RDFNode> response : result) {
            String sub = response.get("subject").toString();
            String obj = response.get("object").toString();
            iri.getTriples().add(new Triple("<" + sub + ">", iri.getValue(), obj, TripleType.PREDICATE));
        }

    }

    private void getObjectTriples(IRI iri, String targetEndpoint) {
        String query = "SELECT ?subject ?predicate WHERE {" +
                "?subject ?predicate " + iri.getValue() + "."
                + "MINUS{ ?subject <http://www.w3.org/2002/07/owl#sameAs> " + iri.getValue() + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#closeMatch> " + iri.getValue() + ".}"
                + "MINUS{ ?subject <http://www.w3.org/2004/02/skos/core#exactMatch> " + iri.getValue() + ".}"
                + "}LIMIT 500";

        List<Map<String, RDFNode>> result = SparqlProxy.query(targetEndpoint, query);


        for (Map<String, RDFNode> response : result) {
            String sub = response.get("subject").toString();
            String pred = response.get("predicate").toString();
            iri.getTriples().add(new Triple("<" + sub + ">", "<" + pred + ">", iri.getValue(), TripleType.OBJECT));
        }

    }

    private void getSubjectTriples(IRI iri, String targetEndpoint) {
        String query = "SELECT ?predicate ?object WHERE {" +
                iri.getValue() + " ?predicate ?object."
                + "MINUS{ " + iri.getValue() + " <http://www.w3.org/2002/07/owl#sameAs> ?object.}"

                + "}LIMIT 500";

        List<Map<String, RDFNode>> result = SparqlProxy.query(targetEndpoint, query);

        for (Map<String, RDFNode> response : result) {
            if (!response.get("object").toString().matches("\"b\\d+\"")) {
                String pred = response.get("predicate").toString();
                String obj = response.get("object").toString();
                iri.getTriples().add(new Triple(iri.getValue(), "<" + pred + ">", obj, TripleType.SUBJECT));
            }

        }

    }


    public String toString() {
        return res.toValueString();
    }

    public int hashCode() {
        return res.toValueString().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SingleAnswer) {
            return res.toValueString().equals(((SingleAnswer) obj).res.toValueString());
        } else {
            return false;
        }
    }

    public boolean hasMatch() {
        return !res.getSimilarIRIs().isEmpty();
    }

    public String printMatchedEquivalents() {
        return res.getSimilarIRIs().toString();
    }

}
