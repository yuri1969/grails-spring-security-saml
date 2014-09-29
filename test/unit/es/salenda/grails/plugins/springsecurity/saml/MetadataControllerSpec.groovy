package es.salenda.grails.plugins.springsecurity.saml

import grails.test.mixin.*
import spock.lang.Specification

@TestFor(MetadataController)
class MetadataControllerSpec extends Specification {

    def metadata


    void setup() {
        metadata = [hostedSPName: 'splocal', SPEntityNames: ['testsp'], IDPEntityNames: ['testidp']]
        controller.metadata = metadata
    }

    void testIndexReturnsMetadataValuesInModel() {
        given:
        when:
        def model = controller.index()

        then:
        model.hostedSP == metadata.hostedSPName
        model.spList == metadata.SPEntityNames
        model.idpList == metadata.IDPEntityNames
    }
}