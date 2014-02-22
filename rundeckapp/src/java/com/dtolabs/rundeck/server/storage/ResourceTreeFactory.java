package com.dtolabs.rundeck.server.storage;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.storage.ResourceConverterPluginAdapter;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.ResourceTree;
import com.dtolabs.rundeck.core.storage.ResourceUtil;
import com.dtolabs.rundeck.core.utils.PropertyUtil;
import com.dtolabs.rundeck.plugins.storage.ResourceConverterPlugin;
import com.dtolabs.rundeck.plugins.storage.ResourceStoragePlugin;
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin;
import com.dtolabs.rundeck.server.plugins.PluginRegistry;
import com.dtolabs.rundeck.server.plugins.services.ResourceConverterPluginProviderService;
import com.dtolabs.rundeck.server.plugins.services.ResourceStoragePluginProviderService;
import org.apache.log4j.Logger;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.ResourceSelector;
import org.rundeck.storage.api.Tree;
import org.rundeck.storage.conf.TreeBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

/**
 * ResourceTreeFactory constructs Rundeck's ResourceTree based on the configuration in the framework.properties
 *
 * @author greg
 * @since 2/19/14 3:24 PM
 */
public class ResourceTreeFactory implements FactoryBean<ResourceTree>, InitializingBean {
    static Logger logger = Logger.getLogger(ResourceTreeFactory.class);
    public static final String TYPE = "type";
    public static final String PATH = "path";
    public static final String CONFIG = "config";
    public static final String SEP = ".";
    public static final String REMOVE_PATH_PREFIX = "removePathPrefix";
    public static final String RESOURCE_SELECTOR = "resourceSelector";
    Framework rundeckFramework;
    private PluginRegistry pluginRegistry;
    private String storageConfigPrefix ;
    private String converterConfigPrefix ;
    private String baseStorageType;
    private Map<String,String> baseStorageConfig = new HashMap<String, String>();

    private ResourceStoragePluginProviderService resourceStoragePluginProviderService;
    private ResourceConverterPluginProviderService resourceConverterPluginProviderService;

    //injected
    public void setRundeckFramework(Framework framework) {
        this.rundeckFramework = framework;
    }


    @Override
    public ResourceTree getObject() throws Exception {
        if (null == rundeckFramework) {
            throw new FactoryBeanNotInitializedException("'rundeckFramework' is required");
        }
        if (null == pluginRegistry) {
            throw new FactoryBeanNotInitializedException("'pluginRegistry' is required");
        }
        if (null == resourceStoragePluginProviderService) {
            throw new FactoryBeanNotInitializedException("'resourceStoragePluginProviderService' is required");
        }
        if (null == resourceConverterPluginProviderService) {
            throw new FactoryBeanNotInitializedException("'resourceConverterPluginProviderService' is required");
        }
        if (null == storageConfigPrefix) {
            throw new FactoryBeanNotInitializedException("'storageConfigPrefix' is required");
        }
        if (null == converterConfigPrefix) {
            throw new FactoryBeanNotInitializedException("'converterConfigPrefix' is required");
        }
        if (null == baseStorageType) {
            throw new FactoryBeanNotInitializedException("'baseStorageType' is required");
        }
        return ResourceUtil.asResourceTree(buildTree(rundeckFramework.getPropertyLookup().getPropertiesMap()));
    }

    @Override
    public Class<?> getObjectType() {
        return ResourceTree.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    private Tree<ResourceMeta> buildTree(Map configProps) {
        //configure the tree
        TreeBuilder<ResourceMeta> builder = TreeBuilder.builder();
        //set base using file storage, could be overridden
        Map<String, String> config1 = expandConfig(getBaseStorageConfig());
        logger.debug("Configuring base storage provider: " + getBaseStorageType() + ", " +
                "config: " + config1);
        ResourceStoragePlugin base = loadPlugin(
                getBaseStorageType(),
                config1,
                resourceStoragePluginProviderService
        );
        builder.base(base);
        Map<String, String> config = stringStringMap(rundeckFramework.getPropertyLookup().getPropertiesMap());
        int storeIndex = 1;

        while (configProps.containsKey(getStorageConfigPrefix() + SEP + storeIndex + SEP + TYPE)) {
            configureStoragePlugin(builder, storeIndex, config);
            storeIndex++;
        }
        if (1 == storeIndex) {
            logger.debug("No storage plugins configured with prefix " + getStorageConfigPrefix());
        }
        int converterIndex = 1;
        while (configProps.containsKey(getConverterConfigPrefix() + SEP + converterIndex + SEP + TYPE)) {
            builder=configureConverterPlugin(builder, converterIndex, config);
            converterIndex++;
        }
        if(1==converterIndex){
            logger.debug("No converter plugins configured with prefix "+getConverterConfigPrefix());
        }
        return builder.build();
    }

    private Map<String,String> stringStringMap(Map map) {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        for (Object o : map.keySet()) {
            stringStringHashMap.put(o.toString(), map.get(o).toString());
        }
        return stringStringHashMap;
    }

    /**
     * Configure converter plugins for the builder
     * @param builder builder
     * @param index given index
     * @param configProps configuration properties
     * @return builder
     */
    private TreeBuilder<ResourceMeta> configureConverterPlugin(TreeBuilder<ResourceMeta> builder, int index,
            Map<String,String> configProps) {
        String pref1 = getConverterConfigPrefix() + SEP + index;
        String pluginType = configProps.get(pref1 + SEP + TYPE);
        String pathProp = pref1 + SEP + PATH;
        String selectorProp = pref1 + SEP + RESOURCE_SELECTOR;
        String path = configProps.get(pathProp);
        String selector = configProps.get(selectorProp);
        if(null==path && null==selector) {
            throw new IllegalArgumentException("Converter plugin [" + index + "] specified by " + (pref1) + " MUST " +
                    "define one of: " +
                    pathProp + " OR " + selectorProp);
        }

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, configProps);
        config= expandConfig(config);
        logger.debug("Add Converter[" + index + "]:"
                + (null != path ? path : "/")
                +  "[" + (null != selector?selector:"*") + "]"
                + " " + pluginType + ", config: " + config);
        ResourceConverterPlugin converterPlugin = loadPlugin(
                pluginType,
                config,
                resourceConverterPluginProviderService
        );
        //convert tree under the subpath if specified, AND matching the selector if specified
        builder=builder.convert(new ResourceConverterPluginAdapter(converterPlugin),
                null != path ? PathUtil.asPath(path.trim()) : null,
                createResourceMetaSelector(selector));
        return builder;
    }

    /**
     * Selector syntax:<br/>
     * <pre>
     * key OP value [; key OP value]*
     * </pre>
     * OP can be "=" or "=~" to indicate a regular expression match.
     * @param selector the selector syntax string to parse
     * @return a resource selector corresponding to the parsed selector string
     */
    private ResourceSelector<ResourceMeta> createResourceMetaSelector(String selector) {
        if(null==selector || "*".equals(selector)){
            return null;
        }
        String[] split = selector.split(";");
        Map<String, String> values = new HashMap<String, String>();
        Map<String, String> regexes = new HashMap<String, String>();
        for (int i = 0; i < split.length; i++) {
            String s = split[i].trim();
            String[] split1 = s.split("=", 2);
            if(split1.length==2) {
                String key = split1[0].trim();
                String value = split1[1].trim();
                if (value.startsWith("~")) {
                    //regex
                    regexes.put(key, value.substring(1));
                }else {
                    values.put(key, value);
                }
            }
        }
        ResourceSelector<ResourceMeta> equalsSelector=null;
        ResourceSelector<ResourceMeta> regexSelector=null;

        if(values.size()>0) {
            equalsSelector = PathUtil.exactMetadataResourceSelector(values, true);
        }
        if(regexes.size()>0) {
            regexSelector  = PathUtil.regexMetadataResourceSelector(regexes, true);
        }
        if(null==equalsSelector){
            return regexSelector;
        }
        if(null==regexSelector){
            return equalsSelector;
        }
        return PathUtil.composeSelector(equalsSelector, regexSelector, true);
    }


    /**
     * Extract a map of the property values starting with the given prefix
     * @return map
     * @param configPrefix prefix
     * @param propertiesMap input
     */
    private Map<String, String> subPropertyMap(String configPrefix, Map propertiesMap) {
        Map<String, String> config = new HashMap<String, String>();
        for (Object o : propertiesMap.keySet()) {
            String key = (String) o;
            if (key.startsWith(configPrefix)) {
                String conf = key.substring(configPrefix.length());
                config.put(conf, propertiesMap.get(key).toString());
            }
        }
        return config;
    }

    /**
     * Configures storage plugins with the builder
     * @param builder builder
     * @param index current prop index
     * @param configProps configuration properties
     */
    private void configureStoragePlugin(TreeBuilder<ResourceMeta> builder, int index,Map<String,String> configProps) {
        String pref1 = getStorageConfigPrefix() + SEP + index;
        String pluginType = configProps.get(pref1 + SEP + TYPE);
        String path = configProps.get(pref1 + SEP + PATH);
        boolean removePathPrefix = Boolean.parseBoolean(configProps.get(pref1 + SEP +
                REMOVE_PATH_PREFIX));

        Map<String, String> config = subPropertyMap(pref1 + SEP + CONFIG + SEP, configProps);
        config = expandConfig(config);
        logger.debug("Add Storage["+index+"]:"+ path+" " + pluginType + ", " +
                "config: " + config);
        Tree<ResourceMeta> resourceMetaTree = loadPlugin(
                pluginType,
                config,
                resourceStoragePluginProviderService
        );
        if (index == 1 && "/".equals(path.trim())) {
            builder.base(resourceMetaTree);
        } else {
            builder.subTree(PathUtil.asPath(path.trim()), resourceMetaTree, !removePathPrefix);
        }
    }

    /**
     * Expand embedded framework property references in the map values
     * @param map map
     * @return expanded map
     */
    private Map<String, String> expandConfig(Map<String, String> map) {
        return expandAllProperties(map, rundeckFramework.getPropertyLookup().getPropertiesMap());
    }

    private Map<String, String> expandAllProperties(Map<String, String> source, Map values) {
        HashMap<String, String> result = new HashMap<String, String>();
        for (String s : source.keySet()) {
            result.put(s, PropertyUtil.expand(source.get(s), values));
        }
        return result;
    }

    private <T> T loadPlugin(String pluginType, Map<String, String> config,
            PluggableProviderService<T> service) {
        ConfiguredPlugin<T> configured = getPluginRegistry().configurePluginByName(pluginType, service,
                rundeckFramework, null, config);
        if (null == configured) {
            throw new IllegalArgumentException(service.getName() + " Plugin named \"" + pluginType + "\" could not be" +
                    " " +
                    "loaded");
        }
        return configured.getInstance();
    }

    public String getStorageConfigPrefix() {
        return storageConfigPrefix;
    }

    public void setStorageConfigPrefix(String storageConfigPrefix) {
        this.storageConfigPrefix = storageConfigPrefix;
    }

    public String getConverterConfigPrefix() {
        return converterConfigPrefix;
    }

    public void setConverterConfigPrefix(String converterConfigPrefix) {
        this.converterConfigPrefix = converterConfigPrefix;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    public void setPluginRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public ResourceConverterPluginProviderService getResourceConverterPluginProviderService() {
        return resourceConverterPluginProviderService;
    }

    public void setResourceConverterPluginProviderService(ResourceConverterPluginProviderService
            resourceConverterPluginProviderService) {
        this.resourceConverterPluginProviderService = resourceConverterPluginProviderService;
    }

    public ResourceStoragePluginProviderService getResourceStoragePluginProviderService() {
        return resourceStoragePluginProviderService;
    }

    public void setResourceStoragePluginProviderService(ResourceStoragePluginProviderService
            resourceStoragePluginProviderService) {
        this.resourceStoragePluginProviderService = resourceStoragePluginProviderService;
    }

    public String getBaseStorageType() {
        return baseStorageType;
    }

    public void setBaseStorageType(String baseStorageType) {
        this.baseStorageType = baseStorageType;
    }

    public Map<String, String> getBaseStorageConfig() {
        return baseStorageConfig;
    }

    public void setBaseStorageConfig(Map<String, String> baseStorageConfig) {
        this.baseStorageConfig = baseStorageConfig;
    }
}
