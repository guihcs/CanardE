package irit.similarity;

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

public class EmbeddingManager {


    private static Map<String, INDArray> embs1 = new HashMap<>();
    public static long[] embshape;

    public static void load(String n1, String e1) throws IOException {

        Map<String, INDArray> embs = loadEmbs(n1, e1);

        Optional<INDArray> first = embs.values().stream().findFirst();
        embshape = first.get().shape();

        embs1.putAll(embs);
    }


    public static double getSim(String s1, String s2){

        INDArray n1 = embs1.get(s1);
        INDArray n2 = embs1.get(s2);

        if (n1 == null){
            n1 = Nd4j.zeros(DataType.DOUBLE, embshape);
        }

        if (n2 == null){
            n2 = Nd4j.zeros(DataType.DOUBLE, embshape);
        }

        return Transforms.cosineSim(n1, n2);
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

    public static INDArray get(String e1){
        if (!embs1.containsKey(e1)) return Nd4j.zeros(DataType.DOUBLE, embshape);
        return embs1.get(e1);
    }


    private static String processLabel(String line){
        line = line.replaceAll("\\\\n", "\\n").trim();
        if (line.startsWith("http://") && line.contains("#")){
            String[] split = line.split("#");
            if (split.length > 1){
                line = split[1];
            } else {
                line = split[0];
            }
        }
        return line;
    }

}
