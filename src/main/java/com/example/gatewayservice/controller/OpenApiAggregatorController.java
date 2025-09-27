package com.example.gatewayservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OpenApiAggregatorController {

    private final WebClient webClient = WebClient.builder().build();

    @GetMapping("/v3/api-docs")
    public Mono<Map<String, Object>> aggregate() {
        Mono<Map<String, Object>> memberDocs = fetchDocs("http://mcp-hub-member:8080/v3/api-docs");
        Mono<Map<String, Object>> mcpDocs = fetchDocs("http://mcp-hub-mcp:8080/v3/api-docs");
        Mono<Map<String, Object>> workspaceDocs = fetchDocs("http://mcp-hub-workspace:8080/v3/api-docs");

        return Mono.zip(memberDocs, mcpDocs, workspaceDocs)
                .map(tuple -> {
                    Map<String, Object> merged = new HashMap<>(tuple.getT1());

                    // paths 병합
                    Map<String, Object> paths = new LinkedHashMap<>();
                    paths.putAll(getMap(tuple.getT1(), "paths"));
                    paths.putAll(getMap(tuple.getT2(), "paths"));
                    paths.putAll(getMap(tuple.getT3(), "paths"));
                    merged.put("paths", paths);

                    // components 병합
                    Map<String, Object> components = new LinkedHashMap<>();
                    components.putAll(getMap(tuple.getT1(), "components"));
                    components.putAll(getMap(tuple.getT2(), "components"));
                    components.putAll(getMap(tuple.getT3(), "components"));
                    merged.put("components", components);

                    return merged;
                });
    }

    private Mono<Map<String, Object>> fetchDocs(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
