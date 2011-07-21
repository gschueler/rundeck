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
* TestFileNodesProvider.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 9:08 AM
* 
*/
package com.dtolabs.rundeck.core.resources.nodes;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.Nodes;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * TestFileNodesProvider is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestFileNodesProvider extends AbstractBaseTest {
    public static final String PROJ_NAME = "TestFileNodesProvider";

    public TestFileNodesProvider(String name) {
        super(name);
    }

    public void setUp() {

        final Framework frameworkInstance = getFrameworkInstance();
        final FrameworkProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
            PROJ_NAME);
        File resourcesfile = new File(frameworkProject.getNodesResourceFilePath());
        //copy test nodes to resources file
        try {
            FileUtils.copyFileStreams(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                resourcesfile);
        } catch (IOException e) {
            throw new RuntimeException("Caught Setup exception: " + e.getMessage(), e);
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public void testConfigureProperties() throws Exception {
        final FileNodesProvider fileNodesProvider = new FileNodesProvider(getFrameworkInstance());
        try {
            fileNodesProvider.configure((Properties) null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        try {
            fileNodesProvider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        try {
            fileNodesProvider.configure(props);
            fail("shouldn't succeed");
        } catch (ConfigurationException e) {
            assertEquals("file is required", e.getMessage());
        }


    }
    public void testValidation() throws Exception {

        Properties props = new Properties();
        FileNodesProvider.Configuration config = new FileNodesProvider.Configuration(props);

        //missing project
        try{
            config.validate();
            fail("should not succeed");
        }catch (ConfigurationException e) {
            assertEquals("project is required", e.getMessage());
        }

        props.setProperty("project", PROJ_NAME);
        config = new FileNodesProvider.Configuration(props);
        //missing file
        try {
            config.validate();
            fail("should not succeed");
        } catch (ConfigurationException e) {
            assertEquals("file is required", e.getMessage());
        }


        props.setProperty("file", "src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        props.setProperty("format", "xml");
        config = new FileNodesProvider.Configuration(props);
        //invalid format
        try {
            config.validate();
            fail("should not succeed");
        } catch (ConfigurationException e) {
            assertEquals("format is not recognized: xml", e.getMessage());
        }

        props.setProperty("format", "yaml");
        config = new FileNodesProvider.Configuration(props);
        //invalid format
        try {
            config.validate();
            fail("should not succeed");
        } catch (ConfigurationException e) {
            assertEquals("format is not recognized: yaml", e.getMessage());
        }

        props.setProperty("format", "resourcexml");
        config = new FileNodesProvider.Configuration(props);
        //validation should succeed
        try {
            config.validate();

        } catch (ConfigurationException e) {
            fail("unexpected failure");
        }

        props.setProperty("format", "resourceyaml");
        config = new FileNodesProvider.Configuration(props);
        //validation should succeed
        try {
            config.validate();

        } catch (ConfigurationException e) {
            fail("unexpected failure");
        }
    }
    public void testConfiguration() throws Exception {

        try {
            FileNodesProvider.Configuration config = new FileNodesProvider.Configuration((Properties) null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Properties props = new Properties();
        FileNodesProvider.Configuration config = new FileNodesProvider.Configuration(props);
        assertNull(config.project);
        assertNull(config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("project", PROJ_NAME);
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNull(config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("format", "resourcexml");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourcexml, config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("format", "resourceyaml");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNull(config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("file", "src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        props.setProperty("generateFileAutomatically", "true");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertTrue(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);


        props.setProperty("generateFileAutomatically", "false");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);


        props.setProperty("includeServerNode", "true");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertTrue(config.includeServerNode);

        props.setProperty("includeServerNode", "false");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.project);
        assertEquals(PROJ_NAME, config.project);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);
        assertNotNull(config.nodesFile);
        assertEquals(new File("src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml"), config.nodesFile);
        assertFalse(config.generateFileAutomatically);
        assertFalse(config.includeServerNode);

        //test using file extension of file to determine format, using xml
        props.remove("format");
        props.setProperty("file", "src/test/com/dtolabs/rundeck/core/common/test-nodes1.xml");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourcexml, config.format);

        props.setProperty("file", "src/test/com/dtolabs/rundeck/core/common/test-nodes1.yaml");
        config = new FileNodesProvider.Configuration(props);
        assertNotNull(config.format);
        assertEquals(Nodes.Format.resourceyaml, config.format);


    }

    public void testConfigure2() throws Exception {

    }

    public void testGetNodes() throws Exception {

    }

    public void testGetNodes2() throws Exception {

    }

    public void testParseFile() throws Exception {

    }

    public void testParseFile2() throws Exception {

    }
}
