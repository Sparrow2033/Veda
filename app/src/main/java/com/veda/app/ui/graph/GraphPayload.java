package com.veda.app.ui.graph;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GraphPayload {

    public int version = 1;
    public GraphMode mode = GraphMode.GLOBAL;

    @Nullable
    public Long focusNoteId;

    public int localDepth = 1;

    public GraphConfig config = GraphConfig.defaults();
    public List<GraphNodeModel> nodes = new ArrayList<>();
    public List<GraphEdgeModel> edges = new ArrayList<>();
}