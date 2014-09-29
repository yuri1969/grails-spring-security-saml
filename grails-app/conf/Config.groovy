// configuration for plugin testing - will not be included in the plugin zip

log4j = {

    def datePattern = ["development", "test"].contains(grails.util.Environment.currentEnvironment.name) ? "" : "yyyy-MM-dd " // no need for date info when running locally
    def logPattern = pattern(conversionPattern:"%d{${datePattern}HH:mm:ss.SSS} %5p {%X{loggingId}} <%X{user}> [%X{hub} %X{device}] (%t) %c{2} - %m%n")

    console name:'stdout', layout:logPattern

    root {
        error 'stdout'
    }

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

}

grails {
	plugin {
		springsecurity {
			userLookup {
				userDomainClassName = 'test.TestSamlUser'
				usernamePropertyName = 'username'
				enabledPropertyName = 'enabled'
				passwordPropertyName = 'password'
				authoritiesPropertyName = 'roles'
				authorityJoinClassName = 'test.TestUserRole'
			}

			requestMap {
				className = 'test.TestRequestmap'
				urlField = 'urlPattern'
				configAttributeField = 'rolePattern'
			}

			authority {
				className = 'test.TestRole'
				nameField = 'auth'
			}
		}
	}
}

grails.doc.authors = 'Aaron J. Zirbes'
grails.doc.license = 'Apache License 2.0'
grails.doc.title = 'Spring Security SAML Plugin'
								
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

