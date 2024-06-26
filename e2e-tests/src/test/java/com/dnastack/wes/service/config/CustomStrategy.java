package com.dnastack.wes.service.config;

import com.dnastack.wes.service.util.EnvUtil;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfiguration;
import org.junit.platform.engine.support.hierarchical.ParallelExecutionConfigurationStrategy;


public class CustomStrategy implements ParallelExecutionConfiguration, ParallelExecutionConfigurationStrategy {

    public static final int JUNIT_PARALLELISM = EnvUtil.optionalEnv("JUNIT_PARALLELISM", Math.min(Runtime.getRuntime().availableProcessors(), 8),
        Integer::parseInt);

    @Override
    public int getParallelism() {
        return JUNIT_PARALLELISM;
    }

    @Override
    public int getMinimumRunnable() {
        // if upgrading to java 17, this needs to be set to 0 as of workaround: https://github.com/SeleniumHQ/selenium/issues/10113
        return 0;
    }

    @Override
    public int getMaxPoolSize() {
        return JUNIT_PARALLELISM;
    }

    @Override
    public int getCorePoolSize() {
        return JUNIT_PARALLELISM;
    }

    @Override
    public int getKeepAliveSeconds() {
        return 60;
    }

    @Override
    public ParallelExecutionConfiguration createConfiguration(final ConfigurationParameters configurationParameters) {
        return this;
    }

}
