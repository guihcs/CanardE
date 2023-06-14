package irit.labelmap;

import java.util.ArrayList;
import java.util.List;

public class Tree<V> {

    V value;
    List<Tree<V>> children;
    Tree<V> parent;
    int depth;

    public Tree(V value, Tree<V> parent, int depth) {
        this.value = value;
        this.parent = parent;
        children = new ArrayList<>();
        this.depth = depth;
    }

    public V getValue() {
        return value;
    }

    public List<Tree<V>> getChildren() {
        return children;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public void setChildren(List<Tree<V>> children) {
        this.children = children;
    }

    public void addChild(Tree<V> child) {
        children.add(child);
    }

    public int getDepth() {
        return depth;
    }

    public Tree<V> getParent() {
        return parent;
    }
}


