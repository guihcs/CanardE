package irit.similarity;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbeddingManager {


    private static Map<String, INDArray> embs1 = new HashMap<>();
    public static long[] embshape;
    private static final Pattern pattern = Pattern.compile("([^>]+)[#/]([A-Za-z0-9_-]+)");

    public static void load(String n1, String e1) throws IOException {

        Map<String, INDArray> embs = loadEmbs(n1, e1);

        Optional<INDArray> first = embs.values().stream().findFirst();
        embshape = first.get().shape();

        embs1.putAll(embs);
    }


    public static double getSim(String s1, String s2) {
        s1 = getSuffix(s1).toLowerCase();
        s2 = getSuffix(s2).toLowerCase();
        return 1 - LevenshteinDistance.getDefaultInstance().apply(s1, s2) / (float) Math.max(s1.length(), s2.length());
    }

    private static String getSuffix(String value) {

        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            return matcher.group(2);
        } else {
            return value;
        }
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
