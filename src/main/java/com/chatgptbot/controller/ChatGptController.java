package com.chatgptbot.controller;

import com.chatgptbot.dto.ChatGptRequest;
import com.chatgptbot.dto.ChatGptResponse;
import com.chatgptbot.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/bot")
public class ChatGptController {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptController.class);

    @Value("${openai.model}")
    private String model;
    @Value("${openai.api.url}")
    private String url;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam("prompt") String prompt) {
        try {
            ChatGptRequest request = new ChatGptRequest(model, prompt);
            ResponseEntity<ChatGptResponse> response = sendRequest(request);
            return handleResponse(response);
        } catch (HttpClientErrorException ex) {
            return handleException(ex);
        } catch (Exception ex) {
            logger.error("Unexpected error occurred", ex);
            throw new RuntimeException("Unexpected error: " + ex.getMessage());
        }
    }

    private ResponseEntity<ChatGptResponse> sendRequest(ChatGptRequest request) {
        return restTemplate.postForEntity(url, request, ChatGptResponse.class);
    }

    private ResponseEntity<String> handleResponse(ResponseEntity<ChatGptResponse> response) {
        HttpStatusCode statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful() && response.getBody() != null) {
            return ResponseEntity.ok(response.getBody().getChoices().get(0).getMessage().getContent());
        } else {
            return handleErrorResponse(statusCode);
        }
    }

    private ResponseEntity<String> handleErrorResponse(HttpStatusCode statusCode) {
        if (statusCode.equals(UNAUTHORIZED)) {
            throw new ApplicationException("Unauthorized");
        } else if (statusCode.equals(NOT_FOUND)) {
            throw new ApplicationException("Not Found");
        } else if (statusCode.equals(TOO_MANY_REQUESTS)) {
            throw new ApplicationException("You reached out the chat gpt user limit. Please find the payment plans on our site.");
        }
        throw new RuntimeException("Unexpected error: " + statusCode);
    }

    private ResponseEntity<String> handleException(HttpClientErrorException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        if (statusCode.equals(UNAUTHORIZED)) {
            throw new ApplicationException("Unauthorized");
        } else if (statusCode.equals(NOT_FOUND)) {
            throw new ApplicationException("Not Found");
        } else if (statusCode.equals(TOO_MANY_REQUESTS)) {
            throw new ApplicationException("You reached out the chat gpt user limit. Please find the payment plans on our site.");
        }
        logger.error("Server error: {}", statusCode, ex);
        throw new RuntimeException("Server Error: " + statusCode);
    }
}
