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

import com.dnastack.wes.model.wdl.WdlField;
import com.dnastack.wes.model.wdl.WdlValidationResponse;
import com.dnastack.wes.model.wdl.WdlTypeRepresentation;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * The WDL Value mapper is a class for marshling any JSON input value into the appropriate Java type corresponding to
 * the type defined within the WDL. This is necessary in order to ensure the proper types are delivered to cromwell and
 * are also properly escaped (Since cromwell does not handle all type conversions very well)
 * <p>
 * This class also optionally will map a File type input to the expected type depending on a fileMapper cass. For
 * Example, if only file id's are stored a mapper could potenially look up the ProjectFileDTO and embed it in the
 * response
 */
public class WdlFileProcessor {

    private Map<String, Object> inputs;

    @Getter
    private Map<String, Object> processedInputs;
    private WdlValidationResponse validationResponse;
    private Gson gson;
    private List<FileWrapper> mappedFiles;


    public WdlFileProcessor(Map<String, Object> inputs, WdlValidationResponse validationResponse) {
        this.inputs = inputs;
        this.validationResponse = validationResponse;
        this.gson = new Gson();
        this.mappedFiles = new ArrayList<>();
        processInputs();
    }


    public boolean requiresTransfer() {
        return mappedFiles.stream().map(FileWrapper::getRequiresTransfer).reduce(false, Boolean::logicalOr);
    }

    public Map<String, Object> getMappedFiles() {

        return mappedFiles.stream().filter(FileWrapper::getWasMapped)
            .collect(Collectors.toMap(FileWrapper::getMappedValue, (wrap) -> gson.fromJson(wrap.getOriginal(),Object.class)));
    }

    public void applyFileMapping(FileMapper mapper) {
        for (FileWrapper wrapper : mappedFiles) {
            if (mapper.shouldMap(wrapper)) {
                mapper.map(wrapper);
                wrapper.setWasMapped(true);
            }
        }
    }

    private Optional<WdlField> getInputField(String inputName) {
        return validationResponse.getWorkflowInputs().stream().filter(inp -> inp.getName().equals(inputName))
            .findFirst();
    }

    private void processInputs() {
        String jsonInputString = gson.toJson(inputs);
        JsonParser parser = new JsonParser();
        JsonObject jsonInputs = (JsonObject) parser.parse(jsonInputString);
        processedInputs = new HashMap<>();
        for (String key : jsonInputs.keySet()) {
            Optional<WdlField> optField = getInputField(key);
            if (optField.isPresent()) {
                WdlField field = optField.get();
                Object value = mapInput(field.getType(), jsonInputs.get(key));
                processedInputs.put(key, value);
            }
        }

    }

    private Object mapInput(WdlTypeRepresentation typeRepresentation, JsonElement inputValue) {

        String type = typeRepresentation.getType();
        if (type.equalsIgnoreCase("map")) {
            return mapMap(typeRepresentation, inputValue);
        } else if (type.equalsIgnoreCase("array")) {
            return mapArray(typeRepresentation, inputValue);
        } else if (type.equalsIgnoreCase("struct")) {
            return mapStruct(typeRepresentation, inputValue);
        } else if (type.equalsIgnoreCase("pair")) {
            return mapPair(typeRepresentation, inputValue);
        } else if (type.equalsIgnoreCase("boolean")) {
            return inputValue.getAsBoolean();
        } else if (type.equalsIgnoreCase("file")) {
            FileWrapper wrapper = new FileWrapper(inputValue);
            mappedFiles.add(wrapper);
            return wrapper;
        } else if (type.equalsIgnoreCase("string")) {
            return inputValue.getAsString();
        } else if (type.equalsIgnoreCase("int")) {
            return inputValue.getAsLong();
        } else if (type.equalsIgnoreCase("float")) {
            return inputValue.getAsFloat();
        } else {
            throw new UnsupportedOperationException("Could not map wdl type: " + type);
        }
    }

    private Object mapMap(WdlTypeRepresentation typeRepresentation, JsonElement value) {
        JsonObject inputValue = value.getAsJsonObject();
        Map<String, Object> outMap = new HashMap<>();
        WdlTypeRepresentation right = typeRepresentation.getItemTypes().get(1);

        inputValue.entrySet().forEach(set -> {
            String key = set.getKey();
            Object val = mapInput(right, set.getValue());
            outMap.put(key, val);
        });

        return outMap;
    }

    private Object mapPair(WdlTypeRepresentation typeRepresentation, JsonElement inputValue) {
        Map<String, Object> pair = new HashMap<>();
        JsonObject values = inputValue.getAsJsonObject();
        pair.put("left", mapInput(typeRepresentation.getItemTypes().get(0), values.get("left")));
        pair.put("right", mapInput(typeRepresentation.getItemTypes().get(1), values.get("right")));
        return pair;
    }

    private Object mapArray(WdlTypeRepresentation typeRepresentation, JsonElement inputValue) {
        List<Object> array = new ArrayList<>();
        JsonArray values = inputValue.getAsJsonArray();
        WdlTypeRepresentation innerType = typeRepresentation.getItemTypes().get(0);
        values.forEach(val -> array.add(mapInput(innerType, val)));
        return array;
    }

    private Object mapStruct(WdlTypeRepresentation typeRepresentation, JsonElement inputValue) {
        JsonObject object = inputValue.getAsJsonObject();
        Map<String, Object> structMap = new HashMap<>();

        object.entrySet().forEach(set -> {
            WdlTypeRepresentation memberType = typeRepresentation.getMembers().get(set.getKey());
            structMap.put(set.getKey(), mapInput(memberType, set.getValue()));
        });
        return structMap;
    }


}
