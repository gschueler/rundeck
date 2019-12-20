package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.CODE_SYNTAX_MODE;
import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.DISPLAY_TYPE_KEY;
import static com.dtolabs.rundeck.plugins.util.DescriptionBuilder.buildDescriptionWith;

public class LocalResourceModelSource
        extends BasicResourceModelSource
        implements ResourceModelSource
{


    public LocalResourceModelSource(final Framework framework) {
        super(framework);
    }

    @Override
    protected String getHostname() {
        return null != hostname ? hostname : framework.getFrameworkNodeHostname();
    }

    @Override
    protected String getName() {
        return framework.getFrameworkNodeName();
    }

    protected String getDefaultOsFamily() {
        final String s = System.getProperty("file.separator");
        return "/".equals(s) ? "unix" : "\\".equals(s) ? "windows" : "";
    }

    protected String getDefaultOsVersion() {
        return System.getProperty("os.version");
    }

    protected String getDefaultOsName() {
        return System.getProperty("os.name");
    }

    protected String getDefaultOsArch() {
        return System.getProperty("os.arch");
    }

    protected String getDefaultNodeDescription() {
        return "Rundeck server node";
    }

    static Description createDescription() {
        return BasicResourceModelSource.createDescriptionWith(
                d -> d
                        .name(LocalResourceModelSourceFactory.SERVICE_PROVIDER_TYPE)
                        .title("Local")
                        .description(
                                "Provides the local node as the single resource"),
                d -> d
                        .property(
                                p -> p
                                        .string("description")
                                        .title("Description")
                                        .description("Description of the local server node")
                                        .defaultValue("Rundeck server node")
                        ).property(d.property("hostname").description("Server hostname (default: via host OS)"))
                        .property(d
                                          .property("osFamily")
                                          .description("OS Family: unix, windows, ... (default: via host OS)"))
                        .property(d.property("osName").description("(default: via host OS)"))
                        .property(d.property("osArch").description("(default: via host OS)"))
                        .property(d.property("osVersion").description("(default: via host OS)"))
        );
    }

    public void configure(final Properties configuration) throws ConfigurationException {
        super.configure(configuration);
    }
}
