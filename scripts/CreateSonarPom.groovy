/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import groovy.xml.MarkupBuilder

/**
 * Gant script to create a POM for the Sonar Groovy Plugin.
 * Warning this will overwrite the existing pom.xml!
 *
 * @author Robin Bramley
 *
 * @version 0.1
 */

includeTargets << grailsScript("Init")
includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsEvents")


target(createPom: "Create a POM for Sonar") {
	// obtain the groupId
	String group
	if (!args) {
		ant.input addProperty: "pom.group", message: "Enter the groupId",
			defaultvalue: 'com.example'
		group = ant.antProject.properties.'pom.group'
	} else {
		group = args
	}
	group = group?.trim()

	// target location
	pomFile = "${grailsSettings.baseDir}/pom.xml"

	// Output the pom.xml
	def pomXML = new File(pomFile).withWriter { writer ->
		def xml = new MarkupBuilder(writer)
		xml.'project'('xmlns':'http://maven.apache.org/POM/4.0.0',
			'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
			'xsi:schemaLocation':'http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd') {
			modelVersion('4.0.0')
			groupId(group)
			artifactId(metadata.'app.name')
			version(metadata.'app.version')
			packaging('pom')
			name(metadata.'app.name')
			build {
				// TODO: walk grails-app/ to avoid packages called conf, controller, domain etc.
				sourceDirectory('src/groovy,src/java,grails-app/controllers,grails-app/services,grails-app/utils,grails-app/domain,grails-app/taglib')
			}
			properties {
				'sonar.language'('grvy')
				'sonar.dynamicAnalysis'(false)
			}
		}
	}

	event("StatusFinal", [ "POM for Sonar Groovy created: ${pomFile}"])
}

setDefaultTarget(createPom)