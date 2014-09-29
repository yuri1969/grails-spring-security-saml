package es.salenda.grails.plugins.springsecurity.saml

import grails.plugin.springsecurity.userdetails.GrailsUser
import spock.lang.Specification

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*
import grails.test.mixin.*

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl

import test.TestSamlUser

@TestFor(SamlSecurityService)
@Mock(TestSamlUser)
class SamlSecurityServiceSpec extends Specification {

    def grailsUser, authToken
    def service

    public void setup() {
        service = new SamlSecurityService()
        grailsUser = new GrailsUser('username', 'password', true, true, true, true, [], 1)

        authToken = new UsernamePasswordAuthenticationToken(grailsUser.username, null)
        authToken.setDetails(grailsUser)

        SamlSecurityService.metaClass.static.isLoggedIn = { -> true }
        SecurityContextHolder.metaClass.static.getContext = { -> new SecurityContextImpl() }
        SecurityContextImpl.metaClass.getAuthentication = { -> authToken }

        def samlUser = new TestSamlUser(username: grailsUser.username, password: 'password')
        assert samlUser.save()

    }

    def "getCurrentUser should return user from sesion when autocreate active flag is false"() {
        given:
        def fakeConfig = [saml: [autoCreate: [active: false]]]
        service.config = fakeConfig
        service.grailsApplication = grailsApplication

        when:
        def user = service.getCurrentUser()

        then:
        user instanceof GrailsUser
        user.username == grailsUser.username
    }

    void "getCurrentUser should return user from the database when autocreate active flag is true"() {
        given:
        def fakeConfig = [
                userLookup: [userDomainClassName: USER_CLASS_NAME],
                saml      : [autoCreate: [
                        active: true,
                        key   : 'username']
                ]
        ]

        service.config = fakeConfig
        service.grailsApplication = grailsApplication

        when:
        def user = service.getCurrentUser()

        then:
        user instanceof TestSamlUser
        assert user.username == grailsUser.username
    }

    void "getCurrentUser should return null when the user is not logged in"() {
        given:
        SamlSecurityService.metaClass.static.isLoggedIn = { -> false }

        expect:
        !service.getCurrentUser()
    }

    void "getCurrentUser should return null when autocreate active and details from session is null"() {
        given:
        def fakeConfig = [saml: [autoCreate: [active: true,]]]

        service.config = fakeConfig
        authToken.setDetails(null)
        expect:
        !service.getCurrentUser()
    }
}
