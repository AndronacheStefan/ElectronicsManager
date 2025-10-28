// improvements: output numeric JSON values as numbers (not as quoted strings)
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WebApiServer {
    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/equipment", new EquipmentHandler());
        server.createContext("/api/reload", new ReloadHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Web API server started at http://localhost:" + port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // Helper to add basic CORS headers
    private static void addCors(Headers h) {
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
    }

    static class EquipmentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers respHeaders = exchange.getResponseHeaders();
            addCors(respHeaders);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            Map<String, String> query = parseQuery(uri.getRawQuery());
            String typeFilter = query.get("type"); // expected simple class name e.g. Imprimante

            List<Electronice> list;
            synchronized (MainAPP.echipamente) {
                // defensive copy
                list = new ArrayList<>(MainAPP.echipamente);
            }

            if (typeFilter != null && !typeFilter.isEmpty()) {
                final String tf = typeFilter;
                list = list.stream()
                        .filter(e -> e.getClass().getSimpleName().equalsIgnoreCase(tf))
                        .collect(Collectors.toList());
            }

            String json = toJson(list);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            respHeaders.add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private static Map<String, String> parseQuery(String raw) {
            if (raw == null || raw.isEmpty()) return Map.of();
            return java.util.Arrays.stream(raw.split("&"))
                    .map(s -> s.split("=", 2))
                    .filter(a -> a.length == 2)
                    .collect(Collectors.toMap(a -> decode(a[0]), a -> decode(a[1])));
        }

        private static String decode(String s) {
            try {
                return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            } catch (Exception ex) {
                return s;
            }
        }

        private static String toJson(List<Electronice> list) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Electronice e : list) {
                if (!first) sb.append(',');
                first = false;
                sb.append(eToJson(e));
            }
            sb.append("]");
            return sb.toString();
        }

        private static String eToJson(Electronice e) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            appendStringField(sb, "class", e.getClass().getSimpleName());
            appendStringField(sb, "denumire", e.getDenumire());
            appendNumberField(sb, "nr_inv", e.getNr_inv());
            appendNumberField(sb, "pret", e.getPret());
            appendStringField(sb, "zona_magazin", e.getZona_magazin());
            appendStringField(sb, "stare", e.getStare() == null ? "" : e.getStare().name());

            if (e instanceof Imprimante) {
                Imprimante im = (Imprimante) e;
                appendNumberField(sb, "ppm", im.getPpm());
                appendNumberField(sb, "dpi", im.getDpi());
                appendNumberField(sb, "p_car", im.getP_car());
                appendStringField(sb, "mod_tiparire", im.getMod_tiparire() == null ? "" : im.getMod_tiparire().name());
            } else if (e instanceof Copiatoarele) {
                Copiatoarele c = (Copiatoarele) e;
                appendNumberField(sb, "p_ton", c.getP_ton());
                appendStringField(sb, "format_copiere", c.getFormat_copiere() == null ? "" : c.getFormat_copiere().name());
            } else if (e instanceof SistemCalculatoare) {
                SistemCalculatoare s = (SistemCalculatoare) e;
                appendStringField(sb, "tip_mon", s.getTip_mon());
                appendNumberField(sb, "vit_proc", s.getVit_proc());
                appendNumberField(sb, "c_hdd", s.getC_hdd());
                appendStringField(sb, "sistem_operare", s.getSistem_operare() == null ? "" : s.getSistem_operare().name());
            }

            // remove trailing comma if present
            if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
            sb.append('}');
            return sb.toString();
        }

        private static void appendStringField(StringBuilder sb, String key, String value) {
            String norm = normalize(value == null ? "" : value);
            sb.append('"').append(escape(key)).append('"').append(':');
            sb.append('"').append(escape(norm)).append('"').append(',');
        }

        private static void appendNumberField(StringBuilder sb, String key, double value) {
            sb.append('"').append(escape(key)).append('"').append(':');
            sb.append(String.valueOf(value)).append(',');
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }


    }

    static class ReloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers respHeaders = exchange.getResponseHeaders();
            addCors(respHeaders);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // reload DB
            try {
                synchronized (MainAPP.echipamente) {
                    MainAPP.echipamente.clear();
                    MainAPP.citireDinBD();
                }
            } catch (Exception ex) {
                String msg = "{\"error\":\"" + ex.getMessage() + "\"}";
                byte[] b = msg.getBytes(StandardCharsets.UTF_8);
                respHeaders.add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(500, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                return;
            }

            // return the new list
            String json;
            synchronized (MainAPP.echipamente) {
                json = EquipmentHandler.toJson(new ArrayList<>(MainAPP.echipamente));
            }
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            respHeaders.add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    // Static file handler: serve j.html, f.css and other files from project root
    static class StaticHandler implements HttpHandler {
        private static final Path ROOT = Paths.get(".").toAbsolutePath().normalize();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/j.html"; // default

            // disallow path traversal
            if (path.contains("..")) {
                sendString(exchange, 400, "Invalid path");
                return;
            }

            Path resolved = ROOT.resolve(path.substring(1)).normalize();
            if (!resolved.startsWith(ROOT) || !Files.exists(resolved) || Files.isDirectory(resolved)) {
                sendString(exchange, 404, "Not found");
                return;
            }

            String contentType = guessContentType(resolved.getFileName().toString());
            byte[] data = Files.readAllBytes(resolved);
            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", contentType);
            addCors(h); // allow API calls from the served page
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
        }

        private static void sendString(HttpExchange exchange, int status, String message) throws IOException {
            byte[] b = message.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            addCors(exchange.getResponseHeaders());
            exchange.sendResponseHeaders(status, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }

        private static String guessContentType(String name) {
            name = name.toLowerCase();
            if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
            if (name.endsWith(".css")) return "text/css; charset=utf-8";
            if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (name.endsWith(".json")) return "application/json; charset=utf-8";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }
}
