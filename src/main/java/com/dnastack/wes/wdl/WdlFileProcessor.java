/*-
 * #%
 * DNAstack - Utilities
 * %%
 * Copyright (C) 2014 - 2018 DNAstack
 * %%
 * All rights reserved.
 * %#
 */

package com.dnastack.wes.wdl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * The WDL Value Processor is a class for marshling any JSON input value into the appropriate Java type corresponding to
 * the type defined within the WDL. This is necessary in order to ensure the proper types are delivered to cromwell and
 * are also properly escaped (Since cromwell does not handle all type conversions very well)
 * <p>
 * This class also optionally will map a File type input to the expected type depending on a fileMapper cass. For
 * Example, if only file id's are stored a mapper could potenially look up the ProjectFileDTO and embed it in the
 * response
 */
public class WdlFileProcessor {

    private final List<ObjectTranslator> translators;
    private Map<String, Object> inputs;
    @Getter
    private Map<String, Object> processedInputs;
    private ObjectMapper mapper;
    @Getter
    private List<ObjectWrapper> mappedObjects;
    private Set<String> uploadedAttachments;

    public WdlFileProcessor(Map<String, Object> inputs, Set<String> uploadedAttachments, List<ObjectTranslator> translators) {
        this.inputs = inputs;
        this.mapper = new ObjectMapper();
        this.mappedObjects = new ArrayList<>();
        this.translators = translators;
        this.uploadedAttachments = uploadedAttachments;
        processInputs();
        applyTranslators();
    }


    /**
     * Apply the first ObjectTranslator (if present) to an object. The role of the object translator is to take the
     * provided object and convert it into a usable URI that can then be passed to the execution engine or used for
     * transfer. If the ObjectTranslator is present, the {@code wasMapped} attribute is set to true and the {@code
     * mappedValue} is set to the new mapped Url
     */
    private void applyTranslators() {
        if (translators != null) {
            for (ObjectWrapper wrapper : mappedObjects) {
                Optional<ObjectTranslator> optionalTranslator = translators.
                    stream().filter(translator -> translator.shouldMap(wrapper)).findFirst();

                if (optionalTranslator.isPresent()) {
                    ObjectTranslator translator = optionalTranslator.get();
                    JsonNode mappedObject = translator.mapObjects(wrapper);
                    wrapper.setWasMapped(true);
                    wrapper.setMappedValue(mappedObject);
                }
            }
        }
    }


    private void processInputs() {
        JsonNode node = mapper.valueToTree(inputs);
        processedInputs = new HashMap<>();
        node.fields().forEachRemaining(key -> {

            Object value = mapInput(key.getValue());
            processedInputs.put(key.getKey(), value);
        });
    }

    private Object mapInput(JsonNode inputValue) {

        if (inputValue.isArray()) {
            return mapArray(inputValue);
        } else if (inputValue.isObject()) {
            return mapObject(inputValue);
        } else if (inputValue.isTextual()) {
            return mapString(inputValue);
        } else {
            return inputValue;
        }
    }


    private Object mapObject(JsonNode value) {
        ObjectNode inputValue = (ObjectNode) value;
        Map<String, Object> outMap = new HashMap<>();

        inputValue.fields().forEachRemaining(set -> {
            String key = set.getKey();
            Object val = mapInput(set.getValue());
            outMap.put(key, val);
        });

        return outMap;
    }

    private Object mapArray(JsonNode inputValue) {
        List<Object> array = new ArrayList<>();
        ArrayNode values = (ArrayNode) inputValue;
        values.forEach(val -> array.add(mapInput(val)));
        return array;
    }

    private Object mapString(JsonNode value) {
        String valueString = value.textValue();
        if (isUri(valueString)) {
            ObjectWrapper wrapper = new ObjectWrapper(value);
            mappedObjects.add(wrapper);
            return wrapper;
        }
        return value;
    }

    private boolean isUri(String workflowUrl) {
        try {
            if (uploadedAttachments.contains(workflowUrl)) {
                return true;
            } else {
                URI uri = URI.create(workflowUrl);
                return (uri.getScheme() != null && uri.getHost() != null);
            }
        } catch (Exception e) {
            try {
                Path path = Paths.get(workflowUrl);
                return path.isAbsolute();
            } catch (Exception e2) {
                return false;
            }
        }
    }


}
