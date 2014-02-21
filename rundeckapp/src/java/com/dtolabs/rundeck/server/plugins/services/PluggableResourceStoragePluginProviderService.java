package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.plugins.BasePluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.ResourceStoragePlugin;

/**
 * Pluggable service for ResourceStoragePlugin
 */
public class PluggableResourceStoragePluginProviderService extends BasePluggableProviderService<ResourceStoragePlugin> {
    public static final String SERVICE_NAME = ServiceNameConstants.ResourceStorage;
    private ServiceProviderLoader rundeckServerServiceProviderLoader;

    public PluggableResourceStoragePluginProviderService() {
        super(SERVICE_NAME, ResourceStoragePlugin.class);
    }

    @Override
    public ServiceProviderLoader getPluginManager() {
        return getRundeckServerServiceProviderLoader();
    }

    @Override
    public boolean isScriptPluggable() {
        //for now
        return false;
    }

    public ServiceProviderLoader getRundeckServerServiceProviderLoader() {
        return rundeckServerServiceProviderLoader;
    }

    public void setRundeckServerServiceProviderLoader(ServiceProviderLoader rundeckServerServiceProviderLoader) {
        this.rundeckServerServiceProviderLoader = rundeckServerServiceProviderLoader;
    }
}
