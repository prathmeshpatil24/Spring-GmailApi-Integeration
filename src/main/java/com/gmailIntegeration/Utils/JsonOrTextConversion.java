package com.gmailIntegeration.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.stereotype.Component;

@Component
public class JsonOrTextConversion {

    private static final ObjectMapper mapper = new ObjectMapper();

    public void printJsonOrText(String input) {
        if (input == null || input.trim().isEmpty()) {
            System.out.println("(Empty or null body)");
            return;
        }

        try {
            // Try parsing as JSON
            Object json = mapper.readValue(input, Object.class);
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            String prettyJson = writer.writeValueAsString(json);
            System.out.println("🟢 JSON Detected:");
            System.out.println(prettyJson);
        } catch (Exception e) {
            // Not JSON → print plain text
            System.out.println("🔵 Plain Text Detected:");
            System.out.println(input);
        }
    }
}
