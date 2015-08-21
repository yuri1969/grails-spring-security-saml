package es.salenda.grails.plugins.springsecurity.saml

import org.codehaus.groovy.grails.commons.GrailsApplication

class MetadataFilters {
	
	private static boolean generatorEnabled = false

	def filters = {
		all(controller:'metadata', action:'*') {
			before = {				
					// disable the metadata generation controller
					if(!generatorEnabled) {
						// HTTP 403
						log.info "Entered disabled metadata page!"
						response.sendError(response.SC_FORBIDDEN)
					}
			}
			after = { Map model ->

			}
			afterView = { Exception e ->

			}
		}
	}
}
