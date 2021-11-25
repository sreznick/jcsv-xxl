package ru.study21.jcsv.xxl.algorithms;

import java.util.*;

public class PairingHeap<T> {

    private class PairingNode {
        private T elem;
        private List<PairingNode> children;

        public PairingNode() {
            elem = null;
            children = new ArrayList<>(); // LinkedList-s provide similar performance
        }

        public PairingNode(PairingNode other) {
            this.elem = other.elem;
            this.children = other.children;
        }

        // meld `other` into `this` and MAYBE destroy `other`
        public void meldNodes(PairingNode other) {
            if (other.elem == null) {
                // do nothing
            } else if (elem == null) {
                elem = other.elem;
                children = other.children;
            } else if (comp.compare(elem, other.elem) <= 0) {
                children.add(other);
            } else {
                other.children.add(new PairingNode(this));
                elem = other.elem;
                children = other.children;
                other.elem = null;
                other.children = null;
            }
        }
    }

    private final Comparator<? super T> comp;
    private PairingNode root;

    public PairingHeap(Comparator<? super T> comp) {
        this.comp = comp;
        root = new PairingNode();
    }

    private PairingHeap(Comparator<? super T> comp, T elem) {
        this(comp);
        root.elem = elem;
    }

    public boolean isEmpty() {
        return root.elem == null;
    }

    public void clear() {
        root = new PairingNode();
    }

    public T peekMin() {
        return root.elem;
    }

    // `this` becomes melded heap, accessing `other` after calling this method will cause NPE
    private void meld(PairingHeap<T> other) {
        root.meldNodes(other.root);
        other.root = null;
    }

    public void add(T t) {
        meld(new PairingHeap<T>(comp, t));
    }

    public void addAll(Iterable<T> c) {
        for (T t : c) {
            add(t);
        }
    }

    public T pollMin() {
        T result = root.elem;
        Iterator<PairingNode> iter = root.children.iterator();
        if (iter.hasNext()) {
            root = iter.next();
            while (iter.hasNext()) {
                root.meldNodes(iter.next());
            }
        } else {
            root = new PairingNode();
        }
        return result;
    }

}
