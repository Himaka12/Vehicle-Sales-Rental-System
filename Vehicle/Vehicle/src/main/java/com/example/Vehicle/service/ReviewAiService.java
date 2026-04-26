package com.example.Vehicle.service;

import com.example.Vehicle.dto.ReviewAiResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class ReviewAiService {

    private static final Logger log = LoggerFactory.getLogger(ReviewAiService.class);

    private static final List<String> CRITICAL_KEYWORDS = List.of(
            "unsafe", "dangerous", "scam", "fraud", "harass", "harassment", "refund",
            "charged", "overcharge", "threat", "legal", "police", "accident",
            "brake", "engine failure", "urgent", "immediately", "emergency"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-3-flash-preview}")
    private String geminiModel;

    private volatile boolean missingKeyLogged = false;
    private volatile boolean geminiEnabledLogged = false;

    public ReviewAiResult analyzeReview(String vehicleName, int rating, String comment) {
        String apiKey = geminiApiKey == null ? "" : geminiApiKey.trim();

        if (apiKey.isBlank()) {
            if (!missingKeyLogged) {
                log.warn("Gemini review analysis is running in fallback mode because gemini.api.key is empty.");
                missingKeyLogged = true;
            }
            return heuristicAnalysis(rating, comment);
        }

        if (!geminiEnabledLogged) {
            log.info("Gemini review analysis is enabled with model '{}'.", geminiModel);
            geminiEnabledLogged = true;
        }

        try {
            String responseText = callGemini(apiKey, buildPrompt(vehicleName, rating, comment));
            ReviewAiResult parsed = parseGeminiResponse(responseText);
            if (parsed != null) {
                return normalizeResult(parsed);
            }
            log.warn("Gemini returned an empty or unparsable review analysis response. Falling back to local heuristic logic.");
        } catch (Exception exception) {
            log.warn("Gemini review analysis failed. Falling back to local heuristic logic. Reason: {}", exception.getMessage());
        }

        return heuristicAnalysis(rating, comment);
    }

    private String buildPrompt(String vehicleName, int rating, String comment) {
        return """
                You classify dealership rental review risk for follow-up.
                Review vehicle: %s
                Rating: %d/5
                Comment: %s

                Return JSON only with this exact shape:
                {
                  "sentiment": "POSITIVE | NEGATIVE | CRITICAL",
                  "requiresAdminAttention": true,
                  "aiReason": "short one-line explanation",
                  "adminAttentionReason": "only when critical, otherwise empty string"
                }

                Rules:
                - POSITIVE: satisfied, appreciative, low-risk feedback.
                - NEGATIVE: unhappy or disappointed feedback that does not need urgent manual intervention.
                - CRITICAL: safety issues, fraud/payment disputes, harassment, threats, legal/regulatory risk, severe service failure, or anything needing immediate human follow-up.
                - If sentiment is CRITICAL, requiresAdminAttention must be true.
                - If sentiment is POSITIVE or NEGATIVE, requiresAdminAttention must be false.
                - Do not include markdown or extra text.
                """.formatted(vehicleName == null ? "Unknown vehicle" : vehicleName, rating, comment == null ? "" : comment);
    }

    private String callGemini(String apiKey, String prompt) throws IOException, InterruptedException {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode contentNode = contents.addObject();
        ArrayNode parts = contentNode.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + apiKey))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini request failed with status " + response.statusCode() + " and body: " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.at("/candidates/0/content/parts/0/text").asText("");
    }

    private ReviewAiResult parseGeminiResponse(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return null;
        }

        String normalized = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            JsonNode node = objectMapper.readTree(normalized);
            return new ReviewAiResult(
                    node.path("sentiment").asText("NEGATIVE"),
                    node.path("requiresAdminAttention").asBoolean(false),
                    node.path("aiReason").asText("Review analyzed by Gemini."),
                    node.path("adminAttentionReason").asText("")
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private ReviewAiResult normalizeResult(ReviewAiResult result) {
        String sentiment = result.getSentiment() == null ? "NEGATIVE" : result.getSentiment().trim().toUpperCase(Locale.ROOT);
        boolean critical = "CRITICAL".equals(sentiment) || result.isRequiresAdminAttention();
        return new ReviewAiResult(
                critical ? "CRITICAL" : ("POSITIVE".equals(sentiment) ? "POSITIVE" : "NEGATIVE"),
                critical,
                blankToDefault(result.getAiReason(), critical ? "Critical feedback requires manual admin follow-up." : "Review analyzed by Gemini."),
                critical ? blankToDefault(result.getAdminAttentionReason(), "Critical customer feedback requires an admin response.") : ""
        );
    }

    private ReviewAiResult heuristicAnalysis(int rating, String comment) {
        String safeComment = comment == null ? "" : comment.toLowerCase(Locale.ROOT);
        boolean hasCriticalKeyword = CRITICAL_KEYWORDS.stream().anyMatch(safeComment::contains);

        if (hasCriticalKeyword || (rating <= 2 && containsUrgentComplaintTone(safeComment))) {
            return new ReviewAiResult(
                    "CRITICAL",
                    true,
                    "Critical complaint detected from rating and review wording.",
                    "This review mentions an urgent issue and needs a manual admin response."
            );
        }

        if (rating >= 4) {
            return new ReviewAiResult(
                    "POSITIVE",
                    false,
                    "Positive customer feedback detected.",
                    ""
            );
        }

        return new ReviewAiResult(
                "NEGATIVE",
                false,
                "Negative feedback detected but no urgent escalation is required.",
                ""
        );
    }

    private boolean containsUrgentComplaintTone(String comment) {
        return comment.contains("worst")
                || comment.contains("terrible")
                || comment.contains("awful")
                || comment.contains("unacceptable")
                || comment.contains("complaint")
                || comment.contains("disappointed")
                || comment.contains("bad service");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
