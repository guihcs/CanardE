package irit.main;

import irit.complex.answer.Answer;
import irit.complex.answer.PairAnswer;
import irit.complex.answer.SingleAnswer;
import irit.complex.subgraphs.*;
import irit.dataset.DatasetManager;
import irit.output.OutputManager;
import irit.resource.IRI;
import irit.resource.Resource;
import irit.sparql.SparqlProxy;
import irit.sparql.exceptions.IncompleteSubstitutionException;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import irit.sparql.query.select.SparqlSelect;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.jena.rdf.model.RDFNode;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import irit.similarity.EmbeddingManager;


public class ComplexAlignmentGeneration {


    public static void main(String[] args) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IncompleteSubstitutionException, IOException {

        ArgumentParser parser = buildArgumentParser();

        try {
            Namespace res = parser.parseArgs(args);
            String source = res.get("source");
            String target = res.get("target");
            String cqa = res.get("cqa");
            String range = res.get("range");
            String output = res.get("output");
            int maxMatches = res.get("maxMatches");
            List<String> embeddings = res.get("embeddings");

            if (embeddings != null) {
                EmbeddingManager.loadEmbeddings(embeddings);
            }


            String sourceName = getFileName(source);
            String targetName = getFileName(target);

            List<SparqlSelect> sparqlSelects = SparqlSelect.load(cqa);

            List<Float> rangeList = parseRange(range);

            DatasetManager.getInstance().load(sourceName, source);
            DatasetManager.getInstance().load(targetName, target);


            run(sourceName, targetName, sparqlSelects, rangeList, maxMatches, false, output);


            DatasetManager.getInstance().close();


        } catch (ArgumentParserException e) {
            parser.handleError(e);
        }


    }


    public static ArgumentParser buildArgumentParser() {
        ArgumentParser parser = ArgumentParsers.newFor("Canard").build()
                .description("Complex alignment generator.");

        parser.addArgument("source")
                .type(String.class)
                .required(true)
                .help("Source ontology.");

        parser.addArgument("target")
                .type(String.class)
                .required(true)
                .help("Target ontology.");

        parser.addArgument("cqa")
                .type(String.class)
                .required(true)
                .help("CQA folder.");

        parser.addArgument("--range")
                .type(String.class)
                .setDefault("0.8")
                .help("Threshold range.");

        parser.addArgument("--output")
                .type(String.class)
                .setDefault("output")
                .help("Output folder.");

        parser.addArgument("--silent")
                .type(Boolean.class)
                .action(Arguments.storeConst())
                .setConst(true)
                .setDefault(false)
                .help("Disable console output.");

        parser.addArgument("--maxMatches")
                .type(Integer.class)
                .setDefault(10)
                .help("Max Matches.");

        parser.addArgument("--embeddings")
                .type(String.class)
                .nargs("+")
                .help("Paths to embeddings files.");

        return parser;
    }

    public static String getFileName(String path) {
        String[] split = Paths.get(path).getFileName().toString().split("\\.");
        return split[0];
    }


    public static List<Float> parseRange(String range) {
        List<Float> ranges = new ArrayList<>();
        String[] split = range.split(":");

        float start = Float.parseFloat(split[0]);
        float end = start;
        float step = 0.1f;


        if (split.length > 1) end = Float.parseFloat(split[1]);
        if (split.length > 2) step = Float.parseFloat(split[2]);

        ranges.add(start);
        start += step;
        for (; start < end; start += step) {
            ranges.add(start);
        }

        return ranges;
    }


    public static void run(String sourceEndpoint, String targetEndpoint, List<SparqlSelect> queries, List<Float> th, int maxMatches, boolean reassess, String outputPath) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {

        OutputManager outputManager = new OutputManager();
        outputManager.initOutputEdoal(sourceEndpoint, targetEndpoint, th, outputPath);


        for (SparqlSelect sq : queries) {
            align(sq, sourceEndpoint, targetEndpoint, maxMatches, reassess, th, outputManager);
        }


        outputManager.endOutput();
    }


    public static void align(SparqlSelect sq, String sourceEndpoint, String targetEndpoint, int maxMatches, boolean reassess, List<Float> th, OutputManager outputManager) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        Set<Answer> matchedAnswers = getMatchedAnswers(sq, sourceEndpoint, targetEndpoint, maxMatches);

        for (float threshold : th) {
            List<SubgraphForOutput> subgraphForOutputs = buildSingleOutput(matchedAnswers, sq, sourceEndpoint, targetEndpoint, threshold, reassess);
            if (!subgraphForOutputs.isEmpty()) {
                outputManager.addToOutput(threshold, sq, subgraphForOutputs);
            }

        }

    }


    public static Set<Answer> getMatchedAnswers(SparqlSelect sq, String sourceEndpoint, String targetEndpoint, int maxMatches) {
        HashMap<String, IRI> iriList = sq.getIRIList();
        for (Map.Entry<String, IRI> m : iriList.entrySet()) {
            m.getValue().retrieveLabels(sourceEndpoint);
        }

        ArrayList<Answer> answers = new ArrayList<>();
        HashSet<Answer> matchedAnswers = new HashSet<>();
        int offsetMatch = 0;

        boolean noMoreSourceAnswers = false;
        int offset = 0;
        int limit = 2000;
        while (!noMoreSourceAnswers && matchedAnswers.size() < maxMatches) {

            String queryLimit = " LIMIT " + limit + "\n OFFSET " + offset;

            if (sq.getFocusLength() == 1) {
                List<Map<String, RDFNode>> ret = loadUnary(sourceEndpoint, sq, answers, queryLimit);
                if (ret.size() < limit) {
                    noMoreSourceAnswers = true;
                }
            } else if (sq.getFocusLength() == 2) {
                List<Map<String, RDFNode>> ret = loadBinary(sourceEndpoint, sq, answers, queryLimit);
                if (ret.size() < limit) {
                    noMoreSourceAnswers = true;
                }
            } else {
                System.out.println("ERROR for query : " + sq.toUnchangedString());
                System.err.println("Problem detected: too many variables in SELECT: can only deal with 1 or 2");
                noMoreSourceAnswers = true;
            }


            if (!noMoreSourceAnswers) {
                offset += limit;
            }

            while (matchedAnswers.size() < maxMatches && offsetMatch < answers.size()) {
                Answer ans = answers.get(offsetMatch);
                ans.getExistingMatches(sourceEndpoint, targetEndpoint);
                if (ans.hasMatch()) {
                    matchedAnswers.add(ans);
                }
                offsetMatch++;
            }
        }
        answers.sort(Comparator.comparing(Answer::toString));
        if (matchedAnswers.isEmpty()) {
            Iterator<Answer> ansIt = answers.iterator();
            while (matchedAnswers.size() < maxMatches && ansIt.hasNext()) {
                Answer ans = ansIt.next();
                ans.retrieveIRILabels(sourceEndpoint);
                ans.getSimilarIRIs(targetEndpoint);
                if (ans.hasMatch()) {
                    matchedAnswers.add(ans);
                }
            }

        }

        return matchedAnswers;
    }


    private static List<SubgraphForOutput> buildSingleOutput(Set<Answer> matchedAnswers, SparqlSelect sq, String sourceEndpoint, String targetEndpoint, float threshold, boolean reassess) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException {
        List<InstantiatedSubgraph> goodSubgraphs = new ArrayList<>();


        List<Answer> answers = new ArrayList<>(matchedAnswers);
        answers.sort(Comparator.comparing(Answer::toString));

        for (Answer ans : answers) {
            Set<InstantiatedSubgraph> localSubgraphs = ans.findCorrespondingSubGraph(sq, targetEndpoint, threshold);
            List<InstantiatedSubgraph> instantiatedSubgraphs = new ArrayList<>(localSubgraphs);
            instantiatedSubgraphs.sort(Comparator.comparing(InstantiatedSubgraph::toString));
            goodSubgraphs.addAll(instantiatedSubgraphs);
        }

        ArrayList<SubgraphForOutput> output = new ArrayList<>();
        for (InstantiatedSubgraph t : goodSubgraphs) {
            boolean added = false;
            Iterator<SubgraphForOutput> it = output.iterator();
            while (it.hasNext() && !added) {
                SubgraphForOutput subG = it.next();
                if (t instanceof Triple && subG instanceof TripleSubgraph) {
                    added = ((TripleSubgraph) subG).addSubgraph((Triple) t);
                }
                if (t instanceof Path && subG instanceof PathSubgraph) {
                    added = ((PathSubgraph) subG).addSubgraph((Path) t);
                }
            }
            if (!added) {
                if (t instanceof Triple) {
                    output.add(new TripleSubgraph((Triple) t));
                }
                if (t instanceof Path) {
                    output.add(new PathSubgraph((Path) t));
                }
            }
        }

        if (reassess) {
            for (SubgraphForOutput s : output) {
                s.reassessSimilarityWithCounterExamples(sourceEndpoint, targetEndpoint, sq);
            }
        }


        Collections.sort(output);
        output.sort(Comparator.comparing(SubgraphForOutput::toString));
        ArrayList<SubgraphForOutput> singleOutput = new ArrayList<>();

        if (!output.isEmpty() && output.get(output.size() - 1).getSimilarity() < 0.6 && output.get(output.size() - 1).getSimilarity() > 0.01) {
            double sim = output.get(output.size() - 1).getSimilarity();
            boolean moreCorrespondences = true;
            int i = output.size() - 1;
            while (i >= 0 && moreCorrespondences) {
                if (output.get(i).getSimilarity() == sim) {
                    singleOutput.add(output.get(i));
                } else {
                    moreCorrespondences = false;
                }
                i--;
            }
        } else {
            for (SubgraphForOutput s : output) {
                if (s.getSimilarity() >= 0.6) {
                    singleOutput.add(s);
                }
            }
        }


        return singleOutput;
    }

    private static List<Map<String, RDFNode>> loadBinary(String sourceEndpoint, SparqlSelect sq, ArrayList<Answer> answers, String queryLimit) {
        List<Map<String, RDFNode>> result = SparqlProxy.query(sourceEndpoint, sq.toUnchangedString() + queryLimit);
        for (Map<String, RDFNode> response : result) {
            String s1 = response.get(sq.getSelectFocus().get(0).replaceFirst("\\?", "")).toString();
            String s2 = response.get(sq.getSelectFocus().get(1).replaceFirst("\\?", "")).toString();
            boolean type1 = response.get(sq.getSelectFocus().get(0).replaceFirst("\\?", "")).isAnon();
            boolean type2 = response.get(sq.getSelectFocus().get(1).replaceFirst("\\?", "")).isAnon();
            if (!type1 && !type2) {
                if (!s1.isEmpty() && !s2.isEmpty()) {
                    PairAnswer pair = new PairAnswer(new Resource(s1), new Resource(s2));
                    answers.add(pair);
                }
            }
        }
        return result;
    }

    private static List<Map<String, RDFNode>> loadUnary(String sourceEndpoint, SparqlSelect sq, ArrayList<Answer> answers, String queryLimit) {
        List<Map<String, RDFNode>> result = SparqlProxy.query(sourceEndpoint, sq.toUnchangedString() + queryLimit);
        for (Map<String, RDFNode> response : result) {
            String s = response.get(sq.getSelectFocus().get(0).replaceFirst("\\?", "")).toString();
            boolean type = response.get(sq.getSelectFocus().get(0).replaceFirst("\\?", "")).isAnon();
            if (!type) {
                SingleAnswer singleton = new SingleAnswer(new Resource(s));
                answers.add(singleton);
            }

        }
        return result;
    }
}

