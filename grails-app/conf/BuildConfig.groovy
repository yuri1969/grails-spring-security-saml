grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.release.scm.enabled = false

grails.project.dependency.resolution = {
    inherits('global') {
    }
    log 'warn'

    repositories {
        grailsCentral()
        mavenCentral()
    }

    dependencies {

        compile 'org.springframework.security.extensions:spring-security-saml2-core:1.0.0.RELEASE'
    }

    plugins {

        test ":code-coverage:1.2.5"

        compile(":build-test-data:2.2.1",
                ":hibernate:3.6.10.17",
                ":spring-security-core:2.0-RC4") {
            export = false
        }

        build(":tomcat:7.0.54",
              ":release:3.0.1") {
            export = false
        }
    }
}

codenarc.reports = {
    CodeNarcReport('xml') {
        outputFile = 'target/test-reports/CodeNarcReport.xml'
        title = 'CodeNarc Report'
    }
}
