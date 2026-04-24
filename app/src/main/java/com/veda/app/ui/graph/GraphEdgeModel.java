package com.veda.app.ui.graph;

public class GraphEdgeModel {
    public String id;
    public long from;
    public long to;
    public boolean directed;

    public GraphEdgeModel(String id, long from, long to, boolean directed) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.directed = directed;
    }
}
