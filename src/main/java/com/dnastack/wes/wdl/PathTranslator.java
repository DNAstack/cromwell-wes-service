package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Given a path prefix
 */
public class PathTranslator implements ObjectTranslator {

    private Pattern prefix;
    private String replacement;
    private ObjectMapper mapper;

    public PathTranslator(String prefixRegex, String replacement) {
        prefix = Pattern.compile(prefixRegex);
        this.replacement = replacement;
        mapper = new ObjectMapper();
    }

    @Override
    public JsonNode mapObjects(ObjectWrapper wrapper) {
        JsonNode node = wrapper.getMappedvalue();
        return mapJsonNode(node);
    }


    public JsonNode mapJsonNode(JsonNode node) {
        if (node == null) {
            return null;
        } else if (node.isTextual()) {
            return mapString((TextNode) node);
        } else if (node.isObject()) {
            return mapJsonObject((ObjectNode) node);
        } else if (node.isArray()) {
            return mapJsonArray((ArrayNode) node);
        } else {
            return node;
        }
    }

    private JsonNode mapJsonObject(ObjectNode objectNode) {
        Iterator<Entry<String, JsonNode>> fields = objectNode.fields();
        ObjectNode newObjectNode = mapper.createObjectNode();
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            newObjectNode.set(entry.getKey(), mapJsonNode(entry.getValue()));
        }
        return newObjectNode;
    }

    private JsonNode mapJsonArray(ArrayNode arrayNode) {
        ArrayNode newArrayNode = mapper.createArrayNode();
        for (JsonNode node : arrayNode) {
            newArrayNode.add(mapJsonNode(node));
        }
        return newArrayNode;
    }

    private JsonNode mapString(TextNode textNode) {
        String text = textNode.asText();
        Matcher matcher = prefix.matcher(text);
        if (matcher.find()) {
            String newString = matcher.replaceFirst(replacement);
            textNode = new TextNode(newString);
        }
        return textNode;
    }

    public boolean shouldMapJsonNode(JsonNode node) {
        if (node == null) {
            return false;
        } else if (node.isTextual()) {
            return shouldMapString((TextNode) node);
        } else if (node.isObject()) {
            return shouldMapJsonObject((ObjectNode) node);
        } else if (node.isArray()) {
            return shouldMapJsonArray((ArrayNode) node);
        } else {
            return false;
        }
    }

    private boolean shouldMapJsonObject(ObjectNode objectNode) {
        Iterator<Entry<String, JsonNode>> fields = objectNode.fields();
        boolean match = false;
        while (fields.hasNext()) {
            Entry<String, JsonNode> entry = fields.next();
            match = match || shouldMapJsonNode(entry.getValue());
        }
        return match;
    }

    private boolean shouldMapJsonArray(ArrayNode arrayNode) {
        boolean match = false;
        for (JsonNode node : arrayNode) {
            match = match || shouldMapJsonNode(node);
        }
        return match;
    }

    private boolean shouldMapString(TextNode textNode) {
        Matcher matcher = prefix.matcher(textNode.asText());
        return matcher.find();
    }

    @Override
    public boolean shouldMap(ObjectWrapper wrapper) {
        return shouldMapJsonNode(wrapper.getMappedvalue());
    }
}
