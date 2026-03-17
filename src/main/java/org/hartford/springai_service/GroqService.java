package org.hartford.springai_service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GroqService {

    private final WebClient webClient;
    private final GroqConfig config;

    public GroqService(WebClient.Builder builder, GroqConfig config) {
        this.webClient = builder.build();
        this.config = config;
    }

    public String generateText(String prompt) {

        Map<String, Object> requestBody = Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        JsonNode response = webClient.post()
                .uri(URI.create(config.getUrl()))
                .header("Authorization", "Bearer " + config.getKey())
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> 
                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> 
                        Mono.error(new RuntimeException("Groq API Error: " + clientResponse.statusCode() + " - " + errorBody))
                    )
                )
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("choices")) {
            throw new RuntimeException("Invalid response from Groq API: " + response);
        }

        return response
                .get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();
    }
}
