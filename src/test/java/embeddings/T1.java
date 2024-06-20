package embeddings;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class T1 {


    @Test
    public void t1() throws OWLOntologyCreationException, IOException {
        String ontologyPath = "/home/guilherme/Documents/kg/complex/geolink/gbo.owl";

        OWLOntologyManager om = OWLManager.createOWLOntologyManager();
        OWLOntologyDocumentSource source = new StreamDocumentSource(Files.newInputStream(Paths.get(ontologyPath)));
        om.loadOntologyFromOntologyDocument(source);
        OWLOntology pressInnovOntology = om.createOntology();




    }
}
