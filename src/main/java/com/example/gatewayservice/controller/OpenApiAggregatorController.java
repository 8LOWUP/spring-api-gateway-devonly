package com.example.gatewayservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Hidden
@RestController
@RequiredArgsConstructor
public class OpenApiAggregatorController {

    private final WebClient webClient = WebClient.builder().build();

    @GetMapping("/v3/api-docs/aggregate")
    public Mono<Map<String, Object>> aggregate() {
        Mono<Map<String, Object>> memberDocs = fetchDocs("http://mcp-hub-member:8080/v3/api-docs");
        Mono<Map<String, Object>> mcpDocs = fetchDocs("http://mcp-hub-mcp:8080/v3/api-docs");
        Mono<Map<String, Object>> workspaceDocs = fetchDocs("http://mcp-hub-workspace:8080/v3/api-docs");

        return Mono.zip(memberDocs, mcpDocs, workspaceDocs)
                .map(tuple -> {
                    Map<String, Object> base = new HashMap<>(tuple.getT1());

                    // info 수정 (원하는 대로)
                    Map<String, Object> info = new HashMap<>();
                    info.put("title", "API Gateway Aggregated Docs");
                    info.put("version", "1.0");
                    base.put("info", info);

                    // paths 병합
                    Map<String, Object> paths = new LinkedHashMap<>();
                    paths.putAll(getMap(tuple.getT1(), "paths"));
                    paths.putAll(getMap(tuple.getT2(), "paths"));
                    paths.putAll(getMap(tuple.getT3(), "paths"));
                    base.put("paths", paths);

                    // components 병합
                    Map<String, Object> components = new LinkedHashMap<>();
                    components.putAll(getMap(tuple.getT1(), "components"));
                    components.putAll(getMap(tuple.getT2(), "components"));
                    components.putAll(getMap(tuple.getT3(), "components"));
                    base.put("components", components);

                    // Swagger UI가 Gateway 경유해서 요청하도록 servers 설정 추가
                    base.put("servers", List.of(
                            Map.of("url", "/members", "description", "Member Service"),
                            Map.of("url", "/mcps", "description", "MCP Service"),
                            Map.of("url", "/workspaces", "description", "Workspace Service")
                    ));

                    return base;
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
