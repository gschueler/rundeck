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
package webhooks.importer

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.rundeck.core.projects.ProjectDataImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

class WebhooksProjectImporter implements ProjectDataImporter {

    private static final Logger logger = LoggerFactory.getLogger(WebhooksProjectImporter)

    def webhookService

    @Override
    String getSelector() {
        return "webhooks"
    }
    final String title = "Webhooks"
    final String titleCode = "rundeck.Webhook.projectExporter.title"
    final List<String> importPatterns = ['webhooks.yaml']
    final List<Property> properties = [
            PropertyBuilder.builder().booleanType('import')
            .defaultValue('true')
            .title("Import Webhooks")
            .description("Import webhooks into the project")
            .renderingOptions([
                    'booleanTrueDisplayValue':'Import webhooks',
                    'booleanFalseDisplayValue':'Do not import webhooks',
                    'booleanHasLabelColumn':'false'
            ]).build(),

            PropertyBuilder.builder().booleanType('regenerate')
                           .defaultValue('true')
                           .title("Regenerate Webhook Keys")
                           .description("When importing webhooks, regenerate the unique key")
                           .renderingOptions([
                                   'booleanTrueDisplayValue':'Regenerate webhook keys',
                                   'booleanFalseDisplayValue':'Do not regenerate webhook keys',
                                   'booleanHasLabelColumn':'false'
                           ]).build(),
    ]

    @Override
    List<String> doImport(final UserAndRolesAuthContext authContext, final String project, final File importFile, final Map importOptions) {
        logger.info("Running import for project webhooks")

        def errors = []
        Yaml yaml = new Yaml()
        def data = yaml.loadAs(new FileReader(importFile), HashMap.class)
        data.webhooks.each { hook ->
            logger.debug("Attempting to import" +
                         " hook: " + hook.name)
            if(hook.project != project) {
                hook.project = project //reassign project so that the hook will show up under the new project
            }
            def importResult = webhookService.importWebhook(authContext, hook, importOptions)
            if(importResult.msg) {
                logger.debug(importResult.msg)
            } else if(importResult.err) {
                logger.error(importResult.err)
                errors.add(importResult.err)
            }
        }
        return errors

    }

}
