package irit.labelmap;

import java.util.ArrayList;
import java.util.List;

public class Tree<V> {

    final V value;
    final List<Tree<V>> children;
    final Tree<V> parent;
    final int depth;
    final int direction;

    public Tree(V value, Tree<V> parent, int depth) {
        this.value = value;
        this.parent = parent;
        children = new ArrayList<>();
        this.depth = depth;
        direction = 0;
    }

    public Tree(V value, Tree<V> parent, int depth, int direction) {
        this.value = value;
        this.parent = parent;
        children = new ArrayList<>();
        this.depth = depth;
        this.direction = direction;
    }

    public V getValue() {
        return value;
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

    public int getDirection() {
        return direction;
    }
}


