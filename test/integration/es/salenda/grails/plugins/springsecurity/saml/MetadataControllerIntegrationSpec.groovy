package es.salenda.grails.plugins.springsecurity.saml

import grails.test.mixin.*
import spock.lang.Specification

class MetadataControllerIntegrationSpec extends Specification {

    void testSave() {
        given:
        MetadataController controller = new MetadataController()
        when:
        controller.params << [entityId: 'entityIdTest', alias: 'entityAlias', baseURL: 'https://localhost:54321/testsp',
        ]
        controller.save()

        then:
        controller.response.redirectedUrl == '/metadata/show?entityId='+'entityIdTest'
    }
}