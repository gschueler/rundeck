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

package com.dtolabs.rundeck.core.utils;

import org.apache.commons.lang.text.StrMatcher;

import java.util.*;

/**
 * tokenizes preserving detail about quote type used
 */
public class QuotedStringTokenizer2
    implements Iterator<QuotedStringTokenizer2.Token>, Iterable<QuotedStringTokenizer2.Token> {
    private char[]       string;
    private int          pos;
    private Queue<Token> buffer;
    private StrMatcher   delimiterMatcher;
    private StrMatcher   squoteMatcher;
    private StrMatcher   dquoteMatcher;
    private StrMatcher   whitespaceMatcher;
    private boolean      quashDelimiters;

    static enum Quoted {
        SINGLE,
        DOUBLE
    }

    private static class QuotedToken implements Token {
        String string;
        Quoted quoted;

        public QuotedToken(final String string, final Quoted quoted) {
            this.string = string;
            this.quoted = quoted;
        }

        @Override
        public String getString() {
            return string;
        }

        @Override
        public Optional<Quoted> getQuoted() {
            return Optional.ofNullable(quoted);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final QuotedToken that = (QuotedToken) o;
            return Objects.equals(string, that.string) &&
                   quoted == that.quoted;
        }

        @Override
        public String toString() {
            String q = quoted == Quoted.DOUBLE ? "\"" : quoted == Quoted.SINGLE ? "'" : "";
            return q + string + q;
        }

        @Override
        public int hashCode() {

            return Objects.hash(string, quoted);
        }
    }

    static interface Token {
        String getString();

        Optional<Quoted> getQuoted();

        static Token with(String string, Quoted quoted) {
            return new QuotedToken(string, quoted);
        }
    }

    public QuotedStringTokenizer2(String string) {
        this(string.toCharArray(), 0);
    }

    public QuotedStringTokenizer2(char[] chars, int pos) {
        this.string = chars;
        this.pos = pos;
        buffer = new ArrayDeque<>();
        delimiterMatcher = StrMatcher.trimMatcher();
        squoteMatcher = StrMatcher.singleQuoteMatcher();
        dquoteMatcher = StrMatcher.doubleQuoteMatcher();
        whitespaceMatcher = StrMatcher.trimMatcher();
        quashDelimiters = true;
        readNext();
    }

    public static Token[] tokenizeToArray(String string) {
        List<Token> strings = collectTokens(string);
        return strings.toArray(new Token[0]);
    }

    public static List<Token> tokenizeToList(String string) {
        return collectTokens(string);
    }

    public static Iterable<Token> tokenize(String string) {
        return new QuotedStringTokenizer2(string);
    }

    private static List<Token> collectTokens(String string) {
        ArrayList<Token> strings = new ArrayList<>();
        for (Token s : new QuotedStringTokenizer2(string)) {
            strings.add(s);
        }
        return strings;
    }


    @Override
    public boolean hasNext() {
        return !buffer.isEmpty();
    }

    @Override
    public Token next() {
        Token remove = buffer.remove();
        readNext();
        return remove;
    }

    private void readNext() {
        pos = readNextToken(string, pos, buffer);
    }

    private int readNextToken(char[] chars, int pos, Collection<Token> tokens) {
        if (pos >= chars.length) {
            return -1;
        }
        int ws = whitespaceMatcher.isMatch(chars, pos);
        if (ws > 0) {
            pos += ws;
        }
        if (pos >= chars.length) {
            return -1;
        }
        int delim = delimiterMatcher.isMatch(chars, pos);
        if (delim > 0) {
            if (quashDelimiters) {
                pos = consumeDelimiters(chars, pos, delim);
            } else {
                addToken(buffer, "", null);
                return pos + delim;
            }
        }
        int squote = squoteMatcher.isMatch(chars, pos);
        int dquote = dquoteMatcher.isMatch(chars, pos);
        return readQuotedToken(chars, pos, tokens, squote, dquote);
    }

    private int consumeDelimiters(char[] chars, int start, int delim) {
        while (delim > 0 && start < chars.length - delim) {
            start += delim;
            delim = delimiterMatcher.isMatch(chars, start);
        }
        return start;
    }

    private int readQuotedToken(char[] chars, int start, Collection<Token> tokens, int squotesize, int dquotesize) {
        int pos = start;
        StringBuilder tchars = new StringBuilder();
        Quoted quoted = squotesize > 0 ? Quoted.SINGLE : dquotesize > 0 ? Quoted.DOUBLE : null;
        boolean quoting = squotesize > 0 || dquotesize > 0;
        int quotesize = squotesize > 0 ? squotesize : dquotesize;
        if (quoting) {
            pos += squotesize > 0 ? squotesize : dquotesize;
        }
        while (pos < chars.length) {
            if (quoting) {
                if (charsMatch(chars, start, pos, quotesize)) {
                    //matches the quoting char

                    //if next token is the same quote, it is an escaped quote
                    if (charsMatch(chars, start, pos + quotesize, quotesize)) {
                        //token the quote
                        tchars.append(new String(chars, pos, quotesize));
                        pos += 2 * quotesize;
                        continue;
                    }
                    //end of quoting
                    quoting = false;
                    pos += quotesize;
                    continue;
                }
                //append char
                tchars.append(chars[pos++]);
            } else {
                int delim = delimiterMatcher.isMatch(chars, pos);
                if (delim > 0) {
                    if (quashDelimiters) {
                        pos = consumeDelimiters(chars, pos, delim);
                        addToken(tokens, tchars.toString(), quoted);
                        return pos;
                    } else {
                        addToken(tokens, tchars.toString(), quoted);
                        return pos + delim;
                    }
                }

                if (quotesize > 0 && charsMatch(chars, start, pos, quotesize)) {
                    //new quote
                    quoting = true;
                    pos += quotesize;
                    continue;
                }
                //append char
                tchars.append(chars[pos++]);
            }
        }
        addToken(tokens, tchars.toString(), quoted);
        return pos;
    }

    /**
     * @param chars char set
     * @param pos1  position 1
     * @param pos2  position 2
     * @param len2  length to compare
     * @return true if two sequences of chars match within the array.
     */
    private boolean charsMatch(char[] chars, int pos1, int pos2, int len2) {
        return charsMatch(chars, pos1, len2, pos2, len2);
    }

    /**
     * @param chars char set
     * @param pos1  pos 1
     * @param len1  length 1
     * @param pos2  pos 2
     * @param len2  length 2
     * @return true if two sequences of chars match within the array.
     */
    private boolean charsMatch(char[] chars, int pos1, int len1, int pos2, int len2) {
        if (len1 != len2) {
            return false;
        }
        if (pos1 + len1 > chars.length || pos2 + len2 > chars.length) {
            return false;
        }
        for (int i = 0; i < len1; i++) {
            if (chars[pos1 + i] != chars[pos2 + i]) {
                return false;
            }
        }
        return true;
    }

    private void addToken(Collection<Token> buffer, String token, Quoted quoted) {
        buffer.add(Token.with(token, quoted));
    }

    @Override
    public void remove() {
    }

    @Override
    public Iterator<Token> iterator() {
        return this;
    }
}
