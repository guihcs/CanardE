package irit.labelmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PathResult {

    private List<Map<String, String>> map;
    private List<Integer> finalDirections;



    public PathResult(){
        map = new ArrayList<>();
        finalDirections = new ArrayList<>();
    }
    public PathResult(List<Map<String, String>> map, List<Integer> finalDirections) {
        this.map = map;
        this.finalDirections = finalDirections;
    }



    public List<Map<String, String>> getMap() {
        return map;
    }

    public List<Integer> getFinalDirections() {
        return finalDirections;
    }

    public List<Boolean> getInverseList(){
        List<Boolean> result = new ArrayList<>();
        for(int i = 0; i<finalDirections.size(); i++){
            result.add(finalDirections.get(i) == 1);
        }
        return result;
    }
}
