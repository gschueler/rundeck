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
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;

import java.util.Properties;

public class SingleResourceModelSource
        extends BasicResourceModelSource

{
    String name;

    public SingleResourceModelSource(final Framework framework) {
        super(framework);
    }

    @Override
    protected String getName() {
        return name;
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        super.configure(configuration);
        this.name = configuration.getProperty("name");
        if(null==name || "".equals(name.trim())){
            throw new ConfigurationException("name is required");
        }
    }
}
