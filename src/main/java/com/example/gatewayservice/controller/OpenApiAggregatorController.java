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

                   // components deep merge
                    Map<String, Object> components = deepMergeComponents(
                            getMap(tuple.getT1(), "components"),
                            getMap(tuple.getT2(), "components"),
                            getMap(tuple.getT3(), "components")
                    );
                    base.put("components", components);

                    // BaseResponse 정규화 (제네릭 풀린 스키마 -> allOf 구조)
                    normalizeBaseResponses(base);

                    // Swagger UI가 Gateway 경유해서 요청하도록 servers 설정 추가
                    base.put("servers", List.of(
                            Map.of("url", "/", "description", "MCP hub Service")
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

    // schemas, securitySchemes, parameters, responses 단위로 합치기
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMergeComponents(Map<String, Object>... componentsList) {
        Map<String, Object> merged = new LinkedHashMap<>();

        merged.put("schemas", new LinkedHashMap<>());
        merged.put("securitySchemes", new LinkedHashMap<>());
        merged.put("parameters", new LinkedHashMap<>());
        merged.put("responses", new LinkedHashMap<>());

        for (Map<String, Object> comp : componentsList) {
            if (comp == null) continue;
            for (String key : merged.keySet()) {
                Map<String, Object> source = (Map<String, Object>) comp.getOrDefault(key, Map.of());
                ((Map<String, Object>) merged.get(key)).putAll(source);
            }
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private void normalizeBaseResponses(Map<String, Object> openApiDoc) {
        Map<String, Object> components = (Map<String, Object>) openApiDoc.get("components");
        if (components == null) return;

        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        if (schemas == null) return;

        // 공통 BaseResponse 정의 추가 (없으면)
        schemas.putIfAbsent("BaseResponse", Map.of(
                "type", "object",
                "properties", Map.of(
                        "timestamp", Map.of("type", "string", "format", "date-time"),
                        "code", Map.of("type", "string"),
                        "message", Map.of("type", "string"),
                        "result", Map.of("type", "object")
                )
        ));

        Map<String, Object> toAdd = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("BaseResponse") && !key.equals("BaseResponse")) {
                Map<String, Object> schema = (Map<String, Object>) entry.getValue();

                if (schema.containsKey("properties")) {
                    Map<String, Object> props = (Map<String, Object>) schema.get("properties");
                    Object resultRef = props.get("result");

                    // BaseResponseXXXResponse → allOf 구조로 치환
                    Map<String, Object> newSchema = Map.of(
                            "allOf", List.of(
                                    Map.of("$ref", "#/components/schemas/BaseResponse"),
                                    Map.of(
                                            "type", "object",
                                            "properties", Map.of(
                                                    "result", resultRef
                                            )
                                    )
                            )
                    );

                    toAdd.put(key, newSchema);
                }
            }
        }

        // 치환 반영
        schemas.putAll(toAdd);
    }
}
