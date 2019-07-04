package com.dnastack.wes.transfer;


import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferConfig {

    private boolean enabled = false;

    private String stagingDirectory = null;

    //This whitelist identifies a set of prefixes which the
    private List<String> objectPrefixWhitelist = new ArrayList<>();

    private Integer numMonitoringThreads = 1;

    private String objectTransferUri = null;

    private String externalAccountUri = null;

}
