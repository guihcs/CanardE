package irit.complex.subgraphs;

import irit.resource.IRI;
import irit.resource.Resource;
import irit.similarity.EmbeddingManager;
import irit.sparql.query.exception.SparqlEndpointUnreachableException;
import irit.sparql.query.exception.SparqlQueryMalFormedException;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.HashSet;

public class Triple extends InstantiatedSubgraph {
    private final IRI subject;
    private final IRI predicate;
    private final Resource object;
    public boolean keepObjectType;
    public boolean keepSubjectType;
    private TripleType tripleType;
    private IRI subjectType;
    private IRI objectType;
    private double objectSimilarity;
    private double subjectSimilarity;
    private double predicateSimilarity;

    public Triple() {
        subject = new IRI("");
        object = new Resource("");
        predicate = new IRI("");
    }

    public Triple(String sub, String pred, String obj, TripleType type) {
        subject = new IRI(sub);
        predicate = new IRI(pred);
        Resource r = new Resource(obj);
        if (r.isIRI()) {
            object = new IRI("<" + obj.replaceAll("[<>]", "") + ">");
        } else {
            object = r;
        }
        tripleType = type;
        boolean visited = false;
        keepObjectType = false;
        keepSubjectType = false;
        objectSimilarity = 0;
        subjectSimilarity = 0;
        predicateSimilarity = 0;
    }

    public void retrieveIRILabels(String targetEndpoint) {
        if (tripleType != TripleType.SUBJECT) {
            subject.retrieveLabels(targetEndpoint);
        }
        if (tripleType != TripleType.PREDICATE) {
            predicate.retrieveLabels(targetEndpoint);
        }
        if (tripleType != TripleType.OBJECT && object instanceof IRI) {
            ((IRI) object).retrieveLabels(targetEndpoint);
        }
    }

    public void retrieveTypes(String targetEndpoint) throws SparqlQueryMalFormedException, SparqlEndpointUnreachableException {
        if (tripleType != TripleType.SUBJECT) {
            subject.retrieveTypes(targetEndpoint);
        }
        if (tripleType != TripleType.PREDICATE) {
            predicate.retrieveTypes(targetEndpoint);
        }
        if (tripleType != TripleType.OBJECT && object instanceof IRI) {
            ((IRI) object).retrieveTypes(targetEndpoint);
        }
    }


    public double compareSim(INDArray label, double threshold) {
        if (tripleType != TripleType.SUBJECT) {
            subjectSimilarity = Transforms.cosineSim(EmbeddingManager.get(subject.toString().replaceAll("[<>]", "")), label);
            subjectSimilarity = subjectSimilarity >= threshold ? subjectSimilarity : 0;
        }
        if (tripleType != TripleType.PREDICATE && !predicate.toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            predicateSimilarity = Transforms.cosineSim(EmbeddingManager.get(predicate.toString().replaceAll("[<>]", "")), label);
            predicateSimilarity = predicateSimilarity >= threshold ? predicateSimilarity : 0;
        }
        if (tripleType != TripleType.OBJECT) {
            objectSimilarity = Transforms.cosineSim(EmbeddingManager.get(object.toString().replaceAll("[<>]", "")), label);
            objectSimilarity = objectSimilarity >= threshold ? objectSimilarity : 0;
        }

        return subjectSimilarity + predicateSimilarity + objectSimilarity;
    }

    public double compareLabel(HashSet<String> targetLabels, double threshold, String targetEndpoint) {
        if (tripleType != TripleType.SUBJECT) {
            subjectType = subject.findMostSimilarType(targetEndpoint, targetLabels, threshold);
            double scoreTypeSubMax = 0;
            if (subjectType != null) {
                scoreTypeSubMax = IRI.similarity(subjectType.getLabels(), targetLabels, threshold);
            }
            subjectSimilarity = IRI.similarity(subject.getLabels(), targetLabels, threshold);
            if (scoreTypeSubMax > subjectSimilarity) {
                keepSubjectType = true;
                subjectSimilarity = scoreTypeSubMax;
            }
        }
        if (tripleType != TripleType.PREDICATE && !predicate.toString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
            predicateSimilarity = IRI.similarity(predicate.getLabels(), targetLabels, threshold);
        }
        if (tripleType != TripleType.OBJECT && object instanceof IRI) {
            objectType = ((IRI) object).findMostSimilarType(targetEndpoint, targetLabels, threshold);
            if (objectType != null) {
                double scoreTypeObMax = IRI.similarity(objectType.getLabels(), targetLabels, threshold);
                objectSimilarity = IRI.similarity(((IRI) object).getLabels(), targetLabels, threshold);
                if (scoreTypeObMax > objectSimilarity) {
                    keepObjectType = true;
                    objectSimilarity = scoreTypeObMax;
                }
            }

        } else if (tripleType != TripleType.OBJECT) {
            HashSet<String> hashObj = new HashSet<>();
            hashObj.add(object.toString());
            objectSimilarity = IRI.similarity(hashObj, targetLabels, threshold);
        }


        return subjectSimilarity + predicateSimilarity + objectSimilarity;
    }

    public IRI getSubject() {
        return subject;
    }

    public IRI getPredicate() {
        return predicate;
    }

    public Resource getObject() {
        return object;
    }

    public boolean isSubjectTriple() {
        return tripleType == TripleType.SUBJECT;
    }

    public boolean isPredicateTriple() {
        return tripleType == TripleType.PREDICATE;
    }

    public boolean isObjectTriple() {
        return tripleType == TripleType.OBJECT;
    }

    public String toString() {
        String subjStr = subject.toValueString();
        String predStr = predicate.toValueString();
        String objStr = object.toValueString();

        if (isSubjectTriple()) {
            subjStr = "?answer";
        } else if (isPredicateTriple()) {
            predStr = "?answer";
        } else if (isObjectTriple()) {
            objStr = "?answer";
        }

        String result = subjStr + " " + predStr + " " + objStr + ". ";
        if (keepSubjectType && !keepObjectType) {
            result = "?x " + predStr + " " + objStr + ". " +
                    "?x a " + subjectType + ". ";
        } else if (keepObjectType && !keepSubjectType) {
            result = subjStr + " " + predStr + " ?y. " +
                    "?y a " + objectType + ". ";
        } else if (keepObjectType) {
            result = "?x " + predStr + " ?y. " +
                    "?y a " + objectType + ". " +
                    "?x a " + subjectType + ". ";
        }
        return result;
    }


    public TripleType commonPartValue(Triple t) {
        TripleType res = TripleType.NONE;
        if (getType() == t.getType()) {
            if (getPredicate().equals(t.getPredicate()) && !isPredicateTriple()) {
                res = TripleType.PREDICATE;
            }
            if (getObject().equals(t.getObject()) && !isObjectTriple() && !keepObjectType) {
                res = TripleType.OBJECT;
            }
            if (getSubject().equals(t.getSubject()) && !isSubjectTriple() && !keepSubjectType) {
                res = TripleType.SUBJECT;
            }
        }
        return res;
    }

    public boolean hasCommonPart(Triple t) {
        boolean res = false;
        if (getType() == t.getType()) {
            if (!isSubjectTriple()) {
                res = getSubject().equals(t.getSubject());
            }
            if (!isPredicateTriple()) {
                res = res || getPredicate().equals(t.getPredicate());
            }
            if (!isObjectTriple()) {
                res = res || getObject().equals(t.getObject());
            }
        }
        return res;
    }


    public TripleType getType() {
        return tripleType;
    }

    public int hashCode() {
        return (subject.toString() + predicate.toString() + object.toString()).hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Triple) {
            return (subject.toString() + predicate.toString() + object.toString())
                    .equals(((Triple) obj).subject.toString() + ((Triple) obj).predicate.toString() + ((Triple) obj).object.toString());
        } else {
            return false;
        }

    }

    public boolean isNullTriple() {
        return subject.toString().equals("") && predicate.toString().equals("") && object.toString().equals("");
    }


    public double getSimilarity() {
        return subjectSimilarity + predicateSimilarity + objectSimilarity;
    }


    public TripleType getPartGivingMaxSimilarity() {
        TripleType res = TripleType.NONE;
        if (subjectSimilarity > objectSimilarity && subjectSimilarity > predicateSimilarity) {
            res = TripleType.SUBJECT;
        } else if (objectSimilarity > subjectSimilarity && objectSimilarity > predicateSimilarity) {
            res = TripleType.OBJECT;
        } else if (predicateSimilarity > subjectSimilarity && predicateSimilarity > objectSimilarity) {
            res = TripleType.PREDICATE;
        }
        return res;

    }
}
