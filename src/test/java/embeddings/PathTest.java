package embeddings;

import irit.dataset.DatasetManager;
import irit.labelmap.PathResult;
import irit.similarity.EmbeddingManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class PathTest {


    @Test
    public void testRead() throws IOException {
        DatasetManager.getInstance().load("agrotaxon", "/home/guilherme/Downloads/taxon/ont/agrotaxon.owl");
        DatasetManager.getInstance().load("agrovoc", "/home/guilherme/Downloads/taxon/ont/agrovoc.owl");

        var lb = DatasetManager.getInstance().labelMaps.get("agrovoc");

        PathResult result = DatasetManager.getInstance().labelMaps.get("agrovoc").pathBetween("http://aims.fao.org/aos/agrovoc/c_7957", "http://aims.fao.org/aos/agrovoc/c_7951", 5, true);

        System.out.println(result.getMap());

        DatasetManager.getInstance().close();


    }


}
