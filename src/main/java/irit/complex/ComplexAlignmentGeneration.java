package irit.complex;

import irit.complex.answer.Answer;
import irit.complex.answer.PairAnswer;
import irit.complex.answer.SingleAnswer;
import irit.complex.subgraphs.*;
import irit.dataset.DatasetManager;
import irit.misc.Progress;
import irit.output.OutputManager;
import irit.resource.IRI;
import irit.resource.Resource;
import irit.similarity.EmbeddingManager;
import irit.sparql.exceptions.IncompleteSubstitutionException;
import irit.sparql.SparqlProxy;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import irit.sparql.query.select.SparqlSelect;
import org.apache.jena.rdf.model.RDFNode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;


public class ComplexAlignmentGeneration {


    public static void main(String[] args) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, ExecutionException, InterruptedException, IncompleteSubstitutionException {

        System.out.println("===============================================================================");
        System.out.println("CanardE");
        System.out.println("===============================================================================");

        String datasets = "/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100";
        String source = "cmt_100.ttl";
        String target = "conference_100.ttl";

        Set<String> stringSet = Set.of(source, target);

        Map<String, String> ds = new HashMap<>();

        try {
            Files.walk(Paths.get(datasets), 1).forEach(path -> {
                if (!path.toString().endsWith(".ttl") && !stringSet.contains(path.getFileName().toString())) return;
                ds.put(path.getFileName().toString().split("_")[0], path.toString());
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Found " + ds.size() + " datasets.");

        String needs = "/home/guilherme/IdeaProjects/ComplexAlignmentGenerator/needs";

        Map<String, String> nd = new HashMap<>();
        Map<String, List<SparqlSelect>> cqas = new HashMap<>();

        try {
            Files.walk(Paths.get(needs), 1).forEach(path -> {
                String ont = path.getFileName().toString();
                if (!ds.containsKey(ont)) return;
                nd.put(ont, path.toString());


                try {
                    Files.walk(path, 1).forEach(path1 -> {
                        if (Files.isDirectory(path1)) return;
                        Scanner squery = null;
                        try {
                            squery = new Scanner(path1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        String query = squery.useDelimiter("\\Z").next();
                        SparqlSelect sq = new SparqlSelect(query);
                        cqas.computeIfAbsent(ont, s -> new ArrayList<>()).add(sq);
                        squery.close();
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }




            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String s : ds.keySet()) {
            if (nd.containsKey(s)) continue;
            System.out.println("⚠️ Not found CQAs for " + s + ".");
        }

        String embeddings = "/home/guilherme/Documents/canard/run/glove";
        Map<String, String[]> embs = new HashMap<>();
        try {
            Files.walk(Paths.get(embeddings), 1).forEach(path -> {
                if (Files.isDirectory(path)) return;
                String f = path.getFileName().toString();
                String[] split = f.split("[_.]");

                if (split[1].equals("n")) embs.computeIfAbsent(split[0], s -> new String[2])[0] = path.toString();
                else if (split[1].equals("e")) embs.computeIfAbsent(split[0], s -> new String[2])[1] = path.toString();

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String range = "0:1.1:0.1";

        String[] split = range.split(":");

        List<Float> ths = new ArrayList<>();

        for (float th = Float.parseFloat(split[0]); th <= Float.parseFloat(split[1]); th += Float.parseFloat(split[2])) {
            ths.add(th);
        }

        embs.forEach((name, paths) -> {
            try {
                EmbeddingManager.load(paths[0], paths[1]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        ds.forEach((name, path) -> {
            DatasetManager.getInstance().load(name, path);
        });


        List<String[]> datasetArgs = new ArrayList<>();


        ds.forEach((s, s2) -> {
            if (source != null && !source.startsWith(s)) return;
            ds.forEach((s1, s21) -> {
                if (s.equals(s1)) return;
                if (target != null && !target.startsWith(s1)) return;
                datasetArgs.add(new String[]{s, s1});
            });
        });



        int tc = 1;
//        ExecutorService executorService = Executors.newFixedThreadPool(tc);

        String output = "output";

        for (String[] datasetArg : datasetArgs) {
            run(datasetArg[0], datasetArg[1], cqas.get(datasetArg[0]), ths, 10, false, output);
//            progress.step();
        }


    }


    public static void run(String sourceEndpoint, String targetEndpoint, List<SparqlSelect> queries, List<Float> th, int maxMatches, boolean reassess, String outputPath) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, ExecutionException, InterruptedException, IncompleteSubstitutionException {
        OutputManager outputManager = new OutputManager();
        outputManager.initOutputEdoal(sourceEndpoint, targetEndpoint, th, outputPath);




        for (SparqlSelect sq : queries) {
            align(sq, sourceEndpoint, targetEndpoint, maxMatches, reassess, th, outputManager);
        }


        outputManager.endOutput();
    }


    public static void align(SparqlSelect sq, String sourceEndpoint, String targetEndpoint, int maxMatches, boolean reassess, List<Float> th, OutputManager outputManager) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, ExecutionException, InterruptedException, IncompleteSubstitutionException {
        Set<Answer> matchedAnswers = getMatchedAnswers(sq, sourceEndpoint, targetEndpoint, maxMatches);


        for (float threshold : th) {

            List<SubgraphForOutput> subgraphForOutputs = buildSingleOutput(matchedAnswers, sq, sourceEndpoint, targetEndpoint, threshold, reassess);

            if (!subgraphForOutputs.isEmpty()) {
                outputManager.addToOutput(threshold, sq, subgraphForOutputs);
            }

        }


    }


    public static Set<Answer> getMatchedAnswers(SparqlSelect sq, String sourceEndpoint, String targetEndpoint, int maxMatches) throws SparqlEndpointUnreachableException, SparqlQueryMalFormedException, IncompleteSubstitutionException {
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
        HashSet<InstantiatedSubgraph> goodSubgraphs = new HashSet<>();
        for (Answer ans : matchedAnswers) {

            HashSet<InstantiatedSubgraph> localSubgraphs = ans.findCorrespondingSubGraph(sq, targetEndpoint, threshold);
            goodSubgraphs.addAll(localSubgraphs);
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

//        System.out.println("Number of correspondences found (" + threshold + "): " + output.size());

        if (reassess) {
            System.out.println("Reassessing similarity");
            for (SubgraphForOutput s : output) {
                s.reassessSimilarityWithCounterExamples(sourceEndpoint, targetEndpoint, sq);
            }
        }

        Collections.sort(output);
        ArrayList<SubgraphForOutput> singleOutput = new ArrayList<>();
        if (output.size() > 0 && output.get(output.size() - 1).getSimilarity() < 0.6 && output.get(output.size() - 1).getSimilarity() > 0.01) {
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
                if (!s1.equals("") && !s2.equals("")) {
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

