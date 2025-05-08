package com.inoit.aiemailgenerator.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inoit.aiemailgenerator.controller.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    @Value("${gemini.api.key}")
    private String geminiAPIKey;
    @Value("${gemini.api.url}")
    private String geminiUrl;


   private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClient) {
        this.webClient = webClient.build();
    }


    public String generateEmail(EmailRequest emailRequest) {
        //build the prompt
        String prompt = BuildPrompt(emailRequest);
        //craft a request
        Map<String,Object> requestBody = Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of(
                                        "text",prompt
                                )
                        })
                }
        );

        //do request to qet response

        System.out.println(geminiUrl + geminiAPIKey);
        System.out.println(emailRequest.emailContent());

        String response = webClient.post()
                .uri(geminiUrl+geminiAPIKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();



        // return response
        return extractResponse(response);
    }

    private String extractResponse(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String BuildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply to the following email without the subject  show interst  \n ");
        if (emailRequest.tone() != null && !emailRequest.tone().isEmpty()) {
            prompt.append("please use the following email tone: ").append(emailRequest.tone());
        }
        System.out.println(emailRequest.emailContent());
        prompt.append("Original Email content:\n").append(emailRequest.emailContent());
        return prompt.toString();

    }
}
