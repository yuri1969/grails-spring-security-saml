
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.release.scm.enabled = false

grails.project.dependency.resolution = {
    inherits ('global') {
		excludes "xml-apis"
	}
	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()
		mavenRepo "https://build.shibboleth.net/nexus/content/repositories/releases"
		mavenRepo "https://build.shibboleth.net/nexus/content/groups/public/"
		mavenCentral()
	}

    dependencies {
			build ("org.apache.ivy:ivy:2.2.0") {
				transitive = false
			}

			compile ('org.opensaml:opensaml:2.5.3') {
				transitive = false
			}
			compile ('org.opensaml:xmltooling:1.3.4') {
				transitive = false
			}
			compile ('org.apache.santuario:xmlsec:1.4.4') {
				transitive = false
			}
			compile ('org.bouncycastle:bcprov-jdk15:1.45') {
				transitive = false
			}
			compile ('org.opensaml:openws:1.4.4') {
				transitive = false
			}
			compile('joda-time:joda-time:1.6.2') {
				transitive = false
			}
			compile('commons-httpclient:commons-httpclient:3.1') {
				transitive = false
			}
			compile('org.apache.velocity:velocity:1.7') {
				transitive = false
			}
			compile ('ca.juliusdavies:not-yet-commons-ssl:0.3.9') {
				transitive = false
			}
			compile ('org.owasp.esapi:esapi:2.0.1')
			{
				transitive = false
			}
    }

    plugins {
			compile ":codenarc:0.18"
			test ":spock:0.7"
			test ":code-coverage:1.2.5"
			compile ":build-test-data:2.0.3"
			compile ":guard:1.0.7"
			build(":tomcat:$grailsVersion",
              ":hibernate:$grailsVersion") {
            export = false
        }
    }
}

//<editor-fold desc="Release Plugin External Maven Config">
def mavenConfigFile = new File("${basedir}/grails-app/conf/mavenInfo.groovy")
if (mavenConfigFile.exists()) {
	def slurpedMavenInfo = new ConfigSlurper().parse(mavenConfigFile.toURL())
	slurpedMavenInfo.grails.project.repos.each {k, v ->
		println "Adding maven info for repo $k"
		grails.project.repos."$k" = v
	}
}
else {
	println "No mavenInfo file found."
}
//</editor-fold>



//<editor-fold desc="CodeNarc Settings">
codenarc.processTestUnit = false
codenarc.processTestIntegration = false

codenarc.propertiesFile = 'grails-app/conf/codenarc.properties'
codenarc.ruleSetFiles = "rulesets/basic.xml,rulesets/exceptions.xml, rulesets/imports.xml,rulesets/grails.xml, rulesets/unused.xml, rulesets/concurrency.xml,rulesets/convention.xml,rulesets/design.xml,rulesets/groovyism.xml,rulesets/imports.xml,rulesets/logging.xml"

codenarc.reports = {
	MyXmlReport('xml') {
		// The report name "MyXmlReport" is user-defined; Report type is 'xml'
		outputFile = 'target/test-reports/CodeNarcReport.xml' // Set the 'outputFile' property of the (XML) Report
		title = 'CodeNarc' // Set the 'title' property of the (XML) Report
	}
	MyHtmlReport('html') {
		// Report type is 'html'
		outputFile = 'target/test-reports/CodeNarcReport.html'
		title = 'CodeNarc HTML'
	}
}
//</editor-fold>
