class Junit {
    def testsuites = [:]
    long suites = 0

    Testsuite testsuite(String name, String group = 'files') {
        if (testsuites[name]) {
            return new Testsuite(name: name, suite: testsuites[name])
        }
        def suite = [
            name     : name,
            package  : group,
            id       : suites++,
            failures : 0,
            tests    : 0,
            skipped  : 0,
            timestamp: new Date().toString(),
            testcases: []
        ]
        testsuites[name] = suite
        return new Testsuite(name: name, suite: suite)
    }

    public String toString() {
        def writer = new StringWriter()
        writeTo(writer)
        writer.toString()
    }

    public void writeTo(OutputStream out) {
        writeTo new OutputStreamWriter(out)
    }

    public void writeTo(Writer writer) {
        def mb = new groovy.xml.MarkupBuilder(writer)
        mb.testsuites {
            testsuites.values().each { suite ->
                def testcases = suite.remove('testcases')
                mb.testsuite(suite) {
                    testcases.each { tcase ->
                        def failure = tcase.remove('failure')
                        def sysout = tcase.remove('sysout')
                        def syserr = tcase.remove('syserr')
                        mb.testcase(tcase + [classname: 'testbuild']) {
                            if (failure) {
                                mb.'failure'(message: failure)
                            }
                            if (sysout) {
                                mb.'system-out'(sysout)
                            }
                            if (syserr) {
                                mb.'system-err'(syserr)
                            }
                        }
                    }
                }
            }
        }
        writer.close()
    }
}

class Testsuite {
    Map suite
    String name

    def ok(String tname) {
        suite.testcases << [name: tname]
        suite.tests++
    }

    def fail(String tname, String message) {
        suite.testcases << [name: tname, failure: tname + " did not pass", syserr: message - "[${name}]"]
        suite.failures++
        suite.tests++
    }

    def skip() {
        suite.skipped++
    }

    def test(tname, message, value) {
        if (value) {
            ok(tname)
        } else {
            fail(tname, message)
        }
    }
}
