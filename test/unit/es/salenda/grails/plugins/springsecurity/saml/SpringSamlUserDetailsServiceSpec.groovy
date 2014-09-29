package es.salenda.grails.plugins.springsecurity.saml

import grails.plugin.springsecurity.userdetails.GrailsUser
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.opensaml.saml2.core.impl.AssertionImpl
import org.opensaml.saml2.core.impl.NameIDImpl
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.SAMLCredential
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges
import test.TestRole
import test.TestSamlUser
import test.TestUserRole

import static es.salenda.grails.plugins.springsecurity.saml.UnitTestUtils.*

@TestFor(SpringSamlUserDetailsService)
@Mock([TestSamlUser, TestRole, TestUserRole])
class SpringSamlUserDetailsServiceSpec extends Specification {
    def credential, nameID, assertion, mockGrailsAplication, testRole, testRole2
    def service

    String username = "jackSparrow"
    Map detailsServiceSettings = [:]
    DefaultGrailsApplication grailsApplication

    public void setup() {
        service = new SpringSamlUserDetailsService()
        mockOutDefaultGrailsApplication()
        grailsApplication = new DefaultGrailsApplication()

        mockOutSpringSecurityUtilsConfig()
        mockWithTransaction()

        service.authorityClassName = ROLE_CLASS_NAME
        service.authorityJoinClassName = JOIN_CLASS_NAME
        service.authorityNameField = "authority"
        service.samlAutoCreateActive = false
        service.samlAutoCreateKey = null
        service.samlUserAttributeMappings = [username: USERNAME_ATTR_NAME]
        service.samlUserGroupAttribute = GROUP_ATTR_NAME
        service.samlUserGroupToRoleMapping = ['myGroup': ROLE]
        service.userDomainClassName = USER_CLASS_NAME
        service.grailsApplication = grailsApplication

        nameID = new NameIDImpl("", "", "")
        assertion = new AssertionImpl("", "", "")

        // This is what a SamlResponse will eventually be marshalled to
        credential = new SAMLCredential(nameID, assertion, null, null)
        credential.metaClass.getNameID = { [value: "$username"] }

        testRole = new TestRole(authority: ROLE)
        testRole2 = new TestRole(authority: "FAKEROLE2")

        // set default username to be returned in the saml response
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username])
    }

    void "loadUserBySAML should return a GrailsUser"() {
        given:
        def user = service.loadUserBySAML(credential)

        expect:
        assert user instanceof GrailsUser
    }

    void "loadUserBySAML should return NameID as the username when no mapping specified"() {
        given:
        service.samlUserAttributeMappings = [:]

        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": "someotherValue"])

        when:
        def user = service.loadUserBySAML(credential)
        then:
        user.username == username
    }

    void "loadUserBySAML should set username from the mapped saml attribute"() {
        when:
        def user = service.loadUserBySAML(credential)
        then:
        user.username == username
    }

    void "loadUserBySAML should raise an exception if username not supplied in saml response"() {
        given:
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": null])

        when:
        service.loadUserBySAML(credential)

        then:
        thrown UsernameNotFoundException
    }


    void "loadUserBySAML should return a user with the mapped role"() {
        given:
        testRole.save()
        and:
        setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": 'myUsername'])

        when:
        def user = service.loadUserBySAML(credential)
        then:
        user.authorities.size() == 1
        user.authorities.toArray()[0].authority == ROLE
    }

    void "loadUserBySAML should not persist the user if autocreate is not active"() {
        expect:
        TestSamlUser.count() == 0
        when:
        def userDetails = service.loadUserBySAML(credential)
        then:
        TestSamlUser.count() == 0
    }

    void "loadUserBySAML should persist the user when autocreate is active"() {
        given:
        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'

        expect:
        TestSamlUser.count() == 0

        when:
        def userDetails = service.loadUserBySAML(credential)

        then:
        TestSamlUser.count() == 1
        TestSamlUser.findByUsername(userDetails.username)
    }

    void "loadUserBySAML should set additional mapped attributes on the user"() {
        given:
        def emailAddress = "test@mailinator.com"
        def firstname = "Jack"
        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'

        service.samlUserAttributeMappings = [email: "$MAIL_ATTR_NAME", firstName: "$FIRSTNAME_ATTR_NAME"]
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username, "$MAIL_ATTR_NAME": emailAddress, "$FIRSTNAME_ATTR_NAME": firstname])
        when:
        def user = service.loadUserBySAML(credential)
        def samlUser = TestSamlUser.findByUsername(username)

        then:
        samlUser.email == emailAddress
        samlUser.firstName == firstname
    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should not persist a user that already exists"() {
        given:
        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'

        when:
        def user = new TestSamlUser(username: username, password: 'test')
        user.save()

        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles -> }

        then:
        TestSamlUser.count() == 1
        when:
        def userDetail = service.loadUserBySAML(credential)

        then:
        TestSamlUser.count() == 1
    }

    void "loadUserBySAML should raise valid exception for users in invalid states"() {
        given:
        def sharedEmail = "some.user@gmail.com"
        // email should be unique but we are going to try and save a user whose username has changed but email has not.
        def oldAccount = new TestSamlUser(username: "someUser", password: 'test', email: sharedEmail).save();

        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'
        service.samlUserAttributeMappings = [email: "$MAIL_ATTR_NAME"]
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username, "$MAIL_ATTR_NAME": sharedEmail])

        when:
        service.loadUserBySAML(credential)
        then:
        thrown UsernameNotFoundException

    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should persist the role for a new user"() {
        given:
        testRole.save()

        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'

        setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->
            // no roles to remove
            assert false
        }
        TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->
            assert userWithNoRoles.username == username
            assert role.authority == ROLE
        }

        when:
        def userDetail = service.loadUserBySAML(credential)

        then:
        userDetail
    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should update the roles for an existing user"() {
        given:
        testRole.save()

        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'

        setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

        when:
        def user = new TestSamlUser(username: username, password: 'test')
        user.save()

        def removedExistingRoles = false
        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->
            assert userWithRoles.username == user.username
            removedExistingRoles = true
        }

        def savedNewRoles = false
        TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->
            assert userWithNoRoles.username == user.username
            assert role.authority == ROLE
            savedNewRoles = true
        }

        def userDetail = service.loadUserBySAML(credential)
        then:
        removedExistingRoles
        savedNewRoles
    }


    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should  not update the roles for an existing user"() {
        given:
        testRole.save()

        service.samlAutoCreateActive = true
        service.samlAutoAssignAuthorities = false
        service.samlAutoCreateKey = 'username'

        setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

        when:
        def user = new TestSamlUser(username: username, password: 'test')
        user.save(failOnError: true)

        def removedExistingRoles = false
        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->
            assert userWithRoles.username == user.username
            removedExistingRoles = true
        }

        def savedNewRoles = false
        TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->
            assert userWithNoRoles.username == user.username
            assert role.authority == ROLE
            savedNewRoles = true
        }

        def userDetail = service.loadUserBySAML(credential)
        then:
        !removedExistingRoles
        !savedNewRoles
    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should still pull details from DB"() {
        given:
        testRole.save()
        testRole2.save()
        service.samlAutoCreateActive = true
        service.samlAutoAssignAuthorities = false
        service.samlAutoCreateKey = 'username'

        and:
        setMockSamlAttributes(credential, ["$GROUP_ATTR_NAME": "something=something,CN=myGroup", "$USERNAME_ATTR_NAME": username])

        when:
        def user = new TestSamlUser(username: username, password: 'test')
        user.save(failOnError: true)

        TestUserRole.create(user, testRole2)

        and:
        def removedExistingRoles = false
        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser userWithRoles ->
            assert userWithRoles.username == user.username
            removedExistingRoles = true
        }
        and:
        def savedNewRoles = false
        TestUserRole.metaClass.'static'.create = { TestSamlUser userWithNoRoles, TestRole role ->
            assert userWithNoRoles.username == user.username
            assert role.authority == ROLE
            savedNewRoles = true
        }

        def userDetail = service.loadUserBySAML(credential)
        then:
        !removedExistingRoles
        !savedNewRoles

        when:
        Set authorities = userDetail.getAuthorities()

        then:
        authorities.size() == 1
        authorities.iterator().next().authority == testRole2.authority

    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should set any mapped fields for a user"() {
        given:
        def emailAddress = "test@mailinator.com"
        def firstname = "Jack"

        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'
        service.samlUserAttributeMappings = [email: "$MAIL_ATTR_NAME", firstName: "$FIRSTNAME_ATTR_NAME"]
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username, "$MAIL_ATTR_NAME": emailAddress, "$FIRSTNAME_ATTR_NAME": firstname])


        def user = new TestSamlUser(username: username, password: 'test')
        assert user.save()

        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser samlUser -> }
        when:
        service.loadUserBySAML(credential)

        def updatedUser = TestSamlUser.findByUsername(username)
        then:
        updatedUser.email == emailAddress
        updatedUser.firstName == firstname
    }

    @ConfineMetaClassChanges(TestUserRole)
    void "loadUserBySAML should update mapped fields for a user"() {
        given:
        def intialEmail = 'myfirstmail@mailinator.com'
        def emailAddress = "test@mailinator.com"

        service.samlAutoCreateActive = true
        service.samlAutoCreateKey = 'username'
        service.samlUserAttributeMappings = [email: "$MAIL_ATTR_NAME"]
        setMockSamlAttributes(credential, ["$USERNAME_ATTR_NAME": username, "$MAIL_ATTR_NAME": emailAddress])
        TestUserRole.metaClass.'static'.removeAll = { TestSamlUser samlUser -> }

        def user = new TestSamlUser(username: username, password: 'test', email: intialEmail)
        assert user.save()
        when:
        service.loadUserBySAML(credential)

        def updatedUser = TestSamlUser.findByUsername(username)
        then:
        updatedUser.email == emailAddress
    }
}
