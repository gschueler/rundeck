/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* URLNodesProvider.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 4:33 PM
* 
*/
package com.dtolabs.rundeck.core.resources.nodes;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdaterBuilder;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * URLNodesProvider is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class URLNodesProvider implements NodesProvider, Configurable {
    static final Logger logger = Logger.getLogger(URLNodesProvider.class.getName());
    public static final int DEFAULT_TIMEOUT = 30;
    final private Framework framework;
    private Configuration configuration;
    private File destinationTempFile;
    private File destinationCacheData;
    private String tempFileName;
    private Nodes.Format contentFormat;

    public URLNodesProvider(final Framework framework) {
        this.framework = framework;
    }

    public static class Configuration {
        public static final String URL = "url";
        public static final String PROJECT = "project";
        public static final String CACHE = "cache";
        public static final String TIMEOUT = "timeout";
        URL nodesUrl;
        String project;
        boolean useCache = true;
        int timeout = DEFAULT_TIMEOUT;

        private final Properties properties;

        Configuration() {
            properties = new Properties();
        }

        Configuration(final Properties configuration) {
            if (null == configuration) {
                throw new NullPointerException("configuration");
            }
            this.properties = configuration;
            configure();
        }

        private void configure() {
            if (properties.containsKey(URL)) {
                try {
                    nodesUrl = new URL(properties.getProperty(URL));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
            if (properties.containsKey(PROJECT)) {
                project = properties.getProperty(PROJECT);
            }
            if (properties.containsKey(CACHE)) {
                useCache = Boolean.parseBoolean(properties.getProperty(CACHE));
            }
            if (properties.containsKey(TIMEOUT)) {
                try {
                    timeout = Integer.parseInt(properties.getProperty(TIMEOUT));
                } catch (NumberFormatException e) {
                }
            }
        }

        void validate() throws ConfigurationException {

            if (null == project) {
                throw new ConfigurationException("project is required");
            }
            if (null == nodesUrl && properties.containsKey(URL)) {
                try {
                    nodesUrl = new URL(properties.getProperty(URL));
                } catch (MalformedURLException e) {
                    throw new ConfigurationException(e);
                }
                //assert allowed URL scheme
                if (!("file".equals(nodesUrl.getProtocol()) || "http".equals(nodesUrl.getProtocol()) || "https".equals(
                    nodesUrl.getProtocol()))) {
                    throw new ConfigurationException("URL protocol not allowed: " + nodesUrl.getProtocol());
                }

            }
            if (properties.containsKey(TIMEOUT)) {
                try {
                    timeout = Integer.parseInt(properties.getProperty(TIMEOUT));
                } catch (NumberFormatException e) {
                    throw new ConfigurationException(e);
                }
            }
        }

        Configuration(final Configuration configuration) {
            this(configuration.getProperties());
        }

        public Configuration url(final String url) {
            try {
                this.nodesUrl = new URL(url);
            } catch (MalformedURLException e) {
            }
            properties.setProperty("url", url);
            return this;
        }

        public Configuration project(final String project) {
            this.project = project;
            properties.setProperty(PROJECT, project);
            return this;
        }

        public Configuration cache(final boolean cache) {
            this.useCache = cache;
            properties.setProperty(CACHE, Boolean.toString(cache));
            return this;
        }

        public Configuration timeout(final int timeout) {
            this.timeout = timeout;
            properties.setProperty(TIMEOUT, Integer.toString(timeout));
            return this;
        }

        public static Configuration fromProperties(final Properties configuration) {
            return new Configuration(configuration);
        }

        public static Configuration clone(final Configuration configuration) {
            return fromProperties(configuration.getProperties());
        }

        public static Configuration build() {
            return new Configuration();
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = new Configuration(configuration);
        this.configuration.validate();
        //set destination temp file
        final FrameworkProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
            this.configuration.project);

        tempFileName = "url-" + this.configuration.nodesUrl.toExternalForm().hashCode() + ".temp";
        destinationTempFile = new File(frameworkProject.getBaseDir(), "var/urlNodesProvider/" + tempFileName);
        destinationCacheData = new File(frameworkProject.getBaseDir(),
            "var/urlNodesProvider/" + tempFileName + ".cache.properties");
        destinationTempFile.getParentFile().mkdirs();
    }

    public INodeSet getNodes() throws NodesProviderException {
        //update from URL if necessary
        URLFileUpdater updater = null;
        try {
            final URLFileUpdaterBuilder urlFileUpdaterBuilder = new URLFileUpdaterBuilder()
                .setUrl(configuration.nodesUrl)
                .setAcceptHeader("*/xml,*/yaml,*/yml")
                .setTimeout(configuration.timeout);
            if (configuration.useCache) {
                urlFileUpdaterBuilder
                    .setCacheMetadataFile(destinationCacheData)
                    .setCachedContent(destinationTempFile)
                    .setUseCaching(true);
            }
            updater = urlFileUpdaterBuilder.createURLFileUpdater();
            UpdateUtils.update(updater, destinationTempFile);

            logger.debug("Updated nodes resources file: " + destinationTempFile);
        } catch (UpdateUtils.UpdateException e) {
            if (!destinationTempFile.isFile() || destinationTempFile.length() < 1) {
                throw new NodesProviderException(
                    "Error updating from URL: " + configuration.nodesUrl + ": " + e.getMessage(), e);
            } else {
                logger.error("Error updating from URL: " + configuration.nodesUrl + ": " + e.getMessage(), e);
            }
        }
        final Nodes.Format format = determineFormat(null != updater ? updater.getContentType() : null);
        if (null != format) {
            contentFormat = format;
        }
        //parse file
        if (null == contentFormat) {
            throw new NodesProviderException("Unable to determine content format");
        }
        logger.debug("Determined URL content format: " + contentFormat);
        if (destinationTempFile.isFile() && destinationTempFile.length() > 0) {
            try {
                return FileNodesProvider.parseFile(destinationTempFile, contentFormat, framework,
                    configuration.project);
            } catch (ConfigurationException e) {
                throw new NodesProviderException(e);
            }
        } else {
            return new NodeSetImpl();
        }
    }


    private Nodes.Format determineFormat(final String contentType) {
        if (null != contentType) {
            if (contentType.endsWith("/xml")) {
                return Nodes.Format.resourcexml;
            } else if (contentType.endsWith("/yaml") || contentType.endsWith("/yml")) {
                return Nodes.Format.resourceyaml;
            }
        }
        return null;
    }

}
