package com.veda.app.ui.graph;

public class GraphConfig {

    public GraphForcePreset forcePreset = GraphForcePreset.BALANCED;

    public boolean showLabels = true;
    public boolean scaleNodesByDegree = true;
    public boolean useGroupColors = true;
    public boolean fadeEdges = false;

    public float repulsion = -4400f;
    public float centralGravity = 0.06f;
    public int springLength = 185;
    public float springStrength = 0.020f;
    public float damping = 0.28f;
    public float overlap = 0.34f;
    public int stabilizationIterations = 1000;

    public GraphConfig() {
        applyPreset(forcePreset);
    }

    public static GraphConfig defaults() {
        return new GraphConfig();
    }

    public static GraphConfig fromPreset(GraphForcePreset preset) {
        return new GraphConfig().applyPreset(preset);
    }

    public GraphConfig applyPreset(GraphForcePreset preset) {
        forcePreset = preset == null ? GraphForcePreset.BALANCED : preset;

        switch (forcePreset) {
            case COMPACT:
                repulsion = -3000f;
                centralGravity = 0.09f;
                springLength = 145;
                springStrength = 0.028f;
                damping = 0.30f;
                overlap = 0.26f;
                stabilizationIterations = 700;
                break;

            case SPACIOUS:
                repulsion = -6200f;
                centralGravity = 0.035f;
                springLength = 235;
                springStrength = 0.016f;
                damping = 0.25f;
                overlap = 0.42f;
                stabilizationIterations = 1300;
                break;

            case BALANCED:
            default:
                repulsion = -4400f;
                centralGravity = 0.06f;
                springLength = 185;
                springStrength = 0.020f;
                damping = 0.28f;
                overlap = 0.34f;
                stabilizationIterations = 1000;
                break;
        }

        return this;
    }
}