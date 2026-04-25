package com.veda.app.ui.graph;

import android.app.Application;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphViewModel extends AndroidViewModel {
    public enum ScreenState {
        LOADING,
        CONTENT,
        EMPTY,
        ERROR
    }

    private static final Pattern INTERNAL_NOTE_LINK_PATTERN =
            Pattern.compile("veda://note/(\\d+)", Pattern.CASE_INSENSITIVE);

    private final MutableLiveData<String> payloadJson = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> statusText = new MutableLiveData<>("Загрузка графа...");
    private final MutableLiveData<ScreenState> screenState = new MutableLiveData<>(ScreenState.LOADING);
    private final MutableLiveData<GraphMode> graphMode = new MutableLiveData<>(GraphMode.GLOBAL);
    private final MutableLiveData<Long> focusNoteId = new MutableLiveData<>(-1L);
    private final MutableLiveData<Integer> localDepth = new MutableLiveData<>(2);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile int loadVersion = 0;
    private volatile Object repositoryRef;

    public GraphViewModel(@NonNull Application application) {
        super(application);
        reload();
    }

    public LiveData<String> getPayloadJson() {
        return payloadJson;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getStatusText() {
        return statusText;
    }

    public LiveData<ScreenState> getScreenState() {
        return screenState;
    }

    public LiveData<GraphMode> getGraphMode() {
        return graphMode;
    }

    public LiveData<Long> getFocusNoteId() {
        return focusNoteId;
    }

    public LiveData<Integer> getLocalDepth() {
        return localDepth;
    }

    public void reload() {
        final int requestVersion = ++loadVersion;
        final GraphMode requestedMode = graphMode.getValue() != null ? graphMode.getValue() : GraphMode.GLOBAL;
        final long requestedFocus = focusNoteId.getValue() != null ? focusNoteId.getValue() : -1L;
        final int requestedDepth = clampDepth(localDepth.getValue());

        postLoading(true);
        mainHandler.post(() -> screenState.setValue(ScreenState.LOADING));

        executor.execute(() -> {
            try {
                GraphSnapshot snapshot = loadSnapshot();
                UiGraphResult result = buildUiGraph(snapshot, requestedMode, requestedFocus, requestedDepth);

                mainHandler.post(() -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    graphMode.setValue(result.mode);
                    focusNoteId.setValue(result.focusId);
                    localDepth.setValue(result.depth);
                    payloadJson.setValue(result.payloadJson);
                    statusText.setValue(result.status);
                    loading.setValue(false);
                    screenState.setValue(result.nodeCount > 0 ? ScreenState.CONTENT : ScreenState.EMPTY);
                });
            } catch (Throwable throwable) {
                final String fallbackPayload = buildEmptyPayloadJson();
                mainHandler.post(() -> {
                    if (requestVersion != loadVersion) {
                        return;
                    }
                    payloadJson.setValue(fallbackPayload);
                    statusText.setValue("Граф не удалось загрузить");
                    loading.setValue(false);
                    screenState.setValue(ScreenState.ERROR);
                });
            }
        });
    }

    public void showGlobalGraph() {
        graphMode.setValue(GraphMode.GLOBAL);
        reload();
    }

    public void showLocalGraph(long noteId) {
        if (noteId <= 0L) {
            return;
        }
        focusNoteId.setValue(noteId);
        graphMode.setValue(GraphMode.LOCAL);
        reload();
    }

    public void setLocalDepth(int depth) {
        int clamped = clampDepth(depth);
        Integer current = localDepth.getValue();
        if (current != null && current == clamped) {
            return;
        }
        localDepth.setValue(clamped);
        if (graphMode.getValue() == GraphMode.LOCAL) {
            reload();
        }
    }

    private int clampDepth(Integer depth) {
        if (depth == null) {
            return 2;
        }
        if (depth < 1) {
            return 1;
        }
        if (depth > 3) {
            return 3;
        }
        return depth;
    }

    private void postLoading(boolean value) {
        mainHandler.post(() -> loading.setValue(value));
    }

    private UiGraphResult buildUiGraph(GraphSnapshot snapshot,
                                       GraphMode requestedMode,
                                       long requestedFocusId,
                                       int requestedDepth) throws Exception {

        GraphMode resolvedMode = requestedMode;
        long resolvedFocusId = requestedFocusId;

        Map<Long, RawNode> nodeMap = new LinkedHashMap<>();
        for (RawNode node : snapshot.nodes) {
            nodeMap.put(node.id, node);
        }

        Map<Long, Set<Long>> adjacency = new HashMap<>();
        for (RawEdge edge : snapshot.edges) {
            if (!nodeMap.containsKey(edge.fromId) || !nodeMap.containsKey(edge.toId)) {
                continue;
            }
            adjacency.computeIfAbsent(edge.fromId, key -> new LinkedHashSet<>()).add(edge.toId);
            adjacency.computeIfAbsent(edge.toId, key -> new LinkedHashSet<>()).add(edge.fromId);
        }

        Set<Long> visibleIds;
        if (requestedMode == GraphMode.LOCAL && requestedFocusId > 0L && nodeMap.containsKey(requestedFocusId)) {
            visibleIds = bfsVisibleIds(requestedFocusId, requestedDepth, adjacency);
            if (!visibleIds.contains(requestedFocusId)) {
                visibleIds.add(requestedFocusId);
            }
        } else {
            resolvedMode = GraphMode.GLOBAL;
            resolvedFocusId = requestedFocusId > 0L ? requestedFocusId : -1L;
            visibleIds = new LinkedHashSet<>(nodeMap.keySet());
        }

        List<RawEdge> visibleEdges = new ArrayList<>();
        Map<Long, Integer> degreeMap = new HashMap<>();

        for (RawEdge edge : snapshot.edges) {
            if (!visibleIds.contains(edge.fromId) || !visibleIds.contains(edge.toId)) {
                continue;
            }
            visibleEdges.add(edge);
            degreeMap.put(edge.fromId, degreeMap.getOrDefault(edge.fromId, 0) + 1);
            degreeMap.put(edge.toId, degreeMap.getOrDefault(edge.toId, 0) + 1);
        }

        List<RawNode> visibleNodes = new ArrayList<>();
        for (Long nodeId : visibleIds) {
            RawNode node = nodeMap.get(nodeId);
            if (node != null) {
                visibleNodes.add(node);
            }
        }

        Collections.sort(visibleNodes, new Comparator<RawNode>() {
            @Override
            public int compare(RawNode left, RawNode right) {
                int degreeCompare = Integer.compare(
                        degreeMap.getOrDefault(right.id, 0),
                        degreeMap.getOrDefault(left.id, 0)
                );
                if (degreeCompare != 0) {
                    return degreeCompare;
                }
                return safeNodeLabel(left).compareToIgnoreCase(safeNodeLabel(right));
            }
        });

        LinkedHashMap<String, GroupStyle> groupStyles = buildGroupStyles(visibleNodes);
        GraphForcePreset requestedForcePreset = GraphForcePreset.BALANCED;
        GraphConfig config = GraphConfig.fromPreset(requestedForcePreset);

        JSONArray subjectsJson = new JSONArray();
        JSONArray groupsJson = new JSONArray();
        Set<String> addedGroups = new LinkedHashSet<>();

        JSONArray nodesJson = new JSONArray();
        for (RawNode node : visibleNodes) {
            int degree = degreeMap.getOrDefault(node.id, 0);
            boolean isFocus = resolvedMode == GraphMode.LOCAL && node.id == resolvedFocusId;

            String groupKey = resolveGroupKey(node.subject);
            GroupStyle style = groupStyles.get(groupKey);
            if (style == null) {
                style = fallbackGroupStyle(groupKey, 0);
            }

            if (addedGroups.add(groupKey)) {
                JSONObject groupItem = new JSONObject();
                groupItem.put("key", groupKey);
                groupItem.put("label", style.label);
                groupItem.put("color", style.nodeColor);
                groupsJson.put(groupItem);
                if (!"other".equals(groupKey)) {
                    subjectsJson.put(style.label);
                }
            }

            String borderColor = isFocus ? "#F5C451" : style.borderColor;
            int value = Math.max(16, Math.min(36, 16 + degree * 2 + (isFocus ? 4 : 0)));

            JSONObject item = new JSONObject();
            item.put("id", node.id);
            item.put("label", safeNodeLabel(node));
            item.put("title", buildTooltip(node, degree));
            item.put("subject", safe(node.subject));
            item.put("groupKey", groupKey);
            item.put("groupLabel", style.label);
            item.put("degree", degree);
            item.put("value", value);
            item.put("backgroundColor", style.nodeColor);
            item.put("borderColor", borderColor);
            item.put("fontColor", style.fontColor);
            item.put("borderWidth", isFocus ? 3 : 2);
            item.put("isFocus", isFocus);
            item.put("seedX", style.seedX);
            item.put("seedY", style.seedY);
            nodesJson.put(item);
        }

        JSONArray edgesJson = new JSONArray();
        for (RawEdge edge : visibleEdges) {
            JSONObject item = new JSONObject();
            item.put("id", edge.fromId + "_" + edge.toId);
            item.put("from", edge.fromId);
            item.put("to", edge.toId);
            edgesJson.put(item);
        }

        JSONObject filtersJson = new JSONObject();
        filtersJson.put("subjects", subjectsJson);
        filtersJson.put("groups", groupsJson);

        JSONObject forceJson = new JSONObject();
        forceJson.put("preset", requestedForcePreset.name());
        if (requestedForcePreset == GraphForcePreset.COMPACT) {
            forceJson.put("groupRadius", 250);
            forceJson.put("nodeSpread", 95);
            forceJson.put("springLength", 125);
            forceJson.put("gravity", -70);
        } else if (requestedForcePreset == GraphForcePreset.SPACIOUS) {
            forceJson.put("groupRadius", 470);
            forceJson.put("nodeSpread", 190);
            forceJson.put("springLength", 215);
            forceJson.put("gravity", -110);
        } else {
            forceJson.put("groupRadius", 360);
            forceJson.put("nodeSpread", 140);
            forceJson.put("springLength", 165);
            forceJson.put("gravity", -90);
        }

        JSONObject configJson = new JSONObject();
        configJson.put("mode", resolvedMode.name());
        configJson.put("focusNoteId", resolvedFocusId);
        configJson.put("localDepth", requestedDepth);
        configJson.put("nodeCount", visibleNodes.size());
        configJson.put("edgeCount", visibleEdges.size());
        configJson.put("showLabels", config.showLabels);
        configJson.put("scaleNodesByDegree", config.scaleNodesByDegree);
        configJson.put("useGroupColors", config.useGroupColors);
        configJson.put("fadeEdges", config.fadeEdges);
        configJson.put("forcePreset", config.forcePreset.name());
        configJson.put("repulsion", config.repulsion);
        configJson.put("centralGravity", config.centralGravity);
        configJson.put("springLength", config.springLength);
        configJson.put("springStrength", config.springStrength);
        configJson.put("damping", config.damping);
        configJson.put("overlap", config.overlap);
        configJson.put("stabilizationIterations", config.stabilizationIterations);
        configJson.put("force", forceJson);

        JSONObject payload = new JSONObject();
        payload.put("config", configJson);
        payload.put("filters", filtersJson);
        payload.put("nodes", nodesJson);
        payload.put("edges", edgesJson);

        String status;
        if (resolvedMode == GraphMode.LOCAL && resolvedFocusId > 0L) {
            status = String.format(
                    Locale.getDefault(),
                    "Локальный граф · глубина: %d · узлов: %d · связей: %d",
                    requestedDepth,
                    visibleNodes.size(),
                    visibleEdges.size()
            );
        } else {
            status = String.format(
                    Locale.getDefault(),
                    "Глобальный граф · узлов: %d · связей: %d",
                    visibleNodes.size(),
                    visibleEdges.size()
            );
        }

        return new UiGraphResult(
                payload.toString(),
                status,
                resolvedMode,
                resolvedFocusId,
                requestedDepth,
                visibleNodes.size()
        );
    }

    private LinkedHashMap<String, GroupStyle> buildGroupStyles(List<RawNode> nodes) {
        LinkedHashMap<String, String> groupLabels = new LinkedHashMap<>();
        for (RawNode node : nodes) {
            String key = resolveGroupKey(node.subject);
            if (!groupLabels.containsKey(key)) {
                groupLabels.put(key, resolveGroupLabel(node.subject));
            }
        }

        LinkedHashMap<String, GroupStyle> result = new LinkedHashMap<>();
        int index = 0;
        int total = Math.max(1, groupLabels.size());

        for (Map.Entry<String, String> entry : groupLabels.entrySet()) {
            result.put(entry.getKey(), fallbackGroupStyle(entry.getKey(), entry.getValue(), index, total));
            index++;
        }

        return result;
    }

    private GroupStyle fallbackGroupStyle(String groupKey, int index) {
        return fallbackGroupStyle(groupKey, resolveGroupLabelFromKey(groupKey), index, 1);
    }

    private GroupStyle fallbackGroupStyle(String groupKey, String label, int index, int total) {
        String color = resolveNodeColor(label);
        String border = darken(color, 0.22f);
        String font = resolveFontColor(color);

        double angle = total <= 1 ? 0.0 : ((Math.PI * 2d) * index / total);
        float radius = total <= 1 ? 0f : 360f;
        float seedX = (float) (Math.cos(angle) * radius);
        float seedY = (float) (Math.sin(angle) * radius);

        GroupStyle style = new GroupStyle();
        style.key = groupKey;
        style.label = label;
        style.nodeColor = color;
        style.borderColor = border;
        style.fontColor = font;
        style.seedX = seedX;
        style.seedY = seedY;
        return style;
    }

    private String resolveGroupKey(String subject) {
        if (TextUtils.isEmpty(subject)) {
            return "other";
        }

        String normalized = subject.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("мат")) return "math";
        if (normalized.contains("физ")) return "physics";
        if (normalized.contains("хим")) return "chemistry";
        if (normalized.contains("био")) return "biology";
        if (normalized.contains("лит")) return "literature";
        if (normalized.contains("рус")) return "russian";
        if (normalized.contains("ист")) return "history";
        if (normalized.contains("гео")) return "geography";
        if (normalized.contains("инф")) return "informatics";
        if (normalized.contains("анг")) return "english";

        return "subject_" + Math.abs(normalized.hashCode());
    }

    private String resolveGroupLabel(String subject) {
        if (!TextUtils.isEmpty(subject)) {
            return subject.trim();
        }
        return "Без предмета";
    }

    private String resolveGroupLabelFromKey(String groupKey) {
        if ("math".equals(groupKey)) return "Математика";
        if ("physics".equals(groupKey)) return "Физика";
        if ("chemistry".equals(groupKey)) return "Химия";
        if ("biology".equals(groupKey)) return "Биология";
        if ("literature".equals(groupKey)) return "Литература";
        if ("russian".equals(groupKey)) return "Русский";
        if ("history".equals(groupKey)) return "История";
        if ("geography".equals(groupKey)) return "География";
        if ("informatics".equals(groupKey)) return "Информатика";
        if ("english".equals(groupKey)) return "Английский";
        return "Без предмета";
    }

    private Set<Long> bfsVisibleIds(long focusId, int depth, Map<Long, Set<Long>> adjacency) {
        Set<Long> visited = new LinkedHashSet<>();
        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();

        visited.add(focusId);
        queue.add(new NodeDepth(focusId, 0));

        while (!queue.isEmpty()) {
            NodeDepth current = queue.removeFirst();
            if (current.depth >= depth) {
                continue;
            }

            Set<Long> neighbors = adjacency.get(current.nodeId);
            if (neighbors == null || neighbors.isEmpty()) {
                continue;
            }

            for (Long neighborId : neighbors) {
                if (visited.add(neighborId)) {
                    queue.addLast(new NodeDepth(neighborId, current.depth + 1));
                }
            }
        }

        return visited;
    }

    private GraphSnapshot loadSnapshot() throws Exception {
        Object repository = getRepository();

        List<?> nodeObjects = loadBestList(repository, SourceKind.NODES);
        List<?> edgeObjects = loadBestList(repository, SourceKind.EDGES);
        List<?> noteObjects = loadBestList(repository, SourceKind.NOTES);

        Map<Long, RawNode> nodes = new LinkedHashMap<>();

        for (Object object : nodeObjects) {
            RawNode rawNode = parseNode(object);
            if (rawNode != null) {
                nodes.put(rawNode.id, rawNode);
            }
        }

        if (nodes.isEmpty()) {
            for (Object object : noteObjects) {
                RawNode rawNode = parseNode(object);
                if (rawNode != null) {
                    nodes.put(rawNode.id, rawNode);
                }
            }
        }

        List<RawEdge> edges = new ArrayList<>();
        Set<String> uniqueEdges = new HashSet<>();

        for (Object object : edgeObjects) {
            RawEdge rawEdge = parseEdge(object);
            if (rawEdge == null) {
                continue;
            }
            if (!nodes.containsKey(rawEdge.fromId) || !nodes.containsKey(rawEdge.toId)) {
                continue;
            }
            String key = buildEdgeKey(rawEdge.fromId, rawEdge.toId);
            if (uniqueEdges.add(key)) {
                edges.add(rawEdge);
            }
        }

        if (edges.isEmpty() && !noteObjects.isEmpty()) {
            List<RawEdge> fallbackEdges = deriveEdgesFromNoteBodies(noteObjects, nodes.keySet());
            for (RawEdge rawEdge : fallbackEdges) {
                String key = buildEdgeKey(rawEdge.fromId, rawEdge.toId);
                if (uniqueEdges.add(key)) {
                    edges.add(rawEdge);
                }
            }
        }

        return new GraphSnapshot(new ArrayList<>(nodes.values()), edges);
    }

    private List<RawEdge> deriveEdgesFromNoteBodies(List<?> noteObjects, Set<Long> allowedNodeIds) {
        List<RawEdge> edges = new ArrayList<>();

        for (Object object : noteObjects) {
            if (object == null) {
                continue;
            }

            long fromId = extractLong(object, "getId", "getNoteId", "id", "noteId");
            if (fromId <= 0L || !allowedNodeIds.contains(fromId)) {
                continue;
            }

            String html = extractString(object,
                    "getContentHtml", "getHtml", "getContent",
                    "contentHtml", "html", "content");

            if (TextUtils.isEmpty(html)) {
                continue;
            }

            Matcher matcher = INTERNAL_NOTE_LINK_PATTERN.matcher(html);
            while (matcher.find()) {
                try {
                    long toId = Long.parseLong(matcher.group(1));
                    if (toId <= 0L || toId == fromId || !allowedNodeIds.contains(toId)) {
                        continue;
                    }
                    RawEdge edge = new RawEdge();
                    edge.fromId = fromId;
                    edge.toId = toId;
                    edges.add(edge);
                } catch (Throwable ignored) {
                }
            }
        }

        return edges;
    }

    private String buildEdgeKey(long fromId, long toId) {
        return fromId < toId ? fromId + "_" + toId : toId + "_" + fromId;
    }

    private Object getRepository() throws Exception {
        if (repositoryRef != null) {
            return repositoryRef;
        }

        String[] candidateClassNames = new String[]{
                "com.veda.app.data.repo.VedaRepository",
                "com.veda.app.data.VedaRepository",
                "com.veda.app.data.repository.VedaRepository",
                "com.veda.app.repository.VedaRepository"
        };

        for (String className : candidateClassNames) {
            try {
                Class<?> repositoryClass = Class.forName(className);

                Method getInstance = findCompatibleFactory(repositoryClass);
                if (getInstance != null) {
                    Object repo = invokeFactory(getInstance);
                    if (repo != null) {
                        repositoryRef = repo;
                        return repo;
                    }
                }

                Object repo = tryConstruct(repositoryClass);
                if (repo != null) {
                    repositoryRef = repo;
                    return repo;
                }
            } catch (Throwable ignored) {
            }
        }

        throw new IllegalStateException("VedaRepository not found");
    }

    private Method findCompatibleFactory(Class<?> repositoryClass) {
        for (Method method : repositoryClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!repositoryClass.isAssignableFrom(method.getReturnType())
                    && !method.getReturnType().isAssignableFrom(repositoryClass)) {
                continue;
            }
            String name = method.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("instance") && !name.contains("get")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && Application.class.isAssignableFrom(params[0])) {
                method.setAccessible(true);
                return method;
            }
            if (params.length == 1 && android.content.Context.class.isAssignableFrom(params[0])) {
                method.setAccessible(true);
                return method;
            }
            if (params.length == 0) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private Object invokeFactory(Method method) throws Exception {
        Class<?>[] params = method.getParameterTypes();
        if (params.length == 1 && Application.class.isAssignableFrom(params[0])) {
            return method.invoke(null, getApplication());
        }
        if (params.length == 1 && android.content.Context.class.isAssignableFrom(params[0])) {
            return method.invoke(null, getApplication().getApplicationContext());
        }
        if (params.length == 0) {
            return method.invoke(null);
        }
        return null;
    }

    private Object tryConstruct(Class<?> repositoryClass) {
        try {
            return repositoryClass.getConstructor(Application.class).newInstance(getApplication());
        } catch (Throwable ignored) {
        }

        try {
            return repositoryClass.getConstructor(android.content.Context.class)
                    .newInstance(getApplication().getApplicationContext());
        } catch (Throwable ignored) {
        }

        try {
            return repositoryClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ignored) {
        }

        return null;
    }

    private List<?> loadBestList(Object rootSource, SourceKind kind) throws Exception {
        List<Object> sources = discoverSources(rootSource);

        List<Candidate> sorted = new ArrayList<>();
        for (Object source : sources) {
            sorted.addAll(discoverCandidates(source, kind));
        }
        Collections.sort(sorted, (left, right) -> Integer.compare(right.score, left.score));

        for (Candidate candidate : sorted) {
            try {
                Object value = candidate.method.invoke(candidate.source);
                List<?> list = unwrapToList(value);
                if (list != null) {
                    return list;
                }
            } catch (Throwable ignored) {
            }
        }

        return Collections.emptyList();
    }

    private List<Object> discoverSources(Object rootSource) {
        List<Object> sources = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        sources.add(rootSource);
        seen.add(rootSource);

        try {
            for (Method method : rootSource.getClass().getMethods()) {
                if (method.getParameterTypes().length != 0) {
                    continue;
                }
                String returnName = method.getReturnType().getSimpleName().toLowerCase(Locale.ROOT);
                String methodName = method.getName().toLowerCase(Locale.ROOT);
                if (!returnName.contains("dao")
                        && !returnName.contains("database")
                        && !methodName.contains("dao")
                        && !methodName.contains("database")) {
                    continue;
                }
                method.setAccessible(true);
                Object nested = method.invoke(rootSource);
                if (nested != null && seen.add(nested)) {
                    sources.add(nested);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Field field : rootSource.getClass().getDeclaredFields()) {
                String typeName = field.getType().getSimpleName().toLowerCase(Locale.ROOT);
                String fieldName = field.getName().toLowerCase(Locale.ROOT);
                if (!typeName.contains("dao")
                        && !typeName.contains("database")
                        && !fieldName.contains("dao")
                        && !fieldName.contains("database")) {
                    continue;
                }
                field.setAccessible(true);
                Object nested = field.get(rootSource);
                if (nested != null && seen.add(nested)) {
                    sources.add(nested);
                }
            }
        } catch (Throwable ignored) {
        }

        return sources;
    }

    private List<Candidate> discoverCandidates(Object source, SourceKind kind) {
        List<Candidate> result = new ArrayList<>();
        for (Method method : source.getClass().getMethods()) {
            if (method.getParameterTypes().length != 0) {
                continue;
            }

            String returnTypeName = method.getReturnType().getName();
            if (!returnTypeName.contains("LiveData")
                    && !Collection.class.isAssignableFrom(method.getReturnType())
                    && !method.getReturnType().isArray()) {
                continue;
            }

            int score = scoreMethod(method, kind);
            if (score <= 0) {
                continue;
            }

            method.setAccessible(true);
            result.add(new Candidate(source, method, score));
        }
        return result;
    }

    private int scoreMethod(Method method, SourceKind kind) {
        String name = method.getName().toLowerCase(Locale.ROOT);
        Type genericType = method.getGenericReturnType();
        String signature = genericType != null
                ? genericType.getTypeName().toLowerCase(Locale.ROOT)
                : method.getReturnType().getName().toLowerCase(Locale.ROOT);

        int score = 0;

        if (kind == SourceKind.NODES) {
            if (signature.contains("graphnodeitem")) score += 350;
            if (name.contains("graph")) score += 120;
            if (name.contains("node")) score += 90;
            if (name.contains("note")) score += 40;
            if (name.contains("all")) score += 20;
            if (signature.contains("homework")) score -= 250;
            if (signature.contains("subject")) score -= 180;
            if (signature.contains("link")) score -= 220;
        } else if (kind == SourceKind.EDGES) {
            if (signature.contains("link")) score += 360;
            if (signature.contains("edge")) score += 260;
            if (name.contains("link")) score += 160;
            if (name.contains("edge")) score += 160;
            if (name.contains("graph")) score += 60;
            if (name.contains("backlink")) score += 80;
            if (name.contains("relation")) score += 80;
            if (signature.contains("graphnodeitem")) score -= 260;
            if (signature.contains("homework")) score -= 240;
            if (signature.contains("subject")) score -= 200;
        } else if (kind == SourceKind.NOTES) {
            if (signature.contains("note")) score += 240;
            if (name.contains("note")) score += 140;
            if (name.contains("all")) score += 40;
            if (name.contains("list")) score += 25;
            if (signature.contains("graphnodeitem")) score -= 300;
            if (signature.contains("link")) score -= 260;
            if (signature.contains("subject")) score -= 220;
            if (signature.contains("homework")) score -= 260;
        }

        if (method.getReturnType().getName().contains("LiveData")) score += 25;
        if (Collection.class.isAssignableFrom(method.getReturnType())) score += 25;
        if (method.getReturnType().isArray()) score += 20;

        return score;
    }

    private List<?> unwrapToList(Object value) throws Exception {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof List) {
            return (List<?>) value;
        }

        if (value instanceof Collection) {
            return new ArrayList<>((Collection<?>) value);
        }

        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(value, i));
            }
            return list;
        }

        if (value instanceof LiveData) {
            return awaitLiveDataList((LiveData<?>) value);
        }

        return Collections.emptyList();
    }

    private List<?> awaitLiveDataList(LiveData<?> liveData) throws Exception {
        Object current = liveData.getValue();
        if (current instanceof List) {
            return (List<?>) current;
        }
        if (current instanceof Collection) {
            return new ArrayList<>((Collection<?>) current);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final List<Object> box = new ArrayList<>(1);

        final Observer<Object> observer = new Observer<Object>() {
            @Override
            public void onChanged(Object value) {
                if (value instanceof List) {
                    box.add(value);
                    liveData.removeObserver(this);
                    latch.countDown();
                } else if (value instanceof Collection) {
                    box.add(new ArrayList<>((Collection<?>) value));
                    liveData.removeObserver(this);
                    latch.countDown();
                }
            }
        };

        mainHandler.post(() -> {
            try {
                liveData.observeForever(observer);
            } catch (Throwable ignored) {
                latch.countDown();
            }
        });

        latch.await(1500, TimeUnit.MILLISECONDS);

        mainHandler.post(() -> {
            try {
                liveData.removeObserver(observer);
            } catch (Throwable ignored) {
            }
        });

        if (!box.isEmpty()) {
            Object value = box.get(0);
            if (value instanceof List) {
                return (List<?>) value;
            }
            if (value instanceof Collection) {
                return new ArrayList<>((Collection<?>) value);
            }
        }

        return Collections.emptyList();
    }

    private RawNode parseNode(Object source) {
        if (source == null) {
            return null;
        }

        long id = extractLong(source, "getId", "getNoteId", "id", "noteId");
        if (id <= 0L) {
            return null;
        }

        String title = extractString(source, "getTitle", "getName", "title", "name");
        String subject = extractString(source,
                "getSubject", "getSubjectName", "getSubjectTitle",
                "subject", "subjectName", "subjectTitle");

        RawNode node = new RawNode();
        node.id = id;
        node.title = !TextUtils.isEmpty(title) ? title : "Заметка " + id;
        node.subject = subject;
        return node;
    }

    private RawEdge parseEdge(Object source) {
        if (source == null) {
            return null;
        }

        long fromId = extractLong(source,
                "getFromId", "getSourceId", "getNoteId", "getSourceNoteId", "getFromNoteId",
                "getSrcId", "getSrcNoteId", "getParentNoteId",
                "fromId", "sourceId", "noteId", "sourceNoteId", "fromNoteId",
                "srcId", "srcNoteId", "parentNoteId");

        long toId = extractLong(source,
                "getToId", "getTargetId", "getLinkedNoteId", "getTargetNoteId", "getToNoteId",
                "getDstId", "getDstNoteId", "getChildNoteId",
                "toId", "targetId", "linkedNoteId", "targetNoteId", "toNoteId",
                "dstId", "dstNoteId", "childNoteId");

        if (fromId <= 0L || toId <= 0L || fromId == toId) {
            return null;
        }

        RawEdge edge = new RawEdge();
        edge.fromId = fromId;
        edge.toId = toId;
        return edge;
    }

    private long extractLong(Object source, String... names) {
        for (String name : names) {
            try {
                if (name.startsWith("get")) {
                    Method method = source.getClass().getMethod(name);
                    method.setAccessible(true);
                    Object value = method.invoke(source);
                    if (value instanceof Number) {
                        return ((Number) value).longValue();
                    }
                } else {
                    Field field = findField(source.getClass(), name);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(source);
                        if (value instanceof Number) {
                            return ((Number) value).longValue();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return -1L;
    }

    private String extractString(Object source, String... names) {
        for (String name : names) {
            try {
                if (name.startsWith("get")) {
                    Method method = source.getClass().getMethod(name);
                    method.setAccessible(true);
                    Object value = method.invoke(source);
                    if (value != null) {
                        String text = String.valueOf(value).trim();
                        if (!text.isEmpty()) {
                            return text;
                        }
                    }
                } else {
                    Field field = findField(source.getClass(), name);
                    if (field != null) {
                        field.setAccessible(true);
                        Object value = field.get(source);
                        if (value != null) {
                            String text = String.valueOf(value).trim();
                            if (!text.isEmpty()) {
                                return text;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return "";
    }

    private Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private String safeNodeLabel(RawNode node) {
        String value = safe(node.title);
        if (value.length() > 42) {
            return value.substring(0, 39) + "...";
        }
        return value;
    }

    private String buildTooltip(RawNode node, int degree) {
        List<String> parts = new ArrayList<>();
        parts.add(safe(node.title));
        if (!TextUtils.isEmpty(node.subject)) {
            parts.add("Предмет: " + node.subject);
        }
        parts.add("Связей: " + degree);
        return TextUtils.join("\n", parts);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveNodeColor(String subject) {
        if (TextUtils.isEmpty(subject)) {
            return "#8A8F98";
        }

        String normalized = subject.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("мат")) return "#4C8BF5";
        if (normalized.contains("физ")) return "#8E6CFF";
        if (normalized.contains("хим")) return "#2AA889";
        if (normalized.contains("био")) return "#4CAF50";
        if (normalized.contains("лит")) return "#D97757";
        if (normalized.contains("рус")) return "#E35D8D";
        if (normalized.contains("ист")) return "#B7814A";
        if (normalized.contains("гео")) return "#17A2B8";
        if (normalized.contains("инф")) return "#5C7CFA";
        if (normalized.contains("анг")) return "#F59F00";

        String[] palette = new String[]{
                "#4C8BF5",
                "#8E6CFF",
                "#2AA889",
                "#D97757",
                "#E35D8D",
                "#17A2B8",
                "#F59F00",
                "#4CAF50",
                "#B7814A"
        };

        int index = Math.abs(normalized.hashCode()) % palette.length;
        return palette[index];
    }

    private String resolveFontColor(String backgroundHex) {
        int color = Color.parseColor(backgroundHex);
        double brightness =
                (0.299 * Color.red(color))
                        + (0.587 * Color.green(color))
                        + (0.114 * Color.blue(color));
        return brightness >= 160 ? "#111111" : "#FFFFFF";
    }

    private String darken(String hex, float factor) {
        int color = Color.parseColor(hex);
        int red = Math.max(0, (int) (Color.red(color) * (1f - factor)));
        int green = Math.max(0, (int) (Color.green(color) * (1f - factor)));
        int blue = Math.max(0, (int) (Color.blue(color) * (1f - factor)));
        return String.format(Locale.US, "#%02X%02X%02X", red, green, blue);
    }

    private String buildEmptyPayloadJson() {
        try {
            GraphConfig config = GraphConfig.defaults();

            JSONObject forceJson = new JSONObject();
            forceJson.put("preset", config.forcePreset.name());
            forceJson.put("groupRadius", 360);
            forceJson.put("nodeSpread", 140);
            forceJson.put("springLength", 165);
            forceJson.put("gravity", -90);

            JSONObject configJson = new JSONObject();
            configJson.put("mode", GraphMode.GLOBAL.name());
            configJson.put("focusNoteId", -1L);
            configJson.put("localDepth", clampDepth(localDepth.getValue()));
            configJson.put("nodeCount", 0);
            configJson.put("edgeCount", 0);
            configJson.put("showLabels", config.showLabels);
            configJson.put("scaleNodesByDegree", config.scaleNodesByDegree);
            configJson.put("useGroupColors", config.useGroupColors);
            configJson.put("fadeEdges", config.fadeEdges);
            configJson.put("forcePreset", config.forcePreset.name());
            configJson.put("repulsion", config.repulsion);
            configJson.put("centralGravity", config.centralGravity);
            configJson.put("springLength", config.springLength);
            configJson.put("springStrength", config.springStrength);
            configJson.put("damping", config.damping);
            configJson.put("overlap", config.overlap);
            configJson.put("stabilizationIterations", config.stabilizationIterations);
            configJson.put("force", forceJson);

            JSONObject filters = new JSONObject();
            filters.put("subjects", new JSONArray());
            filters.put("groups", new JSONArray());

            JSONObject payload = new JSONObject();
            payload.put("config", configJson);
            payload.put("filters", filters);
            payload.put("nodes", new JSONArray());
            payload.put("edges", new JSONArray());
            return payload.toString();
        } catch (Throwable ignored) {
            return "{\"config\":{\"mode\":\"GLOBAL\",\"focusNoteId\":-1,\"localDepth\":2,\"nodeCount\":0,\"edgeCount\":0},\"filters\":{\"subjects\":[],\"groups\":[]},\"nodes\":[],\"edges\":[]}";
        }
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
        super.onCleared();
    }

    private enum SourceKind {
        NODES,
        EDGES,
        NOTES
    }

    private static class Candidate {
        final Object source;
        final Method method;
        final int score;

        Candidate(Object source, Method method, int score) {
            this.source = source;
            this.method = method;
            this.score = score;
        }
    }

    private static class GraphSnapshot {
        final List<RawNode> nodes;
        final List<RawEdge> edges;

        GraphSnapshot(List<RawNode> nodes, List<RawEdge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }
    }

    private static class UiGraphResult {
        final String payloadJson;
        final String status;
        final GraphMode mode;
        final long focusId;
        final int depth;
        final int nodeCount;

        UiGraphResult(String payloadJson, String status, GraphMode mode, long focusId, int depth, int nodeCount) {
            this.payloadJson = payloadJson;
            this.status = status;
            this.mode = mode;
            this.focusId = focusId;
            this.depth = depth;
            this.nodeCount = nodeCount;
        }
    }

    private static class RawNode {
        long id;
        String title;
        String subject;
    }

    private static class RawEdge {
        long fromId;
        long toId;
    }

    private static class NodeDepth {
        final long nodeId;
        final int depth;

        NodeDepth(long nodeId, int depth) {
            this.nodeId = nodeId;
            this.depth = depth;
        }
    }

    private static class GroupStyle {
        String key;
        String label;
        String nodeColor;
        String borderColor;
        String fontColor;
        float seedX;
        float seedY;
    }
}
