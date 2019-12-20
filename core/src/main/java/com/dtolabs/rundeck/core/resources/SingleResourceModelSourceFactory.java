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
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.util.Properties;


@Plugin(name = SingleResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, service = "ResourceModelSource")
public class SingleResourceModelSourceFactory
        implements ResourceModelSourceFactory, Describable
{
    public static final String SERVICE_PROVIDER_TYPE = "single";
    private Framework framework;

    public SingleResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {
        final SingleResourceModelSource fileResourceModelSource = new SingleResourceModelSource(framework);
        fileResourceModelSource.configure(configuration);
        return fileResourceModelSource;
    }

    public Description getDescription() {
        return BasicResourceModelSource.createDescriptionWith(
                d ->
                        d.name(SingleResourceModelSourceFactory.SERVICE_PROVIDER_TYPE)
                         .title("Single")
                         .description("Define a single node")
                         .stringProperty(
                                 "name",
                                 null,
                                 true,
                                 "Name",
                                 "Node Name"
                         ),
                d -> d
                        .property(d.property("hostname").description("Server hostname"))
                        .property(d.property("osFamily").description("OS Family: unix, windows, ... "))
                        .property(d.property("osName").description(""))
                        .property(d.property("osArch").description(""))
                        .property(d.property("osVersion").description(""))
        );
    }
}
