/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.cli.CLIUtils
import com.dtolabs.rundeck.core.data.MultiDataContextImpl
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.utils.OptsUtil
import spock.lang.Specification
import spock.lang.Unroll

class ExecArgListSpec extends Specification {
    @Unroll
    def "quotes preserved in string"() {
        given:
        def data = DataContextUtils.context('option', [warversion: '123'])
        def dataContext = new WFSharedContext([(ContextView.global()): data])
        String nodeName = 'node1'
        String osFamily = 'unix'
        when:
        def result = ExecArgList.fromStrings(
            DataContextUtils.stringContainsPropertyReferencePredicate.or(CLIUtils.&containsSpace),
            OptsUtil.burst(input)
        )
        def strings = result.buildCommandForNode(dataContext, nodeName, osFamily)
        then:
        strings == expected

        where:
        input                                                       | expected
        'scpt.sh -c --command="a /opt/deploy/${option.warversion}"' | ['scpt.sh -c --command="a /opt/deploy/123"']
        'echo " hi there "'                                         | ['echo " hi there "']
        'echo "cheese"'                                             | ['echo "cheese"']
    }
}
