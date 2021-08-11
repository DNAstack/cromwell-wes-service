package com.dnastack.wes.drs;

import com.dnastack.wes.shared.InvalidRequestException;
import com.dnastack.wes.wdl.ObjectTranslator;
import com.dnastack.wes.wdl.ObjectWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * A simple class for resolving <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.0.0/docs/#_drs_uris">DrsUris</a>
 * and converting the resulting Drs Object into corresonding Urls to pass to cromwell
 */
@Slf4j
public class DrsObjectResolver implements ObjectTranslator {

    private final static Pattern drsPattern = Pattern
        .compile("drs://(?<drsServer>[^/]*)/(?<objectId>[A-Za-z0-9.-_~]*)");
    private final static String HTTP_SCHEME = "http";
    private final DrsClient drsClient;
    private DrsConfig drsConfig;

    DrsObjectResolver(DrsConfig drsConfig, DrsClient drsClient) {
        this.drsConfig = drsConfig;
        this.drsClient = drsClient;
    }

    /**
     * Recursively resolve a single DrsUri. If the resulting object is a bundle, resolve all of the
     * Content's objects as DrsObjects and set the {@link DrsObject#setResolvedDrsContents(List)}. By default
     * This supports Drs V1
     *
     * @param drsUri
     *
     * @return
     */
    private DrsObject resolveDrsObject(String drsUri) {
        Matcher matcher = drsPattern.matcher(drsUri);
        if (matcher.find()) {
            String host = matcher.group("drsServer");
            String id = matcher.group("objectId");
            URI baseUri = URI.create(HTTP_SCHEME + "://" + host);
            DrsObject drsObject = drsClient.getObject(baseUri, id);
            return resolveDrsObject(drsObject);
        } else {
            throw new InvalidRequestException("Drs URI is not valid");
        }

    }

    private List<DrsObject> resolveContents(String parentUri, List<ContentsObject> contents) {
        List<DrsObject> resolvedObjects = new ArrayList<>();
        for (ContentsObject objectToResolve : contents) {
            if (objectToResolve.getId() != null) {
                String uri = URI.create(parentUri).resolve(objectToResolve.getId()).toString();
                resolvedObjects.add(resolveDrsObject(uri));
            } else if (objectToResolve.getDrsUri() != null && !objectToResolve.getDrsUri().isEmpty()) {
                DrsObject resolvedObject = null;
                Iterator<String> uris = objectToResolve.getDrsUri().iterator();
                while (resolvedObject == null && uris.hasNext()) {
                    try {
                        String drsUri = uris.next();
                        resolvedObject = resolveDrsObject(drsUri);
                    } catch (Exception e) {
                        log.warn("Encountered exception while resolved drs objects, will try next ID");
                    }
                }
                if (resolvedObject == null) {
                    throw new InvalidRequestException("Could not resolve DRS object");
                }

                resolvedObjects.add(resolveDrsObject(resolvedObject));
            } else if (objectToResolve.getContents() != null && !objectToResolve.getContents().isEmpty()) {

                DrsObject drsObject = new DrsObject();
                drsObject.setResolvedDrsContents(resolveContents(parentUri, contents));
                resolvedObjects.add(drsObject);
            }
        }

        return resolvedObjects.isEmpty() ? null : resolvedObjects;
    }

    private DrsObject resolveDrsObject(DrsObject drsObject) {
        if (drsObject.getContents() != null) {
            drsObject.setResolvedDrsContents(resolveContents(drsObject.getSelfUri(), drsObject.getContents()));
        }
        return drsObject;
    }

    /**
     * Extract an AccessUrl from the access method. Use the list of supported access types defined by the configuration
     * and resolve the URL in the order returned by the supported types. Currently, only access methods requiring no
     * headers, or accessId's are supported
     */
    private String extractUrlFromAccessMethods(List<AccessMethod> accessMethods) {
        for (AccessType accessType : drsConfig.getSupportedAccessTypes()) {
            List<AccessMethod> validMethodsType = accessMethods.stream()
                .filter(accessMethod -> accessMethod.getType().equals(accessType))
                .collect(Collectors.toList());

            for (AccessMethod validMethod : validMethodsType) {
                if (validMethod.getAccessUrl() != null) {
                    AccessURL url = validMethod.getAccessUrl();
                    if (url.getHeaders() == null || url.getHeaders().isEmpty()) {
                        return url.getUrl();
                    } else {
                        log.trace("Currently only access methods not requiring headers are supported");
                    }

                } else if (validMethod.getAccessId() != null) {
                    log.trace("Currently access methods requiring an accessID are not supported");
                }
            }
        }

        throw new InvalidRequestException("There are no supported access methods");

    }

    private JsonNode extractUrlsFromDrsObject(DrsObject drsObject) {
        if (drsObject.getAccessMethods() != null && !drsObject.getAccessMethods().isEmpty()) {
            String extractedUrl = extractUrlFromAccessMethods(drsObject.getAccessMethods());
            return new TextNode(extractedUrl);
        } else if (drsObject.getResolvedDrsContents() != null && !drsObject.getResolvedDrsContents().isEmpty()) {
            List<DrsObject> resolvedObjects = drsObject.getResolvedDrsContents();
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            for (DrsObject object : resolvedObjects) {
                arrayNode.add(extractUrlsFromDrsObject(object));
            }
            return arrayNode;

        } else {
            return NullNode.instance;
        }
    }

    @Override
    public JsonNode mapObjects(ObjectWrapper wrapper) {
        JsonNode node = wrapper.getOriginalValue();
        DrsObject drsObject = resolveDrsObject(node.textValue());
        return extractUrlsFromDrsObject(drsObject);
    }

    @Override
    public boolean shouldMap(ObjectWrapper wrapper) {
        return isDrsUri(wrapper.getOriginalValue());
    }

    private boolean isDrsUri(JsonNode element) {
        if (element.isTextual()) {
            String elemString = element.textValue();
            return drsPattern.matcher(elemString).find();
        }
        return false;
    }

}
