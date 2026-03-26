package org.openmrs.contrib.qaframework.rag;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PromptBuilder {

    public String buildPrompt(String queryContextString, String buildContextString, String queryString) {
        return "\n\nHuman:\n" + getSystemInstructionBlock() + "\n"
                + "=== EXISTING REPOSITORY CONTEXT === \n"
                + buildContextString + "\n"
                + queryContextString + "\n"
                + "=== END OF CONTEXT === \n"
                + "=== FEATURE FILE TO IMPLEMENT === \n"
                + queryString + "\n"
                + "=== END OF FEATURE FILE === \n"
                + "Based on the context above generate:\n" +
                "1. Each Gherkin step marked EXISTING or NEW\n" +
                "2. For NEW steps only — step definition method and any required page object method\n" +
                "3. Which page object class each new method belongs to\n" +
                "4. Any new test.properties keys required\n" +
                "\n" +
                "Output only valid Java code blocks and the step analysis. No explanatory prose.\n" +
                "\n" +
                "\n\nAssistant:";

    }

    private String getSystemInstructionBlock() {
        return "You are a senior Java test automation engineer.\n" +
                "\n" +
                "The following is always true for this project:\n" +
                "- Implementation language: Java\n" +
                "- Test framework: Cucumber BDD\n" +
                "- Java version for test code: Java 17 (Unless stated otherwise in the further context provided)\n" +
                "- Build tool: Maven\n" +
                "\n" +
                "Your task is to generate ONLY the missing Java implementation for the Gherkin \n" +
                "steps provided. Additional framework context — including dependency versions, \n" +
                "base classes, page object patterns and available test properties — is provided \n" +
                "in the repository context section below.\n" +
                "\n" +
                "You must:\n" +
                "1. Reuse existing step definitions exactly where they already exist in the context\n" +
                "2. Reuse existing page object methods exactly where they already exist in the context\n" +
                "3. Only generate new step definitions for steps with no existing implementation\n" +
                "4. Only generate new page object methods where no suitable method exists\n" +
                "5. Follow the exact same patterns, naming conventions and structure as the existing code\n" +
                "6. Never regenerate steps or methods that already exist in the context\n" +
                "7. Use Java 17 syntax for all generated code\n" +
                "8. Use Cucumber annotations from io.cucumber.java.en package\n";
    }

}
