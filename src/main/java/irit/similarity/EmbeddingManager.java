package irit.similarity;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EmbeddingManager {


    private static final Map<String, INDArray> embs1 = new HashMap<>();
    private static final Pattern pattern = Pattern.compile("([^>]+)[#/]([A-Za-z0-9_-]+)");
    public static long[] embshape;
    private static LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();

    public static double similarity(Set<String> labels1, HashSet<String> labels2, double threshold) {
        double score = 0;
        Set<String> lab1 = labels1.stream().map(EmbeddingManager::getSuffix).map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> lab2 = labels2.stream().map(EmbeddingManager::getSuffix).map(String::toLowerCase).collect(Collectors.toSet());

        for (String l1 : lab1) {
            for (String l2 : lab2) {
                double sim = 1 - levenshteinDistance.apply(l1, l2) / (float) Math.max(l1.length(), l2.length());
                sim = sim < threshold ? 0 : sim;
                score += sim;
            }
        }
        return score;
    }

    private static String getSuffix(String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(2) : value;
    }

    private static Map<String, INDArray> loadEmbs(String n1, String e1) throws IOException {
        List<String> ents = Files.readAllLines(Paths.get(n1));
        List<String> embs = Files.readAllLines(Paths.get(e1));

        Map<String, INDArray> embsMap = new HashMap<>();

        for (int i = 0; i < ents.size(); i++) {
            String[] split = embs.get(i).split(", ");
            double[] de = new double[split.length];
            for (int j = 0; j < split.length; j++) {
                de[j] = Double.parseDouble(split[j]);
            }
            INDArray indArray = Nd4j.create(de);
            embsMap.put(ents.get(i), indArray);
        }
        return embsMap;
    }

    public static INDArray get(String e1) {
        if (!embs1.containsKey(e1)) return Nd4j.zeros(DataType.DOUBLE, embshape);
        return embs1.get(e1);
    }


    private static String processLabel(String line) {
        line = line.replaceAll("\\\\n", "\\n").trim();
        if (line.startsWith("http://") && line.contains("#")) {
            String[] split = line.split("#");
            if (split.length > 1) {
                line = split[1];
            } else {
                line = split[0];
            }
        }
        return line;
    }

}
