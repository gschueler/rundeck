import org.gradle.api.*
import org.gradle.api.tasks.*
import org.yaml.snakeyaml.Yaml

class BuildManifestPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("buildManifest", BuildManifestExtension)
        project.task("verifyBuild", type: VerifyBuildManifest)
    }
}

class VerifyBuildManifest extends DefaultTask {

    File getManifestFile() {
        new File(project.buildManifest.manifestFile)
    }

    File getJunitOutputFile() {
        new File(project.buildManifest.junitOutput)
    }

    @TaskAction
    def validate() {

        Map manifest = null
        manifestFile.withInputStream {
            manifest = new Yaml().loadAs(it, Map)
        }
        if (!manifest || !manifest.files) {
            throw new Exception("One or more entries in `files` is required: $manifestFile")
        }

        Validate v = new Validate() {
            boolean valid = true

            void log(String t) { println "${t}" }

            void ok(String t) {
                if (debug) {
                    log "OK: $t"
                }
            }

            void warn(String t) { println "WARN: ${t}" }

            boolean fail(String t) {
                println "FAIL: ${t}"
                valid = false
                false
            }

            boolean require(String t, def v, boolean aff = false) {
                if (!v) {
                    fail(t)
                } else {
                    if (aff) {//affirm
                        log "OK: $t"
                    } else {
                        ok(t)
                    }
                    true
                }
            }

            boolean expect(String t, def v) {
                if (!v) {
                    warn(t)
                } else {
                    ok(t)
                }
                true
            }
        }
//process manifest
        manifest.each { fname, mfest ->
            def f = new File(fname)
            if (!v.require("[${fname}] MUST exist: ${f.exists()}", f.exists())) {
                return
            }
            if (!mfest) {
                return
            }
            def z = new java.util.zip.ZipFile(f)
            List<Check> checks = []
            def fverify = true
            mfest.each { path ->
                if (path instanceof Check) {
                    checks << path
                } else if (path instanceof Map) {

                    if (path.type == 'mustExist' || path.mustExist) {
                        checks << new MustExist(path.path, !!path.affirm)
                    } else if (path.type == 'count' || path.count) {
                        def count = path.all ? new Count(path.path, path.all) :
                                    path.max ? new Count(path.path, path.min ?: 0, path.max) :
                                    path.min ? new Count(path.path, path.min) : null
                        if (!count) {
                            throw new Exception("count: incorrect contents")
                        }
                        checks << count
                    } else if (path.type == 'match' || path.match) {
                        checks << new MustExist(path.path, !!path.affirm)
                    } else if (path.type == 'sha' || path.sha) {
                        checks << new SHASum(path.path, new File(path.compare))
                    } else if (path.type == 'stem' || path.stem) {
                        checks << new MustExist(path.path, !!path.affirm)

                    }
                } else {
                    checks << new MustExist(path, affirm)
                }
            }
            z.entries().each { e ->
                if (!e.isDirectory()) {
                    checks.each { check ->
                        check.update(z, e)
                    }
                }
            }
            checks.each { check ->
                fverify &= check.validate(f.name, v)
            }
            v.require("${f}: was${fverify ? '' : ' NOT'} verified", fverify, affirm)
        }

    }

}

class BuildManifestExtension {
    def String manifestFile = 'build-manifest.yaml'
    def String junitOutput = 'build/reports/build-manifest-junit.xml'
    def boolean junit = true
    def Map<String, Object> vars = [:]
}
