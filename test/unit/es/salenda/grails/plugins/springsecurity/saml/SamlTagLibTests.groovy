package es.salenda.grails.plugins.springsecurity.saml

import grails.test.mixin.*
import org.junit.Before
import org.junit.Test
import org.junit.Ignore

@TestFor(SamlTagLib)
class SamlTagLibTests {

	@Test
	@Ignore("currently has a bug and requires rework (no yak shave)grta")
	void loginLinkRendersCorrectUrl() {
		def expectedLink = '<a href=\'/saml/login\'>login</a>'
		assert applyTemplate('<sec:loginLink>Logout</sec:loginLink>') == expectedLink
	}

	@Test
	void loginLinkShouldSetBody() {
		def body = "Login here"

		def expectedLink = "<a href=\'[:]?idp=null\'>${body}</a>"
		assert applyTemplate("<sec:loginLink>${body}</sec:loginLink>") == expectedLink
	}

	@Test
	void loginLinkShouldSetClassAttribute() {
		def expectedClass = 'loginBtn link'
		def expectedLink = "<a href=\'[:]?idp=null\' class=\'$expectedClass\'>Login</a>"
		assert applyTemplate("<sec:loginLink class=\'${expectedClass}\'>Login</sec:loginLink>") == expectedLink
	}

	@Test
	void loginLinkShouldSetIdAttribute() {
		def expectedId = 'loginBtn'

		def expectedLink = "<a href=\'[:]?idp=null\' id=\'$expectedId\'>Login</a>"
		assert applyTemplate("<sec:loginLink id=\'${expectedId}\'>Login</sec:loginLink>") == expectedLink
	}

	@Test
	void logoutLinkShouldRenderCorrectUrl() {
		def expectedLink = '<a href=\'/saml/logout\'>Logout</a>'
		assert applyTemplate('<sec:logoutLink>Logout</sec:logoutLink>') == expectedLink
	}

	@Test
	void logoutLinkShouldSetBody() {
		def body = "Logout here"

		def expectedLink = "<a href=\'/saml/logout\'>${body}</a>"
		assert applyTemplate("<sec:logoutLink>${body}</sec:logoutLink>") == expectedLink
	}

	@Test
	void logoutLinkShouldSetClassAttribute() {
		def expectedClass = 'logoutBtn link'

		def expectedLink = "<a href=\'/saml/logout\' class=\'$expectedClass\'>Logout</a>"
		assert applyTemplate("<sec:logoutLink class=\'${expectedClass}\'>Logout</sec:logoutLink>") == expectedLink
	}

	@Test
	void logoutLinkShouldSetIdAttribute() {
		def expectedId = 'logoutBtn'

		def expectedLink = "<a href=\'/saml/logout\' id=\'$expectedId\'>Logout</a>"
		assert applyTemplate("<sec:logoutLink id=\'${expectedId}\'>Logout</sec:logoutLink>") == expectedLink
	}
}