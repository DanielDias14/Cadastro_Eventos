package br.com.minispring.exemplo.controller;

import br.com.minispring.framework.annotation.*;
import java.util.Map;

@RestController
public final class InfoController {
    private final String appName;

    public InfoController(@Value("app.name") String appName) { this.appName = appName; }

    @Route(method = "GET", path = "/")
    public Map<String, String> info() { return Map.of("application", appName, "framework", "Mini Spring", "server", "Jetty"); }

    @Route(method = "GET", path = "/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }
}
