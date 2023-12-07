package determinism;

import irit.complex.answer.SingleAnswer;
import irit.dataset.DatasetManager;
import irit.resource.IRI;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeterminismTest {
    private static final String sourceName = "cmt_100";
    private static final String targetName = "conference_100";

    private static final String source = "/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/" + sourceName + ".ttl";
    private static final String target = "/home/guilherme/IdeaProjects/conference-dataset-population-elodie/populated_datasets/data_100/" + targetName + ".ttl";


    @BeforeAll
    public static void setup() throws IOException {
        DatasetManager.getInstance().load(sourceName, source);
        DatasetManager.getInstance().load(targetName, target);
    }

    @AfterAll
    public static void close() {
        DatasetManager.getInstance().close();
    }

    private static Stream<Arguments> provideTest0() {
        return Stream.of(
                Arguments.of("http://cmt-instances#paper-10140749615101114",
                        List.of("5101114", "the influence of target backing on in-beam electron spectra", "paper-10140749615101114"),
                        List.of("<http://conference-instances#abstr-10140749615101114>", "<http://conference-instances#paper-10140749615101114>")),

                Arguments.of("http://cmt-instances#paper-101412517078257",
                        List.of("78257", "paper-101412517078257", "reactive concurrent programming revisited"),
                        List.of("<http://conference-instances#paper-101412517078257>", "<http://conference-instances#abstr-101412517078257>")),

                Arguments.of("http://cmt-instances#paper-10197076583581114",
                        List.of("lattice artefacts in su(3) lattice gauge theory with a mixed fundamental and adjoint plaquette action.", "3581114", "paper-10197076583581114"),
                        List.of()),

                Arguments.of("http://cmt-instances#paper-10279070292502198",
                        List.of("paper-10279070292502198", "angular distributions in the dimuon hadronic production at 150 gev/c", "2502198"),
                        List.of("<http://conference-instances#paper-10279070292502198>", "<http://conference-instances#abstr-10279070292502198>")),

                Arguments.of("http://cmt-instances#paper-10318174732262198",
                        List.of("paper-10318174732262198", "low-spin states of doubly odd ^{182}au", "2262198"),
                        List.of()),

                Arguments.of("http://cmt-instances#paper-10383822314262198",
                        List.of("4262198", "paper-10383822314262198", "news from hellaz - (dedicated to tom ypsilantis)"),
                        List.of("<http://conference-instances#abstr-10383822314262198>", "<http://conference-instances#paper-10383822314262198>")),

                Arguments.of("http://cmt-instances#paper-10421966453972198",
                        List.of("paper-10421966453972198", "observation of semileptonic decays of charmed baryons", "3972198"),
                        List.of("<http://conference-instances#paper-10421966453972198>", "<http://conference-instances#abstr-10421966453972198>")),

                Arguments.of("http://cmt-instances#paper-10471626671281114",
                        List.of("1281114", "excitation-assisted inelastic processes in trapped bose-einstein condensates", "paper-10471626671281114"),
                        List.of("<http://conference-instances#paper-10471626671281114>", "<http://conference-instances#poster-10471626671281114>", "<http://conference-instances#abstr-10471626671281114>")),

                Arguments.of("http://cmt-instances#paper-1061898766171114",
                        List.of("polymer-based micro deformable mirror for adaptive optics applications", "171114", "paper-1061898766171114"),
                        List.of("<http://conference-instances#poster-1061898766171114>", "<http://conference-instances#paper-1061898766171114>", "<http://conference-instances#abstr-1061898766171114>")),

                Arguments.of("http://cmt-instances#paper-10621745376421114",
                        List.of("evidence for the metal-insulator transition in a pure 3d metal", "6421114", "paper-10621745376421114"),
                        List.of("<http://conference-instances#abstr-10621745376421114>", "<http://conference-instances#paper-10621745376421114>")),

                Arguments.of("http://cmt-instances#paper-10651508131301263",
                        List.of("paper-10651508131301263", "transfer results for odd-odd ^{196}au and the extended supersymmetry", "1301263"),
                        List.of()),

                Arguments.of("http://cmt-instances#paper-10660131037981114",
                        List.of("placers of cosmic dust in the blue ice lakes of greenland", "paper-10660131037981114", "7981114"),
                        List.of("<http://conference-instances#abstr-10660131037981114>", "<http://conference-instances#paper-10660131037981114>")),

                Arguments.of("http://cmt-instances#paper-10723706712872198",
                        List.of("paper-10723706712872198", "radiopolarography of mendelevium in aqueous solutions", "2872198"),
                        List.of("<http://conference-instances#abstr-10723706712872198>", "<http://conference-instances#paper-10723706712872198>")),

                Arguments.of("http://cmt-instances#paper-1074964566151263",
                        List.of("relativistic shell models and meson-exchange currents", "151263", "paper-1074964566151263"),
                        List.of("<http://conference-instances#paper-1074964566151263>", "<http://conference-instances#abstr-1074964566151263>"))

        );
    }

    @ParameterizedTest
    @MethodSource("provideTest0")
    public void test0(String si, List<String> labs, List<String> sim) {

        IRI iri = new IRI(si);
        SingleAnswer singleAnswer = new SingleAnswer(iri);

        singleAnswer.retrieveIRILabels(sourceName);

        IRI ri = (IRI) singleAnswer.res;
        Assertions.assertEquals(labs.size(), ri.labels.size());
        Assertions.assertTrue(ri.labels.containsAll(labs));

        singleAnswer.getSimilarIRIs(targetName);
        Assertions.assertEquals(sim.size(), singleAnswer.res.similarIRIs.size());
        Set<String> collect = singleAnswer.res.similarIRIs.stream().map(IRI::toString).collect(Collectors.toSet());
        Assertions.assertTrue(collect.containsAll(sim));
    }





}
