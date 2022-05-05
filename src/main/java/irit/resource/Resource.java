package irit.resource;

import irit.dataset.DatasetManager;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Resource {

    protected final String value;
    protected final HashSet<IRI> similarIRIs;
    private final Pattern pattern = Pattern.compile("[a-z][/:#]");

    public Resource(String val) {
        Pattern inp = Pattern.compile("\\\\");
        value = inp.matcher(val).replaceAll("");
        similarIRIs = new HashSet<>();
    }

    public boolean isIRI() {
        Matcher matcher = pattern.matcher(value);
        return !value.contains(" ") && matcher.find();
    }


    public static String join(String del, Iterable<String> data) {

        Iterator<String> iterator = data.iterator();
        if (!iterator.hasNext()) return "";

        StringBuilder pref = new StringBuilder(iterator.next());

        while (iterator.hasNext()) {
            pref.append(del).append(iterator.next());
        }

        return pref.toString();
    }

    public void findSimilarResource(String targetEndpoint) {
        Set<String> values = new HashSet<>();
        values.add(value);
        values.add("\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"@en");
        values.add("\"" + value.substring(0, 1).toUpperCase() + value.substring(1) + "\"");


        Set<String> nml = new HashSet<>();

        for (String label : values) {
            nml.addAll(DatasetManager.getInstance().labelMaps.get(targetEndpoint).getSimilar(label.toLowerCase()));
        }


        for (String s : nml) {
            similarIRIs.add(new IRI("<" + s + ">"));
        }

    }

    public String toValueString() {
        if (!(isIRI())) {
            return "\"" + value + "\"";
        } else {
            return toString();
        }
    }

    public String toString() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Resource) {
            return value.equals(((Resource) obj).value);
        } else {
            return false;
        }
    }

    public HashSet<IRI> getSimilarIRIs() {
        return similarIRIs;
    }


    public String getValue() {
        return value;
    }
}
