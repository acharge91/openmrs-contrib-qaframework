package org.openmrs.contrib.qaframework.rag;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepoChunker {

    private final Path repoRootPath;

    public RepoChunker(String repoRootPath) {
        this.repoRootPath = Paths.get(repoRootPath);
    }

    public List<TextSegment> chunkRepo() {
        List<TextSegment> textChunks = new ArrayList<>();
        try (Stream<Path> walkFiles = Files.walk(repoRootPath)){
            walkFiles.filter(p -> p.toString().contains("qaframework-bdd-tests") &&
                            !p.toString().contains("/rag/") &&
                            !p.toString().contains("/target/") &&
                            (p.toString().contains("pom.xml") ||
                            (p.toString().contains("/src/") &&
                            (p.toString().endsWith(".java") ||
                            p.toString().endsWith(".properties") ||
                            p.toString().endsWith(".feature")))))
                    .forEach(path -> {
                textChunks.addAll(chunkFile(path));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return textChunks;
    }

    private List<TextSegment> chunkFile(Path path) {
        List<TextSegment> textChunks = new ArrayList<>();
        try {
            String fileType = classifyFile(path.toString());
            if (path.toString().endsWith(".java")) {
                CompilationUnit unit = StaticJavaParser.parse(path);
                textChunks.addAll(getJavaMetadata(unit, path, fileType));
            } else if (path.toString().endsWith(".properties")) {
                Files.readAllLines(path).stream()
                        .filter(l -> !l.startsWith("#") &&
                                !l.isBlank())
                        .forEach(l -> {
                                Metadata propMetadata = new Metadata();
                                propMetadata.put("file", path.getFileName().toString());
                                propMetadata.put("type", fileType);
                                propMetadata.put("key", l.split("=")[0]);
                                textChunks.add(new TextSegment(l, propMetadata));
                        });
            } else if (path.toString().endsWith(".feature")) {
                String featureText = "";
                String scenarioText;
                boolean inBackground = false;
                List<String> fullBackgroundList = new ArrayList<>();
                List<String> fullScenarioList = new ArrayList<>();
                Metadata metadata = new Metadata();
                List<String> fileLines = Files.readAllLines(path);
                for (String line : fileLines) {
                    line = line.trim();
                    if (line.startsWith("Feature")) {
                        featureText = line.split(":")[1].trim();
                    } else if (line.startsWith("Background:")) {
                        // flag that following steps belong to the background block
                        inBackground = true;
                    } else if ((line.startsWith("Scenario Outline") || line.startsWith("Scenario")) && !fullScenarioList.isEmpty()) {
                        // flush previous scenario — prepend background lines for full context
                        List<String> fullChunk = new ArrayList<>(fullBackgroundList);
                        fullChunk.addAll(fullScenarioList);
                        textChunks.add(new TextSegment(String.join("\n", fullChunk), metadata));
                        fullScenarioList.clear();
                        inBackground = false;
                    }
                    if ((line.startsWith("Scenario Outline") || line.startsWith("Scenario")) && fullScenarioList.isEmpty()) {
                        inBackground = false;
                        metadata = new Metadata();
                        scenarioText = line.split(":")[1].trim();
                        metadata.put("file", path.getFileName().toString());
                        metadata.put("type", fileType);
                        metadata.put("feature_name", featureText);
                        metadata.put("scenario_name", scenarioText);
                        metadata.put("background", String.join(" | ", fullBackgroundList));
                        fullScenarioList.add(line);
                    } else if (inBackground && !line.isBlank() && !line.startsWith("@")) {
                        // accumulate background steps
                        fullBackgroundList.add(line);
                    } else if (!line.startsWith("@") 
                            && !line.startsWith("Feature") 
                            && !line.startsWith("Scenario") 
                            && !line.startsWith("Background") && !line.isBlank()) {
                        fullScenarioList.add(line);
                    }
                }
                // flush the last scenario
                if (!fullScenarioList.isEmpty()) {
                    List<String> fullChunk = new ArrayList<>(fullBackgroundList);
                    fullChunk.addAll(fullScenarioList);
                    textChunks.add(new TextSegment(String.join("\n", fullChunk), metadata));
                }
            } else if (path.toString().contains("pom.xml")) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document pomDocument;
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    pomDocument = builder.parse(path.toFile());
                } catch (ParserConfigurationException | SAXException e) {
                    throw new RuntimeException(e);
                }
                Metadata propertiesMetadata = new Metadata();
                propertiesMetadata.put("type", classifyFile(path.toString()));
                propertiesMetadata.put("file", path.getFileName().toString());
                propertiesMetadata.put("section", "properties");
                HashMap<String, String> propertiesHashmap = getPropertiesHashMap(pomDocument);
                String propertiesString = propertiesHashmap.keySet().stream()
                        .map(key -> key + "=" + propertiesHashmap.get(key))
                        .collect(Collectors.joining("\n"));
                if (!propertiesString.isBlank()) {
                    TextSegment propertiesSegment = new TextSegment(propertiesString, propertiesMetadata);
                    textChunks.add(propertiesSegment);
                }
                NodeList dependencies = pomDocument.getElementsByTagName("dependency");
                for (int i = 0; i < dependencies.getLength(); i++) {
                    Metadata dependencyMetadata = new Metadata();
                    dependencyMetadata.put("type", classifyFile(path.toString()));
                    dependencyMetadata.put("file", path.getFileName().toString());
                    dependencyMetadata.put("section", "dependency");
                    TextSegment dependencySegment = new TextSegment(getDependencyData(dependencies.item(i), dependencyMetadata, propertiesHashmap), dependencyMetadata);
                    textChunks.add(dependencySegment);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return textChunks;
    }

    private String classifyFile(String path) {
        if (path.contains("Steps"))      return "step_definitions";
        if (path.contains("Page"))       return "page_object";
        if (path.contains("Actions"))    return "actions";
        if (path.contains("Utils"))      return "utility_class";
        if (path.contains("feature"))    return "feature_file";
        if (path.contains("properties")) return "properties";
        if (path.contains("TestData"))   return "test_data";
        if (path.contains("TestBase"))   return "test_base";
        if (path.contains("pom.xml"))    return "build_config";
        return "java_source";
    }

    private List<TextSegment> getJavaMetadata(CompilationUnit unit, Path path, String fileType) {
        List<TextSegment> textChunks = new ArrayList<>();
        String className = unit.findFirst(ClassOrInterfaceDeclaration.class)
                .map(NodeWithSimpleName::getNameAsString)
                .orElse("Unknown");
        List<MethodDeclaration> methodDecs = unit.findAll(MethodDeclaration.class);
        methodDecs.forEach(m -> {
            String methodCodeAsString = m.toString();
            String stepText = m.getAnnotations()
                    .stream()
                    .map(Node::toString)
                    .filter( a -> a.startsWith("@Given") ||
                            a.startsWith("@When") ||
                            a.startsWith("@Then") ||
                            a.startsWith("@And") ||
                            a.startsWith("@But"))
                    .findFirst().orElse("");
            Metadata javaMetadata = new Metadata();
            javaMetadata.put("file", path.getFileName().toString());
            javaMetadata.put("class", className);
            javaMetadata.put("method", m.getNameAsString());
            javaMetadata.put("type", fileType);
            javaMetadata.put("steptext", stepText);
            textChunks.add(new TextSegment(methodCodeAsString, javaMetadata));
        });
        return textChunks;
    }

    private HashMap<String, String> getPropertiesHashMap(Document document) {
        HashMap<String, String> propertiesHash = new HashMap<>();
        org.w3c.dom.Node propertiesNode = document.getElementsByTagName("properties").item(0);
        if (propertiesNode != null) {
            NodeList propertiesList = propertiesNode.getChildNodes();
            for (int i = 0; i < propertiesList.getLength(); i++) {
                if (propertiesList.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    propertiesHash.put(propertiesList.item(i).getNodeName(), propertiesList.item(i).getTextContent());
                }
            }
        }
        return propertiesHash;
    }

    private String getDependencyData(org.w3c.dom.Node dependencyNode, Metadata metadata, HashMap<String, String> propertiesHashmap) {
        ArrayList<String> attributeList = new ArrayList<>();
        String artifactId = "";
        org.w3c.dom.NodeList childNodes = dependencyNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            org.w3c.dom.Node attributeNode = childNodes.item(i);
            if(attributeNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                if (attributeNode.getTextContent().startsWith("${") && attributeNode.getTextContent().endsWith("}")) {
                    String placeHolderString = attributeNode.getTextContent().substring(2, attributeNode.getTextContent().length() - 1);
                    String actualAttribute = propertiesHashmap.getOrDefault(placeHolderString, attributeNode.getTextContent());
                    attributeList.add(attributeNode.getNodeName() + "=" + actualAttribute);
                    if (attributeNode.getNodeName().equals("artifactId")) {
                        artifactId = actualAttribute;
                    }
                } else {
                    attributeList.add(attributeNode.getNodeName() + "=" + attributeNode.getTextContent());
                    if (attributeNode.getNodeName().equals("artifactId")) {
                        artifactId = attributeNode.getTextContent();
                    }
                }
            }
        }
        String attributesString = String.join("\n", attributeList);
        metadata.put("artifactId", artifactId);

        return attributesString;
    }

}
