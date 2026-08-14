package dev.chpg.notebook.ui;

import dev.chpg.pg.api.Graph;
import dev.chpg.pg.multiverse.universe.*;
import dev.pgv.exporter.*;
import io.github.spencerpark.ijava.runtime.Display;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.io.IOException;

public class PGV {

    // 2. Adapter records
    record GraphNode(String id, List<String> tags, Map<String, Object> attributes) implements ExportNode {}
    record GraphEdge(String id, String source, String target, List<String> tags, Map<String, Object> attributes) implements ExportEdge {}
    record GraphSnapshot(String graphId, long version, List<GraphNode> nodesList, List<GraphEdge> edgesList) implements ExportGraph {
        public ExportSchema schema() { return null; }
        public Iterable<? extends ExportNode> nodes() { return nodesList; }
        public Iterable<? extends ExportEdge> edges() { return edgesList; }
    }

    public static void show(Graph graph) {
        try {
            // 1. Create the backend Universe graph
            Universe universe = new Universe();
            int nodeId = universe.idGenerator().createNodeId();
            UniverseNode helloNode = new UniverseNode(universe, nodeId);

            GraphNode exportHelloNode = new GraphNode(
                String.valueOf(helloNode.id()),
                List.of("TestNode"),
                Map.of("name", "hello")
            );

            GraphSnapshot snapshot = new GraphSnapshot("hello-world-graph", 1, List.of(exportHelloNode), List.of());

            // 3. Serialize JSON
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PgvExporter exporter = new PgvExporter();
            exporter.exportGraph(snapshot, baos);
            String jsonPayload = baos.toString(StandardCharsets.UTF_8);

            // 4. Read the Vite Bundles and escape closing script tags
            String jsBundlePath = System.getProperty("pgv.bundle.js", "/app/pgv/dist/pgv-bundle.js");
            String jsBundle = Files.readString(Paths.get(jsBundlePath));
            jsBundle = jsBundle.replace("</script>", "<\\/script>");

            String cssBundlePath = System.getProperty("pgv.bundle.css", "/app/pgv/dist/graph-core.css");
            String cssBundle = Files.readString(Paths.get(cssBundlePath));

            // Generate a unique container ID so multiple cells don't clash
            String containerId = "pgv-viz-" + System.currentTimeMillis();

            // 5. Build the direct HTML injection string using safe .replace() tokens
            String directHtml = """
                <style>
                    __CSS_BUNDLE__
                </style>
                <div id="__CONTAINER_ID__" style="width: 100%; height: 600px; border: 1px solid #ccc; border-radius: 8px; background: #fafafa; resize: vertical; overflow: hidden;"></div>
                <script>
                    // Evaluate the IIFE bundle directly in the notebook context
                    __JS_BUNDLE__
                </script>
                <script>
                    try {
                        let payload = __JSON_PAYLOAD__;

                        payload.schema = {
                            nodes: { "TestNode": { color: "#4f46e5", label: "Test Node" } },
                            edges: {}
                        };

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

            Display.display(directHtml, "text/html");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load pgv bundles", e);
        }
    }
}
