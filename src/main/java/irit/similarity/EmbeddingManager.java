package irit.similarity;

import org.apache.commons.text.similarity.LevenshteinDistance;

import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

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
    private static final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();

    public static double similarity(Set<String> labels1, Collection<String> labels2, double threshold) {
        return EmbeddingManager.embs1.isEmpty() ? stringSimilarity(labels1, labels2, threshold) : embeddingSimilarity(labels1, labels2, threshold);
    }

    public static double similarity(Set<String> labels1, INDArray labels2, double threshold) {
        return embeddingSimilarity(labels1, labels2, threshold);
    }

    public static double similarity(INDArray emb1, INDArray emb2, double threshold) {
        double sim = Transforms.cosineSim(emb1, emb2);
        return sim < threshold ? 0 : sim;
    }



    public static double embeddingSimilarity(Set<String> labels1, Collection<String> labels2, double threshold) {
        double score = 0;

        Set<String> lab1 = labels1.stream().map(s -> s.replaceAll("\n+", " ")).collect(Collectors.toSet());
        Set<String> lab2 = labels2.stream().map(s -> s.replaceAll("\n+", " ")).collect(Collectors.toSet());

        for (String l1 : lab1) {
            for (String l2 : lab2) {

                INDArray emb1 = EmbeddingManager.get(l1);
                INDArray emb2 = EmbeddingManager.get(l2);

                double sim = Transforms.cosineSim(emb1, emb2);
                sim = sim < threshold ? 0 : sim;
                score += sim;
            }
        }
        return score;
    }

    public static double embeddingSimilarity(Set<String> labels1, INDArray labels2, double threshold) {
        double score = 0;

        Set<String> lab1 = labels1.stream().map(s -> s.replaceAll("\n+", " ")).collect(Collectors.toSet());

        for (String l1 : lab1) {
            INDArray emb1 = EmbeddingManager.get(l1);

            double sim = Transforms.cosineSim(emb1, labels2);
            sim = sim < threshold ? 0 : sim;
            score += sim;
        }
        return score;
    }

    public static double stringSimilarity(Set<String> labels1, Collection<String> labels2, double threshold) {
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

    public static void loadEmbs(String n1) throws IOException {
        Scanner scanner = new Scanner(Files.newBufferedReader(Paths.get(n1)));
        int size = Integer.parseInt(scanner.nextLine());
        List<String> keys = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            String line = scanner.nextLine();
            if (line.startsWith("http://") && line.contains("#")) {
                String[] split = line.split("#");
                if (split.length > 1) {
                    line = split[1];
                } else {
                    line = split[0];
                }
            } else {
                line = line.toLowerCase();
            }
            keys.add(line);
        }

        for (String key : keys) {

            INDArray indArray = Nd4j.create(doublesFromLine(scanner.nextLine()));
            if (embshape == null) EmbeddingManager.embshape = indArray.shape();
            EmbeddingManager.embs1.put(key, indArray);
        }
    }


    public static double[] doublesFromLine(String line) {
        double[] doubles = new double[100];
        int size = 0;

        double pref = 1.0;
        double value = 0.0;
        boolean dot = false;
        double currentExp = 0.1;
        boolean readExp = false;
        double epref = 1.0;
        double exp = 0.0;

        for (char c : line.toCharArray()) {
            if (c == '-') {
                if (readExp) {
                    epref = -1.0;
                } else {
                    pref = -1.0;
                }
            } else if (Character.isDigit(c)) {
                if (!dot) {
                    value = value * 10 + Character.getNumericValue(c);
                } else {
                    value = value + Character.getNumericValue(c) * currentExp;
                    currentExp *= 0.1;
                }

                if (readExp) {
                    exp = exp * 10 + Character.getNumericValue(c);
                }
            } else if (c == '.') {
                dot = true;
            } else if (c == ' ') {
                value = value * pref;

                if (readExp) {
                    value = value * Math.pow(10, exp * epref);
                }


                if (size == doubles.length) {
                    doubles = Arrays.copyOf(doubles, size * 2);
                }

                doubles[size] = value;
                size++;
                pref = 1.0;
                value = 0.0;
                dot = false;
                currentExp = 0.1;
                readExp = false;
                epref = 1.0;
                exp = 0.0;
            } else if (c == 'e') {
                readExp = true;
            } else {
                throw new RuntimeException("Unknown char " + c);
            }

        }
        value = value * pref;

        if (size == doubles.length) {
            doubles = Arrays.copyOf(doubles, size * 2);
        }

        doubles[size] = value;
        size++;

        return Arrays.copyOf(doubles, size);
    }

    public static void loadEmbeddings(List<String> paths) throws IOException {
        for(String path : paths) {
            loadEmbs(path);
        }
    }



    public static INDArray get(String e1) {
        if (!embs1.containsKey(e1)) {
            return Nd4j.zeros(DataType.DOUBLE, embshape);
        }
        return embs1.get(e1);
    }

    public static INDArray embLabels(Collection<String> labels) {
        List<INDArray> cqaLabelsEmbs = labels.stream().map(EmbeddingManager::get).toList();
        return Nd4j.vstack(cqaLabelsEmbs).mean(0);
    }


}
