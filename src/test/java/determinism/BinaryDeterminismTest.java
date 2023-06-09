package determinism;

import irit.complex.answer.Answer;
import irit.complex.answer.PairAnswer;
import irit.complex.subgraphs.InstantiatedSubgraph;
import irit.complex.subgraphs.Path;
import irit.dataset.DatasetManager;
import irit.main.ComplexAlignmentGeneration;
import irit.resource.IRI;
import irit.sparql.exceptions.IncompleteSubstitutionException;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import irit.sparql.query.select.SparqlSelect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class BinaryDeterminismTest {
    private static final String sourceName = "edas_100";
    private static final String targetName = "ekaw_100";

    private static final String source = "/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/edas_100.ttl";
    private static final String target = "/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/ekaw_100.ttl";

    @Disabled
    @Test
    public void testMatchedAnswerDeterminism() throws IOException, SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IncompleteSubstitutionException {

        String cqa = "src/test/resources";

        List<String> answers = new ArrayList<>();
        List<SparqlSelect> sparqlSelects = SparqlSelect.load(cqa);
        SparqlSelect select = sparqlSelects.get(0);

        for (int i = 0; i < 10; i++) {
            DatasetManager.getInstance().load(sourceName, source);
            DatasetManager.getInstance().load(targetName, target);

            Set<Answer> matchedAnswers = ComplexAlignmentGeneration.getMatchedAnswers(select, sourceName, targetName, 10);
            List<Answer> ma = new ArrayList<>(matchedAnswers);
            ma.sort(Comparator.comparing(Answer::toString));
            answers.add(ma.toString());

            DatasetManager.getInstance().close();
        }


        for (int i = 1; i < answers.size(); i++) {
            Assertions.assertEquals(answers.get(0), answers.get(i));
        }

    }

    //    @Disabled
    @Test
    public void testBinaryPathDeterminism() throws IOException, SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IncompleteSubstitutionException {

        String cqa = "src/test/resources";
        List<String> results = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            DatasetManager.getInstance().load(sourceName, source);
            DatasetManager.getInstance().load(targetName, target);
            List<SparqlSelect> sparqlSelects = SparqlSelect.load(cqa);
            SparqlSelect select = sparqlSelects.get(0);

            Set<Answer> matchedAnswers = ComplexAlignmentGeneration.getMatchedAnswers(select, sourceName, targetName, 10);

            List<InstantiatedSubgraph> goodSubgraphs = new ArrayList<>();

            List<Answer> answers = new ArrayList<>(matchedAnswers);
            answers.sort(Comparator.comparing(Answer::toString));

//            for (Answer ans : answers) {
            Set<InstantiatedSubgraph> localSubgraphs = answers.get(0).findCorrespondingSubGraph(select, targetName, 0.6);
            List<InstantiatedSubgraph> instantiatedSubgraphs = new ArrayList<>(localSubgraphs);
            instantiatedSubgraphs.sort(Comparator.comparing(InstantiatedSubgraph::toString));
            goodSubgraphs.addAll(instantiatedSubgraphs);
//            }

            results.add(goodSubgraphs.toString());
            DatasetManager.getInstance().close();
        }

        for (int i = 1; i < results.size(); i++) {
            Assertions.assertEquals(results.get(0), results.get(i));
        }
    }

    @RepeatedTest(15)
//    @Test
    public void test1() throws IOException, SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IncompleteSubstitutionException {

        String cqa = "src/test/resources";

        DatasetManager.getInstance().load(sourceName, source);
        DatasetManager.getInstance().load(targetName, target);
        List<SparqlSelect> sparqlSelects = SparqlSelect.load(cqa);
        SparqlSelect select = sparqlSelects.get(0);

        Set<Answer> matchedAnswers = ComplexAlignmentGeneration.getMatchedAnswers(select, sourceName, targetName, 10);

        List<Answer> answers = new ArrayList<>(matchedAnswers);
        answers.sort(Comparator.comparing(Answer::toString));

        PairAnswer answer = (PairAnswer) answers.get(0);

        IRI iri = answer.getR1().getSimilarIRIs().stream().toList().get(0);
        IRI y1 = answer.getR2().getSimilarIRIs().stream().toList().get(0);

        Path p1 = new Path(iri, y1, targetName, 3, List.of(true, false, true));
        Path p2 = new Path(iri, y1, targetName, 3, List.of(false, false, true));


        DatasetManager.getInstance().close();

        Assertions.assertTrue(p1.pathFound());
        Assertions.assertFalse(p2.pathFound());
    }
}
