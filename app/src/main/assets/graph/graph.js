(function () {
    const state = {
        theme: "dark",
        payload: null,
        rawNodes: [],
        rawEdges: [],
        network: null,
        nodes: null,
        edges: null,
        filters: {
            subjects: [],
            hideIsolated: false
        },
        display: {
            showLabels: true,
            scaleNodesByDegree: true,
            useGroupColors: true,
            fadeEdges: false
        },
        force: {
            preset: "balanced"
        },
        lastFiltered: {
            nodes: [],
            edges: []
        },
        touch: {
            targetEl: null,
            startDomPoint: null,
            startViewPosition: null,
            startScale: 1,
            startDistance: 0,
            anchorCanvasPoint: null,
            nodeId: null,
            mode: null,
            moved: false,
            held: false,
            holdTimer: null,
            lastHoldAt: 0
        }
    };

    const graphEl = document.getElementById("graph");
    const fallbackEl = document.getElementById("fallback");

    function bridgeCall(method, value) {
        try {
            if (window.AndroidBridge && typeof window.AndroidBridge[method] === "function") {
                if (value === undefined) {
                    window.AndroidBridge[method]();
                } else {
                    window.AndroidBridge[method](String(value));
                }
            }
        } catch (e) {
            // ignore
        }
    }

    function decodeBase64Utf8(base64) {
        const binary = window.atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i += 1) {
            bytes[i] = binary.charCodeAt(i);
        }
        return new TextDecoder("utf-8").decode(bytes);
    }

    function edgeColor() {
        return state.theme === "dark"
            ? (state.display.fadeEdges ? "rgba(140,150,168,0.36)" : "#8C96A8")
            : (state.display.fadeEdges ? "rgba(123,135,150,0.30)" : "#7B8796");
    }

    function backgroundColor() {
        return state.theme === "dark" ? "#111315" : "#F6F7FB";
    }

    function clearHoldTimer() {
        if (state.touch.holdTimer) {
            clearTimeout(state.touch.holdTimer);
            state.touch.holdTimer = null;
        }
    }

    function resetTouchState() {
        clearHoldTimer();
        state.touch.startDomPoint = null;
        state.touch.startViewPosition = null;
        state.touch.startScale = 1;
        state.touch.startDistance = 0;
        state.touch.anchorCanvasPoint = null;
        state.touch.nodeId = null;
        state.touch.mode = null;
        state.touch.moved = false;
        state.touch.held = false;
    }

    function getTouchTarget() {
        return graphEl.querySelector("canvas") || graphEl;
    }

    function getDomPointFromTouch(touch) {
        const rect = graphEl.getBoundingClientRect();
        return {
            x: touch.clientX - rect.left,
            y: touch.clientY - rect.top
        };
    }

    function midpoint(p1, p2) {
        return {
            x: (p1.x + p2.x) / 2,
            y: (p1.y + p2.y) / 2
        };
    }

    function distance(p1, p2) {
        const dx = p1.x - p2.x;
        const dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    function moveExceeded(start, current, threshold) {
        if (!start || !current) {
            return false;
        }
        return Math.abs(current.x - start.x) > threshold || Math.abs(current.y - start.y) > threshold;
    }

    function clampScale(scale) {
        return Math.max(0.18, Math.min(4.0, scale));
    }

    function stableHash(value) {
        const text = String(value);
        let hash = 0;
        for (let i = 0; i < text.length; i += 1) {
            hash = ((hash << 5) - hash) + text.charCodeAt(i);
            hash |= 0;
        }
        return Math.abs(hash);
    }

    function hash01(value) {
        return (stableHash(value) % 100000) / 100000;
    }

    function organicOffset(id) {
        const angle = hash01(id + ":angle") * Math.PI * 2;
        const radius = 34 + (hash01(id + ":radius") * 140);
        return {
            x: Math.cos(angle) * radius,
            y: Math.sin(angle) * radius
        };
    }

    function getForcePreset() {
        const preset = state.force && state.force.preset ? state.force.preset : "balanced";
        if (preset === "compact" || preset === "wide") {
            return preset;
        }
        return "balanced";
    }

    function getPhysicsConfig(nodeCount) {
        const largeGraph = nodeCount >= 120;
        const preset = getForcePreset();

        let config;
        if (largeGraph) {
            if (preset === "compact") {
                config = {
                    enabled: true,
                    solver: "barnesHut",
                    stabilization: {
                        enabled: true,
                        iterations: 850,
                        updateInterval: 50,
                        fit: false
                    },
                    barnesHut: {
                        gravitationalConstant: -2800,
                        centralGravity: 0.16,
                        springLength: 120,
                        springConstant: 0.028,
                        damping: 0.30,
                        avoidOverlap: 0.24
                    },
                    minVelocity: 0.75
                };
            } else if (preset === "wide") {
                config = {
                    enabled: true,
                    solver: "barnesHut",
                    stabilization: {
                        enabled: true,
                        iterations: 1250,
                        updateInterval: 50,
                        fit: false
                    },
                    barnesHut: {
                        gravitationalConstant: -6200,
                        centralGravity: 0.025,
                        springLength: 260,
                        springConstant: 0.014,
                        damping: 0.24,
                        avoidOverlap: 0.44
                    },
                    minVelocity: 0.55
                };
            } else {
                config = {
                    enabled: true,
                    solver: "barnesHut",
                    stabilization: {
                        enabled: true,
                        iterations: 1000,
                        updateInterval: 50,
                        fit: false
                    },
                    barnesHut: {
                        gravitationalConstant: -4400,
                        centralGravity: 0.06,
                        springLength: 185,
                        springConstant: 0.020,
                        damping: 0.28,
                        avoidOverlap: 0.34
                    },
                    minVelocity: 0.60
                };
            }
        } else {
            if (preset === "compact") {
                config = {
                    enabled: true,
                    solver: "forceAtlas2Based",
                    stabilization: {
                        enabled: true,
                        iterations: 520,
                        updateInterval: 40,
                        fit: false
                    },
                    forceAtlas2Based: {
                        gravitationalConstant: -62,
                        centralGravity: 0.018,
                        springLength: 105,
                        springConstant: 0.080,
                        damping: 0.48,
                        avoidOverlap: 0.45
                    },
                    minVelocity: 0.75
                };
            } else if (preset === "wide") {
                config = {
                    enabled: true,
                    solver: "forceAtlas2Based",
                    stabilization: {
                        enabled: true,
                        iterations: 760,
                        updateInterval: 40,
                        fit: false
                    },
                    forceAtlas2Based: {
                        gravitationalConstant: -135,
                        centralGravity: 0.003,
                        springLength: 220,
                        springConstant: 0.038,
                        damping: 0.42,
                        avoidOverlap: 0.86
                    },
                    minVelocity: 0.52
                };
            } else {
                config = {
                    enabled: true,
                    solver: "forceAtlas2Based",
                    stabilization: {
                        enabled: true,
                        iterations: 650,
                        updateInterval: 40,
                        fit: false
                    },
                    forceAtlas2Based: {
                        gravitationalConstant: -96,
                        centralGravity: 0.005,
                        springLength: 160,
                        springConstant: 0.052,
                        damping: 0.46,
                        avoidOverlap: 0.72
                    },
                    minVelocity: 0.60
                };
            }
        }

        return config;
    }

    function createOptions(nodeCount) {
        return {
            autoResize: true,
            interaction: {
                dragNodes: true,
                dragView: false,
                hover: false,
                multiselect: false,
                navigationButtons: false,
                selectable: true,
                zoomView: false,
                keyboard: false
            },
            physics: getPhysicsConfig(nodeCount),
            nodes: {
                shape: "dot",
                borderWidth: 2,
                borderWidthSelected: 3,
                size: 18,
                font: {
                    size: 14,
                    face: "sans-serif",
                    multi: false,
                    strokeWidth: 0
                },
                scaling: {
                    min: 14,
                    max: 38,
                    label: {
                        enabled: true,
                        min: 12,
                        max: 18,
                        drawThreshold: 4
                    }
                }
            },
            edges: {
                color: {
                    color: edgeColor(),
                    highlight: edgeColor(),
                    hover: edgeColor()
                },
                smooth: false,
                width: state.display.fadeEdges ? 1.2 : 1.8,
                selectionWidth: 0
            },
            layout: {
                improvedLayout: true
            }
        };
    }

    function normalizeNodes(nodes) {
        return nodes.map(function (node) {
            const seedX = typeof node.seedX === "number" ? node.seedX * 1.35 : 0;
            const seedY = typeof node.seedY === "number" ? node.seedY * 1.35 : 0;
            const offset = organicOffset(node.id);

            return {
                id: node.id,
                label: node.label || "",
                title: node.title || "",
                value: node.value || 18,
                shape: "dot",
                subject: node.subject || "",
                groupKey: node.groupKey || "other",
                groupLabel: node.groupLabel || node.subject || "Без предмета",
                degree: node.degree || 0,
                borderWidth: node.borderWidth || 2,
                rawColor: {
                    background: node.backgroundColor || "#8A8F98",
                    border: node.borderColor || "#6B717A"
                },
                rawFontColor: node.fontColor || (state.theme === "dark" ? "#FFFFFF" : "#111111"),
                x: seedX + offset.x,
                y: seedY + offset.y
            };
        });
    }

    function normalizeEdges(edges) {
        return edges.map(function (edge) {
            return {
                id: edge.id,
                from: edge.from,
                to: edge.to
            };
        });
    }

    function materializeNode(node) {
        const label = state.display.showLabels ? (node.label || "") : "";
        const value = state.display.scaleNodesByDegree ? (node.value || 18) : 18;

        let background = node.rawColor.background;
        let border = node.rawColor.border;
        let fontColor = node.rawFontColor;

        if (!state.display.useGroupColors) {
            background = state.theme === "dark" ? "#8E97A6" : "#A7AFBC";
            border = state.theme === "dark" ? "#D5DAE3" : "#6B7480";
            fontColor = state.theme === "dark" ? "#FFFFFF" : "#111111";
        }

        return {
            id: node.id,
            label: label,
            title: node.title || "",
            value: value,
            shape: "dot",
            subject: node.subject || "",
            groupKey: node.groupKey || "other",
            groupLabel: node.groupLabel || node.subject || "Без предмета",
            degree: node.degree || 0,
            borderWidth: node.borderWidth || 2,
            x: node.x,
            y: node.y,
            color: {
                background: background,
                border: border,
                highlight: {
                    background: background,
                    border: border
                },
                hover: {
                    background: background,
                    border: border
                }
            },
            font: {
                color: fontColor
            }
        };
    }

    function materializeEdge(edge) {
        return {
            id: edge.id,
            from: edge.from,
            to: edge.to,
            color: {
                color: edgeColor(),
                highlight: edgeColor(),
                hover: edgeColor()
            },
            width: state.display.fadeEdges ? 1.2 : 1.8
        };
    }

    function removeTouchHandlers() {
        if (!state.touch.targetEl) {
            return;
        }

        const el = state.touch.targetEl;

        if (el.__vedaTouchStart) {
            el.removeEventListener("touchstart", el.__vedaTouchStart, { passive: false });
        }
        if (el.__vedaTouchMove) {
            el.removeEventListener("touchmove", el.__vedaTouchMove, { passive: false });
        }
        if (el.__vedaTouchEnd) {
            el.removeEventListener("touchend", el.__vedaTouchEnd, { passive: false });
        }
        if (el.__vedaTouchCancel) {
            el.removeEventListener("touchcancel", el.__vedaTouchCancel, { passive: false });
        }

        delete el.__vedaTouchStart;
        delete el.__vedaTouchMove;
        delete el.__vedaTouchEnd;
        delete el.__vedaTouchCancel;

        state.touch.targetEl = null;
    }

    function destroyNetwork() {
        removeTouchHandlers();
        resetTouchState();

        if (state.network) {
            try {
                state.network.destroy();
            } catch (e) {
                // ignore
            }
        }
        state.network = null;
    }

    function installMouseClickHandler() {
        if (!state.network) {
            return;
        }

        state.network.on("click", function (params) {
            const srcEvent = params && params.event ? params.event.srcEvent : null;
            const isTouchLike = srcEvent && (
                (typeof srcEvent.pointerType === "string" && (srcEvent.pointerType === "touch" || srcEvent.pointerType === "pen")) ||
                (typeof srcEvent.type === "string" && srcEvent.type.indexOf("touch") >= 0)
            );

            if (isTouchLike) {
                return;
            }

            if (!params || !params.nodes || params.nodes.length === 0) {
                return;
            }

            bridgeCall("onNodeTap", params.nodes[0]);
        });
    }

    function installTouchHandlers() {
        if (!state.network) {
            return;
        }

        removeTouchHandlers();

        const targetEl = getTouchTarget();
        state.touch.targetEl = targetEl;

        const onTouchStart = function (event) {
            if (!state.network) {
                return;
            }

            if (event.touches.length === 2) {
                const p1 = getDomPointFromTouch(event.touches[0]);
                const p2 = getDomPointFromTouch(event.touches[1]);
                const mid = midpoint(p1, p2);

                clearHoldTimer();

                state.touch.mode = "pinch";
                state.touch.startDomPoint = mid;
                state.touch.startDistance = distance(p1, p2);
                state.touch.startScale = state.network.getScale();
                state.touch.anchorCanvasPoint = state.network.DOMtoCanvas(mid);
                state.touch.startViewPosition = state.network.getViewPosition();
                state.touch.moved = false;
                state.touch.held = false;
                state.touch.nodeId = null;

                event.preventDefault();
                return;
            }

            if (event.touches.length !== 1) {
                return;
            }

            const point = getDomPointFromTouch(event.touches[0]);
            const nodeId = state.network.getNodeAt(point);

            state.touch.startDomPoint = point;
            state.touch.startViewPosition = state.network.getViewPosition();
            state.touch.startScale = state.network.getScale();
            state.touch.nodeId = nodeId || null;
            state.touch.moved = false;
            state.touch.held = false;

            if (nodeId) {
                state.touch.mode = "node";
                clearHoldTimer();
                state.touch.holdTimer = setTimeout(function () {
                    if (!state.touch.moved && state.touch.nodeId) {
                        state.touch.held = true;
                        state.touch.lastHoldAt = Date.now();
                        bridgeCall("onNodeHold", state.touch.nodeId);
                    }
                }, 520);
            } else {
                state.touch.mode = "pan";
                clearHoldTimer();
            }
        };

        const onTouchMove = function (event) {
            if (!state.network || !state.touch.mode) {
                return;
            }

            if (event.touches.length === 2 && state.touch.mode === "pinch") {
                const p1 = getDomPointFromTouch(event.touches[0]);
                const p2 = getDomPointFromTouch(event.touches[1]);
                const mid = midpoint(p1, p2);
                const dist = distance(p1, p2);

                if (state.touch.startDistance > 0) {
                    const nextScale = clampScale(state.touch.startScale * (dist / state.touch.startDistance));
                    const centerDom = {
                        x: graphEl.clientWidth / 2,
                        y: graphEl.clientHeight / 2
                    };

                    const nextPos = {
                        x: state.touch.anchorCanvasPoint.x - ((mid.x - centerDom.x) / nextScale),
                        y: state.touch.anchorCanvasPoint.y - ((mid.y - centerDom.y) / nextScale)
                    };

                    state.network.moveTo({
                        position: nextPos,
                        scale: nextScale,
                        animation: false
                    });
                }

                event.preventDefault();
                return;
            }

            if (event.touches.length !== 1) {
                return;
            }

            const point = getDomPointFromTouch(event.touches[0]);

            if (moveExceeded(state.touch.startDomPoint, point, 8)) {
                state.touch.moved = true;
                clearHoldTimer();
            }

            if (state.touch.mode === "pan") {
                const scale = state.network.getScale();
                const dx = point.x - state.touch.startDomPoint.x;
                const dy = point.y - state.touch.startDomPoint.y;

                const nextPos = {
                    x: state.touch.startViewPosition.x - (dx / scale),
                    y: state.touch.startViewPosition.y - (dy / scale)
                };

                state.network.moveTo({
                    position: nextPos,
                    animation: false
                });

                event.preventDefault();
            }
        };

        const onTouchEnd = function (event) {
            if (!state.touch.mode) {
                resetTouchState();
                return;
            }

            clearHoldTimer();

            if (state.touch.mode === "node"
                && state.touch.nodeId
                && !state.touch.held
                && !state.touch.moved
                && (Date.now() - state.touch.lastHoldAt) > 700
                && event.touches.length === 0) {
                bridgeCall("onNodeTap", state.touch.nodeId);
            }

            resetTouchState();
        };

        const onTouchCancel = function () {
            resetTouchState();
        };

        targetEl.__vedaTouchStart = onTouchStart;
        targetEl.__vedaTouchMove = onTouchMove;
        targetEl.__vedaTouchEnd = onTouchEnd;
        targetEl.__vedaTouchCancel = onTouchCancel;

        targetEl.addEventListener("touchstart", onTouchStart, { passive: false });
        targetEl.addEventListener("touchmove", onTouchMove, { passive: false });
        targetEl.addEventListener("touchend", onTouchEnd, { passive: false });
        targetEl.addEventListener("touchcancel", onTouchCancel, { passive: false });
    }

    function applyCurrentFilters() {
        const subjects = Array.isArray(state.filters.subjects) ? state.filters.subjects : [];
        const selectedSubjectSet = new Set(subjects);
        const filterBySubjects = selectedSubjectSet.size > 0;

        let visibleNodes = state.rawNodes.filter(function (node) {
            if (!filterBySubjects) {
                return true;
            }
            return selectedSubjectSet.has(node.subject || "");
        });

        const visibleIds = new Set(visibleNodes.map(function (node) {
            return node.id;
        }));

        let visibleEdges = state.rawEdges.filter(function (edge) {
            return visibleIds.has(edge.from) && visibleIds.has(edge.to);
        });

        if (state.filters.hideIsolated) {
            const degree = new Map();

            visibleEdges.forEach(function (edge) {
                degree.set(edge.from, (degree.get(edge.from) || 0) + 1);
                degree.set(edge.to, (degree.get(edge.to) || 0) + 1);
            });

            visibleNodes = visibleNodes.filter(function (node) {
                return (degree.get(node.id) || 0) > 0;
            });

            const connectedIds = new Set(visibleNodes.map(function (node) {
                return node.id;
            }));

            visibleEdges = visibleEdges.filter(function (edge) {
                return connectedIds.has(edge.from) && connectedIds.has(edge.to);
            });
        }

        return {
            nodes: visibleNodes,
            edges: visibleEdges
        };
    }

    function applyVisualsToCurrentGraph() {
        if (!state.network || !state.nodes || !state.edges) {
            return;
        }

        const nodeUpdates = state.lastFiltered.nodes.map(function (node) {
            const materialized = materializeNode(node);
            return {
                id: materialized.id,
                label: materialized.label,
                value: materialized.value,
                borderWidth: materialized.borderWidth,
                color: materialized.color,
                font: materialized.font
            };
        });

        const edgeUpdates = state.lastFiltered.edges.map(function (edge) {
            const materialized = materializeEdge(edge);
            return {
                id: materialized.id,
                color: materialized.color,
                width: materialized.width
            };
        });

        try {
            state.nodes.update(nodeUpdates);
            state.edges.update(edgeUpdates);

            state.network.setOptions({
                edges: {
                    color: {
                        color: edgeColor(),
                        highlight: edgeColor(),
                        hover: edgeColor()
                    },
                    width: state.display.fadeEdges ? 1.2 : 1.8
                },
                nodes: {
                    scaling: {
                        min: state.display.scaleNodesByDegree ? 14 : 18,
                        max: state.display.scaleNodesByDegree ? 38 : 18,
                        label: {
                            enabled: state.display.showLabels,
                            min: 12,
                            max: 18,
                            drawThreshold: 4
                        }
                    }
                }
            });
        } catch (e) {
            // ignore
        }
    }

    function focusGraphForViewport(duration) {
        if (!state.network || !state.nodes || state.nodes.length === 0) {
            return;
        }

        const ids = state.nodes.getIds();
        if (!ids || ids.length === 0) {
            return;
        }

        let left = Number.POSITIVE_INFINITY;
        let right = Number.NEGATIVE_INFINITY;
        let top = Number.POSITIVE_INFINITY;
        let bottom = Number.NEGATIVE_INFINITY;

        ids.forEach(function (id) {
            try {
                const box = state.network.getBoundingBox(id);
                if (!box) {
                    return;
                }
                left = Math.min(left, box.left);
                right = Math.max(right, box.right);
                top = Math.min(top, box.top);
                bottom = Math.max(bottom, box.bottom);
            } catch (e) {
                // ignore
            }
        });

        if (!isFinite(left) || !isFinite(right) || !isFinite(top) || !isFinite(bottom)) {
            return;
        }

        const graphWidth = Math.max(1, right - left);
        const graphHeight = Math.max(1, bottom - top);

        const viewportWidth = Math.max(1, graphEl.clientWidth);
        const viewportHeight = Math.max(1, graphEl.clientHeight);

        const reservedBottom = Math.min(320, viewportHeight * 0.38);
        const safeWidth = Math.max(1, viewportWidth - 24);
        const safeHeight = Math.max(1, viewportHeight - reservedBottom - 18);

        const scaleX = safeWidth / graphWidth;
        const scaleY = safeHeight / graphHeight;
        const scale = clampScale(Math.min(scaleX, scaleY) * 0.96);

        const center = {
            x: (left + right) / 2,
            y: (top + bottom) / 2
        };

        const offset = {
            x: 0,
            y: -reservedBottom * 0.48
        };

        try {
            state.network.moveTo({
                position: center,
                scale: scale,
                offset: offset,
                animation: duration > 0 ? {
                    duration: duration,
                    easingFunction: "easeOutQuad"
                } : false
            });
        } catch (e) {
            // ignore
        }
    }

    function restabilizeWithCurrentForce() {
        if (!state.network) {
            return;
        }

        const physics = getPhysicsConfig(state.lastFiltered.nodes.length);
        let completed = false;

        function finish() {
            if (completed || !state.network) {
                return;
            }
            completed = true;

            try {
                state.network.setOptions({
                    physics: {
                        enabled: false
                    }
                });
            } catch (e) {
                // ignore
            }

            focusGraphForViewport(240);
        }

        try {
            state.network.setOptions({
                physics: physics
            });

            state.network.once("stabilizationIterationsDone", function () {
                finish();
            });

            state.network.stabilize(physics.stabilization.iterations || 700);
        } catch (e) {
            finish();
            return;
        }

        setTimeout(function () {
            finish();
        }, 1600);
    }

    function render() {
        if (!window.vis || !window.vis.Network || !graphEl) {
            fallbackEl.style.display = "flex";
            bridgeCall("onRendererError", "Не удалось инициализировать renderer графа");
            return;
        }

        fallbackEl.style.display = "none";
        graphEl.style.background = backgroundColor();

        const filtered = applyCurrentFilters();
        state.lastFiltered = filtered;

        destroyNetwork();

        state.nodes = new vis.DataSet(filtered.nodes.map(materializeNode));
        state.edges = new vis.DataSet(filtered.edges.map(materializeEdge));

        state.network = new vis.Network(
            graphEl,
            {
                nodes: state.nodes,
                edges: state.edges
            },
            createOptions(filtered.nodes.length)
        );

        installMouseClickHandler();
        installTouchHandlers();

        let finished = false;

        function finishInitialLayout() {
            if (finished || !state.network) {
                return;
            }
            finished = true;

            try {
                state.network.setOptions({
                    physics: {
                        enabled: false
                    }
                });
            } catch (e) {
                // ignore
            }

            focusGraphForViewport(260);
        }

        state.network.once("stabilizationIterationsDone", function () {
            finishInitialLayout();
        });

        setTimeout(function () {
            finishInitialLayout();
        }, 1600);
    }

    function setTheme(theme) {
        state.theme = theme === "light" ? "light" : "dark";
        document.body.classList.toggle("light", state.theme === "light");
        document.body.classList.toggle("dark", state.theme !== "light");
        graphEl.style.background = backgroundColor();

        if (state.network) {
            applyVisualsToCurrentGraph();
        }
    }

    function setPayload(payload) {
        state.payload = payload || { nodes: [], edges: [], config: {}, filters: {} };
        state.rawNodes = normalizeNodes(state.payload.nodes || []);
        state.rawEdges = normalizeEdges(state.payload.edges || []);
        render();
    }

    function setPayloadBase64(base64) {
        try {
            const decoded = decodeBase64Utf8(base64);
            const payload = JSON.parse(decoded);
            setPayload(payload);
        } catch (e) {
            fallbackEl.style.display = "flex";
            bridgeCall("onRendererError", "Не удалось отрисовать граф");
        }
    }

    function applyFilters(filters) {
        state.filters = {
            subjects: Array.isArray(filters.subjects) ? filters.subjects : [],
            hideIsolated: !!filters.hideIsolated
        };
        render();
    }

    function applyFiltersBase64(base64) {
        try {
            const decoded = decodeBase64Utf8(base64);
            const filters = JSON.parse(decoded);
            applyFilters(filters || {});
        } catch (e) {
            // ignore
        }
    }

    function applyDisplaySettings(settings) {
        state.display = {
            showLabels: settings.showLabels !== false,
            scaleNodesByDegree: settings.scaleNodesByDegree !== false,
            useGroupColors: settings.useGroupColors !== false,
            fadeEdges: !!settings.fadeEdges
        };

        if (state.network && state.lastFiltered.nodes.length > 0) {
            applyVisualsToCurrentGraph();
        } else {
            render();
        }
    }

    function applyDisplaySettingsBase64(base64) {
        try {
            const decoded = decodeBase64Utf8(base64);
            const settings = JSON.parse(decoded);
            applyDisplaySettings(settings || {});
        } catch (e) {
            // ignore
        }
    }

    function applyForceSettings(settings) {
        const preset = settings && typeof settings.preset === "string" ? settings.preset : "balanced";
        state.force.preset = (preset === "compact" || preset === "wide") ? preset : "balanced";

        if (state.network && state.lastFiltered.nodes.length > 0) {
            restabilizeWithCurrentForce();
        }
    }

    function applyForceSettingsBase64(base64) {
        try {
            const decoded = decodeBase64Utf8(base64);
            const settings = JSON.parse(decoded);
            applyForceSettings(settings || {});
        } catch (e) {
            // ignore
        }
    }

    function resetView() {
        if (!state.network) {
            return;
        }

        try {
            state.network.stopSimulation();
        } catch (e) {
            // ignore
        }

        try {
            state.network.setOptions({
                physics: {
                    enabled: false
                }
            });
        } catch (e) {
            // ignore
        }

        try {
            state.network.unselectAll();
        } catch (e) {
            // ignore
        }

        try {
            state.network.redraw();
        } catch (e) {
            // ignore
        }

        focusGraphForViewport(220);

        requestAnimationFrame(function () {
            if (!state.network) {
                return;
            }

            try {
                state.network.redraw();
            } catch (e) {
                // ignore
            }

            focusGraphForViewport(0);

            setTimeout(function () {
                if (!state.network) {
                    return;
                }

                try {
                    state.network.redraw();
                } catch (e) {
                    // ignore
                }

                focusGraphForViewport(0);
            }, 80);
        });
    }

    window.VedaGraph = {
        setTheme: setTheme,
        setPayload: setPayload,
        setPayloadBase64: setPayloadBase64,
        applyFilters: applyFilters,
        applyFiltersBase64: applyFiltersBase64,
        applyDisplaySettings: applyDisplaySettings,
        applyDisplaySettingsBase64: applyDisplaySettingsBase64,
        applyForceSettings: applyForceSettings,
        applyForceSettingsBase64: applyForceSettingsBase64,
        resetView: resetView
    };

    try {
        bridgeCall("onRendererReady");
    } catch (e) {
        // ignore
    }
})();
