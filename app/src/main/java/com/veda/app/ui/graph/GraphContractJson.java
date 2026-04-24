package com.veda.app.ui.graph;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public final class GraphContractJson {

    private GraphContractJson() {
    }

    public static String toJson(GraphPayload payload) {
        try {
            JSONObject root = new JSONObject();

            if (payload == null) {
                root.put("version", 1);
                root.put("mode", "GLOBAL");
                root.put("focusNoteId", JSONObject.NULL);
                root.put("localDepth", 1);
                root.put("config", new JSONObject());
                root.put("nodes", new JSONArray());
                root.put("edges", new JSONArray());
                return root.toString();
            }

            root.put("version", payload.version);
            root.put("mode", payload.mode == null ? GraphMode.GLOBAL.name() : payload.mode.name());
            root.put("focusNoteId", payload.focusNoteId == null ? JSONObject.NULL : payload.focusNoteId);
            root.put("localDepth", payload.localDepth);
            root.put("config", toJsonObject(payload.config));
            root.put("nodes", toJsonArray(payload.nodes));
            root.put("edges", toJsonArray(payload.edges));

            return root.toString();
        } catch (Exception e) {
            return "{\"version\":1,\"mode\":\"GLOBAL\",\"focusNoteId\":null,\"localDepth\":1,\"config\":{},\"nodes\":[],\"edges\":[]}";
        }
    }

    private static JSONArray toJsonArray(Object source) {
        JSONArray array = new JSONArray();

        if (source == null) {
            return array;
        }

        try {
            if (source instanceof Iterable) {
                for (Object item : (Iterable<?>) source) {
                    array.put(normalizeValue(item));
                }
                return array;
            }

            if (source.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(source);
                for (int i = 0; i < length; i++) {
                    array.put(normalizeValue(java.lang.reflect.Array.get(source, i)));
                }
            }
        } catch (Exception ignored) {
        }

        return array;
    }

    private static JSONObject toJsonObject(Object source) {
        JSONObject json = new JSONObject();

        if (source == null) {
            return json;
        }

        try {
            if (source instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) source;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    json.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
                }
                return json;
            }

            Class<?> current = source.getClass();
            while (current != null && current != Object.class) {
                Field[] fields = current.getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    field.setAccessible(true);
                    Object value = field.get(source);
                    json.put(field.getName(), normalizeValue(value));
                }
                current = current.getSuperclass();
            }
        } catch (Exception ignored) {
        }

        return json;
    }

    private static Object normalizeValue(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        }

        if (value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof Iterable || value.getClass().isArray()) {
            return toJsonArray(value);
        }

        if (value instanceof Map) {
            return toJsonObject(value);
        }

        return toJsonObject(value);
    }
}