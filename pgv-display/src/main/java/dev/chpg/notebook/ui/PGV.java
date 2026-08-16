package dev.chpg.notebook.ui;

import dev.chpg.pg.api.*;
import dev.pgv.exporter.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PGV {

    // -------------------------------------------------------------------------
    // 1. Adapter Records for PgvExporter
    // -------------------------------------------------------------------------

    private record PgvNode(
        String id, 
        List<String> tags, 
        Map<String, Object> attributes
    ) implements ExportNode {}

    private record PgvEdge(
        String id, 
        String source, 
        String target, 
        List<String> tags, 
        Map<String, Object> attributes
    ) implements ExportEdge {}

    private record PgvGraphSnapshot(
        String graphId, 
        long version, 
        List<PgvNode> nodesList, 
        List<PgvEdge> edgesList
    ) implements ExportGraph {
        @Override
        public ExportSchema schema() {
            // Hardcode common containment tags based on your example
            return () -> List.of("XCSG.Contains"); 
        }

        @Override
        public Iterable<? extends ExportNode> nodes() { return nodesList; }

        @Override
        public Iterable<? extends ExportEdge> edges() { return edgesList; }
    }

    // -------------------------------------------------------------------------
    // 2. Data Translation Layer
    // -------------------------------------------------------------------------

    /**
     * Unwraps the sealed AttributeValue into standard Java types for the JSON Exporter.
     */
    private static Object unwrapAttribute(AttributeValue attrVal) {
        if (attrVal == null) {
            return null;
        } 
        if (attrVal instanceof AttributeValue.StringValue) {
            return ((AttributeValue.StringValue) attrVal).value();
        } else if (attrVal instanceof AttributeValue.BooleanValue) {
            return ((AttributeValue.BooleanValue) attrVal).value();
        } else if (attrVal instanceof AttributeValue.IntegerValue) {
            return ((AttributeValue.IntegerValue) attrVal).value();
        } else if (attrVal instanceof AttributeValue.LongValue) {
            return ((AttributeValue.LongValue) attrVal).value();
        } else if (attrVal instanceof AttributeValue.DoubleValue) {
            return ((AttributeValue.DoubleValue) attrVal).value();
        } else if (attrVal instanceof AttributeValue.ByteArrayValue) {
            return Base64.getEncoder().encodeToString(((AttributeValue.ByteArrayValue) attrVal).value());
        } else {
            throw new IllegalArgumentException("Unsupported AttributeValue type: " + attrVal.getClass().getName());
        }
    }

    /**
     * Converts an AttributeMap into a standard Map<String, Object>.
     */
    private static Map<String, Object> unwrapAttributes(AttributeMap attributeMap) {
        if (attributeMap == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> unwrapped = new HashMap<>();
        // Note: AttributeMap implements Map<String, AttributeValue>
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            unwrapped.put(entry.getKey(), unwrapAttribute(entry.getValue()));
        }
        return unwrapped;
    }

    /**
     * Adapts the dev.chpg.pg.api.Graph into an ExportGraph.
     */
    private static ExportGraph adaptGraph(Graph graph) {
        List<PgvNode> pgvNodes = new ArrayList<>();
        // NodeSet inherits from standard iterables/collections
        for (Node node : graph.nodes()) {
            // TagSet is a Set<String>
            List<String> tags = new ArrayList<>(node.tags());
            pgvNodes.add(new PgvNode(
                String.valueOf(node.id()), // GraphElements have primitive int identity
                tags, 
                unwrapAttributes(node.attributes())
            ));
        }

        List<PgvEdge> pgvEdges = new ArrayList<>();
        // EdgeSet inherits from standard iterables/collections
        for (Edge edge : graph.edges()) {
            List<String> tags = new ArrayList<>(edge.tags());
            pgvEdges.add(new PgvEdge(
                String.valueOf(edge.id()),
                String.valueOf(edge.from().id()), // Edges strictly dictate direction from->to
                String.valueOf(edge.to().id()),
                tags, 
                unwrapAttributes(edge.attributes())
            ));
        }

        return new PgvGraphSnapshot("jupyter-graph-" + System.currentTimeMillis(), 1L, pgvNodes, pgvEdges);
    }

    // -------------------------------------------------------------------------
    // 3. Visualization & Rendering Layer
    // -------------------------------------------------------------------------

    public static void show(Graph graph) {
        try {
            // 1. Extract and Serialize
            ExportGraph snapshot = adaptGraph(graph);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PgvExporter exporter = new PgvExporter();
            exporter.exportGraph(snapshot, baos);
            String jsonPayload = baos.toString(StandardCharsets.UTF_8);

            // 2. Read Vite Bundles
            // (Adjust these paths if your Docker container mounts them elsewhere)
            String jsBundle = Files.readString(Paths.get("/home/jovyan/work/pgv/dist/pgv-bundle.js"))
                                   .replace("</script>", "<\\/script>");
            String cssBundle = Files.readString(Paths.get("/home/jovyan/work/pgv/dist/graph-core.css"));
            
            String containerId = "pgv-viz-" + System.currentTimeMillis();

            // 3. Build HTML Output
            String directHtml = """
                <style>
                    __CSS_BUNDLE__
                </style>
                <div id="__CONTAINER_ID__" style="width: 100%; max-width: 100%; height: 600px; border: 1px solid #ccc; border-radius: 8px; background: #fafafa; resize: vertical; overflow: hidden; box-sizing: border-box;"></div>
                <script>
                    __JS_BUNDLE__
                </script>
                <script>
                    try {
                        let payload = __JSON_PAYLOAD__;
                        
                        // Default fallback schema if 'pgv' expects one
                        if (!payload.schema) {
                            payload.schema = { nodes: {}, edges: {} };
                        }
                        
                        const container = document.getElementById("__CONTAINER_ID__");
                        const graphSnapshot = pgv.createGraphSnapshot(payload);
                        const view = new pgv.GraphView(container, payload.schema, {
                            layoutOptions: { nodeWidth: 240, nodeHeight: 94, layerSpacing: 152, nodeSpacing: 290, margin: 36 },
                            usePanZoom: true,
                            useThemeToggle: true
                        });
                        
                        view.setGraph(graphSnapshot);
                    } catch (err) {
                        document.getElementById("__CONTAINER_ID__").innerHTML = "<div style='color:red; padding: 20px;'>" + 
                            "<h3>Visualizer Crash:</h3>" + err.message + "<br><br><pre>" + err.stack + "</pre></div>";
                    }
                </script>
                """
                .replace("__CSS_BUNDLE__", cssBundle)
                .replace("__CONTAINER_ID__", containerId)
                .replace("__JS_BUNDLE__", jsBundle)
                .replace("__JSON_PAYLOAD__", jsonPayload);

            // 4. Invoke IJava Display via Reflection (Bypassing stubs)
            Class<?> displayClass = Class.forName("io.github.spencerpark.ijava.runtime.Display");
            
            try {
                // Try varargs signature
                java.lang.reflect.Method displayVarargs = displayClass.getMethod("display", Object.class, String[].class);
                displayVarargs.invoke(null, directHtml, new String[]{"text/html"});
            } catch (NoSuchMethodException e) {
                // Fallback to exact signature
                java.lang.reflect.Method displayStrict = displayClass.getMethod("display", Object.class, String.class);
                displayStrict.invoke(null, directHtml, "text/html");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to render PGV graph", e);
        }
    }
}
