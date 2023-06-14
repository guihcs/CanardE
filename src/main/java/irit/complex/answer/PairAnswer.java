package irit.complex.answer;

import irit.complex.subgraphs.InstantiatedSubgraph;
import irit.complex.subgraphs.Path;
import irit.resource.IRI;
import irit.resource.Resource;
import irit.sparql.query.select.SparqlSelect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PairAnswer extends Answer {
    final Resource r1;
    final Resource r2;
    boolean similarLooked;

    public PairAnswer(Resource r1, Resource r2) {
        if (r1.isIRI()) {
            this.r1 = new IRI("<" + r1 + ">");
        } else {
            this.r1 = r1;
        }
        if (r2.isIRI()) {
            this.r2 = new IRI("<" + r2 + ">");
        } else {
            this.r2 = r2;
        }
        similarLooked = false;
    }

    public void retrieveIRILabels(String endpointURL) {
        if (r1 instanceof IRI) {
            ((IRI) r1).retrieveLabels(endpointURL);
        }
        if (r2 instanceof IRI) {
            ((IRI) r2).retrieveLabels(endpointURL);
        }

    }


    public void getSimilarIRIs(String targetEndpoint) {
        if (!similarLooked) {

            if (r1 instanceof IRI iri) {
                iri.findSimilarResource(targetEndpoint);

            }
            if (r2 instanceof IRI iri) {
                iri.findSimilarResource(targetEndpoint);

            }

            similarLooked = true;
        }
    }

    public void getExistingMatches(String sourceEndpoint, String targetEndpoint) {

        if (r1 instanceof IRI iri) {
            iri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }
        if (r2 instanceof IRI iri) {
            iri.findExistingMatches(sourceEndpoint, targetEndpoint);
        }

    }


    public HashSet<InstantiatedSubgraph> findCorresponding(Set<String> queryLabels, String targetEndpoint, double similarityThreshold, int currentLen, int maxLen) {

        HashSet<InstantiatedSubgraph> paths = new HashSet<>();

        if (currentLen > maxLen) return paths;


        if (hasR1Match() && hasR2Match()) {
            for (Resource x : r1.getSimilarIRIs()) {
                for (Resource y : r2.getSimilarIRIs()) {
                    queryPaths(targetEndpoint, paths, queryLabels, x, y);

                }
            }
        } else if (hasR1Match()) {
            if (!r2.isIRI()) {
                for (IRI x : r1.getSimilarIRIs()) {
                    queryPaths(targetEndpoint, paths, queryLabels, x, r2);
                }
            }
        } else if (hasR2Match()) {
            if (!r1.isIRI()) {
                for (IRI y : r2.getSimilarIRIs()) {
                    queryPaths(targetEndpoint, paths, queryLabels, r1, y);
                }
            }
        }


        for (InstantiatedSubgraph p : paths) {
            if (p instanceof Path pt) {

                pt.compareLabel(queryLabels, similarityThreshold, targetEndpoint, 0.5);

            } else {
                System.err.println("problem in Pair answer: instantiated subgraph is not a path...");
            }
        }


        if (paths.isEmpty() && !similarLooked) {
            getSimilarIRIs(targetEndpoint);
            System.out.println("No path found, similar answers : " + printMatchedEquivalents());
            paths = findCorresponding(queryLabels, targetEndpoint, similarityThreshold, currentLen + 1, maxLen);
        }

        return paths;
    }

    public void queryPaths(String targetEndpoint, HashSet<InstantiatedSubgraph> paths, Set<String> queryLabels, Resource x, Resource y) {

        Path p = new Path(x, y, targetEndpoint, 5);
        if (!p.pathFound()) return;

        p.getMostSimilarTypes(targetEndpoint, queryLabels, 0.0);
        paths.add(p);
    }

    public HashSet<InstantiatedSubgraph> findCorrespondingSubGraph(SparqlSelect query, String targetEndpoint, double similarityThreshold) {
        final Set<String> queryLabels = query.getLabels();
        return findCorresponding(queryLabels, targetEndpoint, similarityThreshold, 0, 5);

    }

    // has at least one match (r1 or r2)
    public boolean hasMatch() {
        boolean match = !r1.isIRI() || hasR1Match();
        if (r2.isIRI() && !hasR2Match()) {
            match = false;
        }
        return match && hasR1Match() && hasR2Match();
    }

    private boolean hasR1Match() {
        return !r1.getSimilarIRIs().isEmpty();
    }

    private boolean hasR2Match() {
        return !r2.getSimilarIRIs().isEmpty();
    }

    public String toString() {
        return r1.toString() + " " + r2.toString();
    }

    public List<List<Boolean>> allInversePossibilities(int length) {
        List<List<Boolean>> result = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, length); i++) {
            ArrayList<Boolean> invArray = new ArrayList<>();
            StringBuilder invStr = new StringBuilder(Integer.toBinaryString(i));
            while (invStr.length() < length) {
                invStr.insert(0, "0");
            }
            for (char invCh : invStr.toString().toCharArray()) {
                if (invCh == '0') {
                    invArray.add(false);
                } else if (invCh == '1') {
                    invArray.add(true);
                }
            }
            result.add(invArray);
        }

        return result;

    }

    public String printMatchedEquivalents() {
        return r1.getSimilarIRIs().toString() + " <--> " + r2.getSimilarIRIs().toString();
    }

    public Resource getR1() {
        return r1;
    }

    public Resource getR2() {
        return r2;
    }

    public boolean isSimilarLooked() {
        return similarLooked;
    }

    public void setSimilarLooked(boolean similarLooked) {
        this.similarLooked = similarLooked;
    }
}
