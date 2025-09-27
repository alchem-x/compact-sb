import module java.net.http;
import module java.xml;

import java.time.Duration;

private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

private static final int MAX_DEPTH = 16;
private static final Set<String> processedDependencies = new HashSet<>();
private static final Map<String, DependencyInfo> resolvedDependencies = new HashMap<>();

record DependencyInfo(String groupId, String artifactId, String version, String jarPath, int depth) {
    String getKey() {
        return groupId + ":" + artifactId;
    }
}

void main() {
    var mavenRepo = Optional.ofNullable(System.getenv("MAVEN_REPO"))
            .filter(s -> !s.isBlank())
            .orElse("https://repo1.maven.org/maven2");

    logInfo("Using Maven repository: " + mavenRepo);
    logInfo("Starting dependency download...");

    try {
        Files.createDirectories(Paths.get("lib"));
        processLibraryFile(mavenRepo);
        generateDependencyReport();
        logInfo("All dependencies downloaded successfully!");
    } catch (Exception e) {
        logError(e);
        System.exit(1);
    }
}

void processLibraryFile(String mavenRepo) throws IOException {
    for (var dependency : Files.readAllLines(Paths.get("lib.txt"))) {
        var dep = dependency.trim();
        if (!dep.isEmpty() && !dep.startsWith("#")) {
            processDependency(dep, mavenRepo, 0);
        }
    }
}


void generateDependencyReport() {
    logInfo("");
    logInfo("=== Dependency Resolution Report ===");
    logInfo("Total resolved dependencies: " + resolvedDependencies.size());
    logInfo("");

    resolvedDependencies.values().stream()
            .sorted(Comparator.comparing(DependencyInfo::getKey))
            .forEach(dep -> logInfo(String.format("%-50s -> %s (depth: %d)",
                    dep.getKey(), dep.version, dep.depth)));
    logInfo("");
}

void processDependency(String dependency, String mavenRepo, int depth) {
    if (depth > MAX_DEPTH) {
        logWarn("Max depth reached, skipping: " + dependency, depth);
        return;
    }

    try {
        var parts = parseDependency(dependency);
        var key = parts[0] + ":" + parts[1];
        var version = parts[2];

        if (processedDependencies.contains(dependency)) {
            logDebug("Already processed: " + dependency, depth);
            return;
        }

        if (resolvedDependencies.containsKey(key)) {
            var existing = resolvedDependencies.get(key);
            logInfo("Dependency conflict: " + key, depth);
            logInfo("  Keeping nearest version " + existing.version + " (depth: " + existing.depth +
                    "), skipping " + version + " (depth: " + depth + ")", depth);
            processedDependencies.add(dependency);
            return;
        }

        processedDependencies.add(dependency);
        logInfo("Processing: " + dependency, depth);

        var jarPath = downloadIfNeeded(parts, mavenRepo);

        var depInfo = new DependencyInfo(parts[0], parts[1], version, jarPath, depth);
        resolvedDependencies.put(key, depInfo);

        var transitiveDeps = getTransitiveDependencies(parts, mavenRepo);
        for (var transitiveDep : transitiveDeps) {
            processDependency(transitiveDep, mavenRepo, depth + 1);
        }

    } catch (Exception e) {
        logError("Failed to process dependency: " + dependency, e, depth);
    }
}

String[] parseDependency(String dependency) {
    var parts = dependency.split(":");
    if (parts.length != 3) {
        throw new IllegalArgumentException("Invalid dependency format: " + dependency + " (expected: groupId:artifactId:version)");
    }
    return parts;
}

String downloadIfNeeded(String[] parts, String mavenRepo) throws IOException, InterruptedException {
    var jarName = parts[1] + "-" + parts[2] + ".jar";
    var outputPath = Paths.get("lib", jarName);

    if (Files.exists(outputPath)) {
        logDebug("Already exists: " + jarName);
        return outputPath.toString();
    }

    var url = buildJarUrl(parts, mavenRepo);
    logDebug("Downloading: " + url);

    var response = sendHttpRequest(url);
    if (response.statusCode() != 200) {
        throw new IOException("HTTP " + response.statusCode() + " for " + url);
    }

    Files.copy(response.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
    logInfo("Downloaded: " + jarName);
    return outputPath.toString();
}

HttpResponse<InputStream> sendHttpRequest(String url) throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Libup/1.0")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
    return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
}

List<String> getTransitiveDependencies(String[] parts, String mavenRepo) {
    try {
        var pomUrl = buildPomUrl(parts, mavenRepo);
        logDebug("Fetching POM: " + pomUrl);

        var response = sendHttpRequest(pomUrl);
        if (response.statusCode() != 200) {
            logDebug("POM not found, skipping transitive dependencies");
            return List.of();
        }

        return parsePomDependencies(response.body());
    } catch (Exception e) {
        logWarn("Failed to fetch POM for " + parts[1] + ":" + parts[2] + ": " + e.getMessage());
        return List.of();
    }
}

List<String> parsePomDependencies(InputStream pomInputStream) throws Exception {
    var dependencies = new ArrayList<String>();

    try (pomInputStream) {
        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        var document = builder.parse(pomInputStream);

        var dependencyNodes = document.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            var dependencyNode = dependencyNodes.item(i);
            if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                var element = (Element) dependencyNode;

                if (shouldSkipDependency(element)) continue;

                var groupId = getElementText(element, "groupId");
                var artifactId = getElementText(element, "artifactId");
                var version = getElementText(element, "version");

                if (isValidDependency(groupId, artifactId, version)) {
                    dependencies.add(groupId + ":" + artifactId + ":" + version);
                }
            }
        }
    }

    return dependencies;
}

String getElementText(Element parent, String tagName) {
    var nodes = parent.getElementsByTagName(tagName);
    return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
}

boolean shouldSkipDependency(Element element) {
    var scope = getElementText(element, "scope");
    if ("test".equals(scope)) return true;

    var optional = getElementText(element, "optional");
    return "true".equals(optional);
}

boolean isValidDependency(String groupId, String artifactId, String version) {
    return groupId != null && artifactId != null && version != null && !version.startsWith("${");
}

String buildJarUrl(String[] parts, String mavenRepo) {
    return mavenRepo + "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
}

String buildPomUrl(String[] parts, String mavenRepo) {
    return mavenRepo + "/" + parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".pom";
}

void logInfo(String message) {
    IO.println(message);
}

void logInfo(String message, int depth) {
    IO.println("  ".repeat(depth) + message);
}

void logDebug(String message) {
    IO.println("  " + message);
}

void logDebug(String message, int depth) {
    IO.println("  ".repeat(depth) + "  " + message);
}

void logWarn(String message) {
    IO.println("WARN: " + message);
}

void logWarn(String message, int depth) {
    IO.println("  ".repeat(depth) + "WARN: " + message);
}

void logError(Exception e) {
    IO.println("ERROR: Failed to process dependencies - " + e.getMessage());
}

void logError(String message, Exception e, int depth) {
    IO.println("  ".repeat(depth) + "ERROR: " + message + " - " + e.getMessage());
}