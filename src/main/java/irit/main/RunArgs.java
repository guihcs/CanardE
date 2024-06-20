package irit.main;

import irit.sparql.query.select.SparqlSelect;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RunArgs {


    private String cqa;

    private String outputPath;

    private String sourceName;
    private String targetName;
    private String sourceEndpoint;
    private String targetEndpoint;
    private List<String> embeddings;

    private List<SparqlSelect> queries;
    private List<Double> thresholds;
    private int maxMatches;
    private String simType;
    private String linkType;

    private float embThreshold;
    private boolean bidirectional;

    public static RunArgs fromNamespace(Namespace ns) throws IOException {
        RunArgs args = new RunArgs();

        args.sourceEndpoint = ns.get("source");
        args.targetEndpoint = ns.get("target");

        args.sourceName = getFileName(args.sourceEndpoint);
        args.targetName = getFileName(args.targetEndpoint);


        args.cqa = ns.get("cqa");
        String range = ns.get("range");
        args.thresholds = parseRange(range);

        args.outputPath = ns.get("output");
        args.maxMatches = ns.get("maxMatches");
        args.embeddings = ns.get("embeddings");


        args.queries = SparqlSelect.load(args.cqa);

        args.simType = ns.get("simType");
        args.linkType = ns.get("linkType");
        args.embThreshold = ns.get("embThreshold");

        args.bidirectional = ns.getBoolean("bidirectional");
        return args;
    }

    public static String getFileName(String path) {
        String[] split = Paths.get(path).getFileName().toString().split("\\.");
        return split[0];
    }


    public static List<Double> parseRange(String range) {
        List<Double> ranges = new ArrayList<>();
        String[] split = range.split(":");

        double start = Double.parseDouble(split[0]);
        double end = start;
        double step = 0.1f;


        if (split.length > 1) end = Double.parseDouble(split[1]);
        if (split.length > 2) step = Double.parseDouble(split[2]);

        ranges.add(start);
        start += step;
        for (; start < end; start += step) {
            ranges.add(start);
        }

        return ranges;
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

        parser.addArgument("--simType")
                .type(String.class)
                .setDefault("lev")
                .choices("lev", "cqa_emb", "sub_emb", "i_lev", "i_cqa_emb", "i_sub_emb")
                .help("Similarity type.");

        parser.addArgument("--linkType")
                .type(String.class)
                .setDefault("regex")
                .choices("regex", "emb", "bertint")
                .help("Link type.");

        parser.addArgument("--embThreshold")
                .type(Float.class)
                .setDefault(0.85f)
                .help("Threshold for the embedding similarity in link step.");

        parser.addArgument("--bidirectional")
                .type(Boolean.class)
                .setDefault(false)
                .help("Bidirectional search.");

        return parser;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public List<String> getEmbeddings() {
        return embeddings;
    }

    public List<SparqlSelect> getQueries() {
        return queries;
    }

    public List<Double> getThresholds() {
        return thresholds;
    }

    public int getMaxMatches() {
        return maxMatches;
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isReassess() {
        return false;
    }

    public String getSimType() {
        return simType;
    }

    public String getLinkType() {
        return linkType;
    }

    public float getEmbThreshold() {
        return embThreshold;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }
}
