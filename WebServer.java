import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WebServer {
    private static final String DATA_FILE = "data/fixmycampus-web.dat";
    private static final Path WEB_DIR = Path.of("web");
    private static final List<Issue> issues = loadIssues();
    private static int nextId = issues.stream().mapToInt(i -> i.id).max().orElse(0) + 1;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/issues", WebServer::handleIssues);
        server.createContext("/", WebServer::serveFile);
        server.setExecutor(null);
        server.start();
        System.out.println("FixMyCampus is running at http://localhost:8080");
    }

    private static void handleIssues(HttpExchange exchange) throws IOException {
        addCors(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { exchange.sendResponseHeaders(204, -1); return; }
        String path = exchange.getRequestURI().getPath();
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 200, issueJson());
        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            Map<String, String> fields = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String title = fields.getOrDefault("title", "").trim();
            String description = fields.getOrDefault("description", "").trim();
            String location = fields.getOrDefault("location", "").trim();
            String name = fields.getOrDefault("name", "Student").trim();
            if (title.isEmpty() || description.isEmpty() || location.isEmpty()) { send(exchange, 400, "{\"error\":\"Please fill all required fields.\"}"); return; }
            Issue issue = new Issue(nextId++, title, description, location, "", Category.valueOf(fields.getOrDefault("category", "OTHER")),
                    Math.max(1, Math.min(5, parseInt(fields.get("severity"), 3))));
            issue.messages.add("Reported by " + name);
            synchronized (issues) { issues.add(issue); saveIssues(); }
            send(exchange, 201, issueJson(issue));
        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST") && path.matches("/api/issues/\\d+/vote")) {
            send(exchange, 400, "{}");
        } else if (path.matches("/api/issues/\\d+/vote") && exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            int id = parseId(path);
            Map<String, String> fields = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            Issue issue = findIssue(id);
            if (issue == null) { send(exchange, 404, "{}"); return; }
            int vote = Math.max(-1, Math.min(1, parseInt(fields.get("vote"), 0)));
            synchronized (issues) { if (vote == 0) issue.votes.remove("web-user"); else issue.votes.put("web-user", vote); saveIssues(); }
            send(exchange, 200, issueJson(issue));
        } else if (path.matches("/api/issues/\\d+/status") && exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            Issue issue = findIssue(parseId(path));
            Map<String, String> fields = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try { if (issue != null) issue.status = Status.valueOf(fields.get("status")); }
            catch (Exception ignored) { }
            if (issue == null) { send(exchange, 404, "{}"); return; }
            saveIssues(); send(exchange, 200, issueJson(issue));
        } else if (path.matches("/api/issues/\\d+/messages") && exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            Issue issue = findIssue(parseId(path));
            Map<String, String> fields = parseForm(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (issue == null) { send(exchange, 404, "{}"); return; }
            issue.messages.add(fields.getOrDefault("name", "Student") + ": " + fields.getOrDefault("message", ""));
            saveIssues(); send(exchange, 200, issueJson(issue));
        } else send(exchange, 404, "{}");
    }

    private static void serveFile(HttpExchange exchange) throws IOException {
        String requested = exchange.getRequestURI().getPath();
        if (requested.equals("/")) requested = "/index.html";
        Path file = WEB_DIR.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(WEB_DIR) || !Files.isRegularFile(file)) { send(exchange, 404, "Not found"); return; }
        String type = requested.endsWith(".css") ? "text/css" : requested.endsWith(".js") ? "text/javascript" : "text/html";
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String issueJson() {
        synchronized (issues) {
            return "[" + String.join(",", issues.stream().sorted(Comparator.comparingInt(Issue::priority).reversed()).map(WebServer::issueJson).toList()) + "]";
        }
    }

    private static String issueJson(Issue i) {
        return "{\"id\":" + i.id + ",\"title\":\"" + esc(i.title) + "\",\"description\":\"" + esc(i.description)
                + "\",\"location\":\"" + esc(i.location) + "\",\"category\":\"" + i.category + "\",\"severity\":" + i.severity
                + ",\"status\":\"" + i.status + "\",\"support\":" + i.support() + ",\"priority\":" + i.priority()
                + ",\"messages\":[" + String.join(",", i.messages.stream().map(m -> "\"" + esc(m) + "\"").toList()) + "]}";
    }

    private static String esc(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
    private static void addCors(HttpExchange e) { e.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); e.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS"); e.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type"); }
    private static void send(HttpExchange e, int code, String body) throws IOException { byte[] bytes = body.getBytes(StandardCharsets.UTF_8); e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); e.sendResponseHeaders(code, bytes.length); e.getResponseBody().write(bytes); e.close(); }
    private static int parseInt(String s, int fallback) { try { return Integer.parseInt(s); } catch (Exception e) { return fallback; } }
    private static int parseId(String path) { return parseInt(path.replaceAll("[^0-9]", " ").trim().split("\\s+")[0], -1); }
    private static Map<String, String> parseForm(String body) { Map<String, String> map = new HashMap<>(); for (String part : body.split("&")) { String[] pair = part.split("=", 2); if (pair.length == 2) try { map.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8")); } catch (Exception ignored) { } } return map; }
    private static Issue findIssue(int id) { synchronized (issues) { return issues.stream().filter(i -> i.id == id).findFirst().orElse(null); } }
    private static List<Issue> loadIssues() {
        File file = new File(DATA_FILE); if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) { return (List<Issue>) in.readObject(); }
        catch (Exception e) { return new ArrayList<>(); }
    }
    private static void saveIssues() {
        File file = new File(DATA_FILE); file.getParentFile().mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) { out.writeObject(issues); }
        catch (IOException e) { System.out.println("Could not save data: " + e.getMessage()); }
    }
}
