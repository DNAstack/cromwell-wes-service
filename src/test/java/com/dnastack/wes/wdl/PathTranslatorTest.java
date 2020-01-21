package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class PathTranslatorTest {


    @Test
    public void testSingleStringTranslatesProperly() {
        String prefix = "http://to-replace";
        String replacement = "replaced";
        String textPath = "http://to-replace/path/suffix";
        TextNode node = new TextNode(textPath);
        PathTranslator translator = new PathTranslator(prefix, replacement);

        JsonNode translatedNode = translator.mapJsonNode(node);
        Assert.assertTrue(translatedNode.asText().startsWith(replacement));
    }

    @Test
    public void testSingleStringShouldMap_returnsTrue() {
        String prefix = "/to-replace";
        String replacement = "replaced";
        String textPath = "/to-replace/path/suffix";
        TextNode node = new TextNode(textPath);
        PathTranslator translator = new PathTranslator(prefix, replacement);
        Assert.assertTrue(translator.shouldMapJsonNode(node));
    }

    @Test
    public void testSingleString_returnsOriginalValue_whenNoMatch() {
        String prefix = "/to-replace";
        String replacement = "replaced";
        String textPath = "/no-match/path/suffix";
        TextNode node = new TextNode(textPath);
        PathTranslator translator = new PathTranslator(prefix, replacement);

        JsonNode translatedNode = translator.mapJsonNode(node);
        Assert.assertEquals(translatedNode.asText(), textPath);
    }

    @Test
    public void testTranslateArray_OnlyTranslatesMatchingPaths() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode originalArray = mapper.valueToTree(Arrays.asList("path-to-translate", "path-to-leave"));
        PathTranslator translator = new PathTranslator("path-to-translate", "look-i-am-translated");

        Assert.assertTrue(translator.shouldMapJsonNode(originalArray));
        ArrayNode newArray = (ArrayNode) translator.mapJsonNode(originalArray);

        Assert.assertEquals(newArray.get(0).asText(), "look-i-am-translated");
        Assert.assertEquals(newArray.get(1).asText(), "path-to-leave");
    }

    @Test
    public void testTranslateObject_OnlyTranslatesMatchingPaths() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode originalNode = mapper.createObjectNode();

        originalNode.set("field-1", new TextNode("path-to-translate"));
        originalNode.set("field-2", new TextNode("path-to-leave"));
        PathTranslator translator = new PathTranslator("path-to-translate", "look-i-am-translated");
        Assert.assertTrue(translator.shouldMapJsonNode(originalNode));
        ObjectNode newObject = (ObjectNode) translator.mapJsonNode(originalNode);

        Assert.assertEquals(newObject.get("field-1").asText(), "look-i-am-translated");
        Assert.assertEquals(newObject.get("field-2").asText(), "path-to-leave");
    }

    @Test
    public void testTranslateNestedObject_OnlyTranslatesMatchingPaths() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode originalNode = mapper.createObjectNode();
        ObjectNode nestedNode1 = mapper.createObjectNode();
        ArrayNode nestedArray = mapper.createArrayNode();
        ObjectNode nestedNode2 = mapper.createObjectNode();

        nestedNode2.set("to-translate", new TextNode("path-to-translate/with-some-prefix"));
        nestedNode2.set("1", new IntNode(1));
        nestedArray.add(nestedNode2);
        nestedNode1.set("to-translate", nestedArray);
        nestedNode1.set("2", new IntNode(2));
        originalNode.set("to-translate", nestedNode1);
        originalNode.set("field-2", new TextNode("path-to-leave"));
        PathTranslator translator = new PathTranslator("path-to-translate", "look-i-am-translated");
        Assert.assertTrue(translator.shouldMapJsonNode(originalNode));
        ObjectNode newObject = (ObjectNode) translator.mapJsonNode(originalNode);

        Assert.assertFalse(newObject == originalNode);

        Assert.assertNotEquals(originalNode, newObject);
        String text = newObject.get("to-translate").get("to-translate").get(0).get("to-translate").textValue();
        Assert.assertEquals(text, "look-i-am-translated/with-some-prefix");

    }
}