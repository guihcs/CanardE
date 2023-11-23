package determinism;

import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EmbeddingManagerTest {

    @Test
    public void readEmbeddings() throws IOException {




//        List<String> embs = Files.readAllLines(Paths.get(e1));
//
//        Map<String, INDArray> embsMap = new HashMap<>();
//
//        for (int i = 0; i < ents.size(); i++) {
//            String[] split = embs.get(i).split(", ");
//            double[] de = new double[split.length];
//            for (int j = 0; j < split.length; j++) {
//                de[j] = Double.parseDouble(split[j]);
//            }
//            INDArray indArray = Nd4j.create(de);
//            embsMap.put(ents.get(i), indArray);
//        }
//        return embsMap;
    }
}
