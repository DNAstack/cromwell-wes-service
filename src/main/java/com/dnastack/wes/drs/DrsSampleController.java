package com.dnastack.wes.drs;

import com.dnastack.wes.drs.AccessMethod;
import com.dnastack.wes.drs.AccessType;
import com.dnastack.wes.drs.AccessURL;
import com.dnastack.wes.drs.CheckSum;
import com.dnastack.wes.drs.ContentsObject;
import com.dnastack.wes.drs.DrsObject;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ga4gh/drs/v1")
public class DrsSampleController {


    private Map<String, DrsObject> drsObjects = buildDrsObjects();

    @GetMapping("/objects/{objectId}")
    public DrsObject getObject(HttpServletRequest request, @PathVariable("objectId") String objectId) {
        if (drsObjects.containsKey(objectId)) {

            String drsUri = "drs://" + request.getServerName() + ":" + request.getServerPort() +  "/" + objectId;
            DrsObject object = drsObjects.get(objectId);
            object.setSelfUri(drsUri);
            return object;

        } else {
            throw new  IllegalArgumentException("Could not find");
        }

    }

    @GetMapping("/objects")
    public List<DrsObject> list() {
        return drsObjects.entrySet().stream().map(Entry::getValue).sorted(Comparator.comparing(DrsObject::getId)).collect(Collectors
            .toList());

    }

    private Map<String,DrsObject> buildDrsObjects() {
        Map<String,DrsObject> drsObjects = new HashMap<>();
        //Build Single Drs Object
        DrsObject singleObject = new DrsObject();
        singleObject.setId("1");
        singleObject.setName("Homo_sapiens_assembly38.fasta");
        singleObject
            .setCheckSums(Arrays.asList(CheckSum.builder().checksum("f/E0lT3MqMiZdFO7uAtrXg==").type("md5").build()));
        singleObject.setCreatedTime(ZonedDateTime.now().toString());
        singleObject.setUpdatedTime(ZonedDateTime.now().toString());
        singleObject.setAccessMethods(Arrays.asList(AccessMethod.builder().accessUrl(AccessURL.builder()
            .url("gs://genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta").build())
            .type(AccessType.gs).build()));
        drsObjects.put("1", singleObject);

        DrsObject singleObject2 = new DrsObject();
        singleObject2.setId("2");
        singleObject2.setName("Homo_sapiens_assembly38.dict");
        singleObject2
            .setCheckSums(Arrays.asList(CheckSum.builder().checksum("OITGLrDlP6kkWe2b/xM65g==").type("md5").build()));
        singleObject2.setCreatedTime(ZonedDateTime.now().toString());
        singleObject2.setUpdatedTime(ZonedDateTime.now().toString());
        singleObject2.setAccessMethods(Arrays.asList(AccessMethod.builder().accessUrl(AccessURL.builder()
            .url("gs://genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.dict").build())
            .type(AccessType.gs).build()));

        drsObjects.put("2", singleObject2);

        DrsObject singleObject3 = new DrsObject();
        singleObject3.setId("3");
        singleObject3.setName("Homo_sapiens_assembly38.fasta.fai");
        singleObject3
            .setCheckSums(Arrays.asList(CheckSum.builder().checksum("92NxsRNzSlbN4ja8A3LeCg==").type("md5").build()));
        singleObject3.setCreatedTime(ZonedDateTime.now().toString());
        singleObject3.setUpdatedTime(ZonedDateTime.now().toString());
        singleObject3.setAccessMethods(Arrays.asList(AccessMethod.builder().accessUrl(AccessURL.builder()
            .url("gs://genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta.fai").build())
            .type(AccessType.gs).build()));

        drsObjects.put("3", singleObject3);

        DrsObject bundledObject = new DrsObject();
        bundledObject.setId("4");
        bundledObject.setName("Hg38");
        bundledObject.setCreatedTime(ZonedDateTime.now().toString());
        bundledObject.setUpdatedTime(ZonedDateTime.now().toString());
        bundledObject.setContents(Arrays
            .asList(ContentsObject.builder().id("1").name(singleObject.getName()).build(), ContentsObject.builder()
                .id("2").name(singleObject2.getName()).build(), ContentsObject.builder().id("3")
                .name(singleObject3.getName())
                .build()));

        drsObjects.put(bundledObject.getId(), bundledObject);
        return drsObjects;

    }


}
