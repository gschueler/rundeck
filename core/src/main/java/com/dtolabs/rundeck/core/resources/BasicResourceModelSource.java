/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.function.Consumer;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.CODE_SYNTAX_MODE;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;
import static com.dtolabs.rundeck.plugins.util.DescriptionBuilder.buildDescriptionWith;

public abstract class BasicResourceModelSource
        implements ResourceModelSource
{
    final Framework framework;
    String description;
    String hostname;
    String osArch;
    String osName;
    String osVersion;
    String osFamily;
    Properties attributes;

    public BasicResourceModelSource(final Framework framework) {
        this.framework = framework;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        NodeSetImpl newNodes = new NodeSetImpl();
        final NodeEntryImpl node = createFrameworkNodeNew(getName(), getHostname());
        newNodes.putNode(node);
        return newNodes;
    }

    protected abstract String getName();

    protected String getHostname() {
        return hostname;
    }

    protected NodeEntryImpl createFrameworkNodeNew(String nodeName, String hostname) {
        NodeEntryImpl node = new NodeEntryImpl(hostname, nodeName);

        node.setDescription(description != null ? description : getDefaultNodeDescription());
        node.setOsArch(osArch != null ? osArch : getDefaultOsArch());
        node.setOsName(osName != null ? osName : getDefaultOsName());
        node.setOsVersion(osVersion != null ? osVersion : getDefaultOsVersion());
        node.setOsFamily(osFamily != null ? osFamily : getDefaultOsFamily());

        if (null != attributes) {
            for (Object o : attributes.keySet()) {
                if (o.toString().equals("tags")) {
                    String[] split = attributes.getProperty(o.toString()).split("\\s*,\\s*");
                    node.setTags(new HashSet<>(Arrays.asList(split)));

                } else {
                    node.setAttribute(o.toString(), attributes.getProperty(o.toString()));
                }
            }
        }
        return node;
    }

    protected String getDefaultOsFamily() {
        return "";
    }

    protected String getDefaultOsVersion() {
        return "";
    }

    protected String getDefaultOsName() {
        return "";
    }

    protected String getDefaultOsArch() {
        return "";
    }

    protected String getDefaultNodeDescription() {
        return "";
    }

    static Description createDescriptionWith(
            Consumer<DescriptionBuilder> builder,
            Consumer<DescriptionBuilder> postBuilder
    )
    {
        return buildDescriptionWith(
                d ->
                {
                    builder.accept(d);
                    d.property(
                            p -> p
                                    .string("description")
                                    .title("Description")
                                    .description("Description of the node")
                                    .defaultValue("")
                    )
                     .property(
                             p -> p
                                     .string("hostname")
                                     .title("Hostname")
                                     .description("Server hostname (default: via host OS)")
                     )
                     .property(
                             p -> p
                                     .freeSelect("osFamily")
                                     .title("OS Family")
                                     .values(Arrays.asList("unix", "windows"))
                                     .description("OS Family: unix, windows, ... (default:"
                                                  + " via host OS)")
                     )
                     .property(
                             p -> p
                                     .string("osName")
                                     .title("OS Name")
                                     .description("(default: via host OS)")
                     )
                     .property(
                             p -> p
                                     .string("osArch")
                                     .title("OS Architecture")
                                     .description("(default: via host OS)")
                     )
                     .property(
                             p -> p
                                     .string("osVersion")
                                     .title("OS Version")
                                     .description("(default: via host OS)")
                     )
                     .property(
                             p -> p
                                     .string("attributes")
                                     .renderingOption(DISPLAY_TYPE_KEY, "CODE")
                                     .renderingOption(CODE_SYNTAX_MODE, "properties")
                                     .title("Attributes")
                                     .description("Custom attributes, in Java properties "
                                                  + "format")
                     )
                     .metadata("faicon", "hdd");
                    if (null != postBuilder) {
                        postBuilder.accept(d);
                    }
                }
        );
    }

    protected void configure(final Properties configuration) throws ConfigurationException {
        description = configuration.getProperty("description");
        hostname = configuration.getProperty("hostname");
        osFamily = configuration.getProperty("osFamily");
        osVersion = configuration.getProperty("osVersion");
        osName = configuration.getProperty("osName");
        osArch = configuration.getProperty("osArch");
        String attributes = configuration.getProperty("attributes");
        if (null != attributes && !("".equals(attributes.trim()))) {
            Properties props = new Properties();
            try {
                props.load(new StringReader(attributes));
            } catch (IOException e) {
                throw new ConfigurationException("Cannot parse attributes text as Java Properties format: "
                                                 + e.getMessage(), e);
            }
            this.attributes = props;
        }
    }
}
