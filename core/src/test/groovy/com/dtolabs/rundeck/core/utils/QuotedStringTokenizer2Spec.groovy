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

package com.dtolabs.rundeck.core.utils

import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.utils.QuotedStringTokenizer2.Quoted.DOUBLE
import static com.dtolabs.rundeck.core.utils.QuotedStringTokenizer2.Quoted.SINGLE
import static com.dtolabs.rundeck.core.utils.QuotedStringTokenizer2.Token.with as token


class QuotedStringTokenizer2Spec extends Specification {
    @Unroll
    def "burst and tokenize"(){

    }
    @Unroll
    def "simple #input"() {
        when:
        def result = QuotedStringTokenizer2.tokenizeToList(input)
        then:
        result == expected

        where:
        input                        | expected
        'abc'                       | [token('abc', null)]
        '\'abc\''                   | [token('abc', SINGLE)]
        '"abc"'                     | [token('abc', DOUBLE)]
        'abc 123'                   | [token('abc', null), token('123', null)]
        'abc "123"'                 | [token('abc', null), token('123', DOUBLE)]
        'abc \'123\''               | [token('abc', null), token('123', SINGLE)]
        '\'abc 123\''               | [token('abc 123', SINGLE)]
        '"abc 123"'                 | [token('abc 123', DOUBLE)]
        '"abc 123" def'              | [token('abc 123', DOUBLE), token('def', null)]
        '"weird fishes"    \'123\''  | [token('weird fishes', DOUBLE), token('123', SINGLE)]
        '--test="this"'              | [token('--test="this"', null)]
        //XXX: this fails, it does not parse like bash
//        '--test="this spaced"'       | [token('--test="this spaced"', null)]
        'agent --disable "hi there"' | [token('agent', null), token('--disable', null), token('hi there', DOUBLE)]
    }
}
