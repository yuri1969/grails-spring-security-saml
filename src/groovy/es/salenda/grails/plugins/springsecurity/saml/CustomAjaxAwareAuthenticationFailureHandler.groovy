package es.salenda.grails.plugins.springsecurity.saml

import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationFailureHandler
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.lang.exception.ExceptionUtils

import org.springframework.security.core.AuthenticationException

class CustomAjaxAwareAuthenticationFailureHandler extends AjaxAwareAuthenticationFailureHandler {
	/**
	 * {@inheritDoc}
	 * @see org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler#onAuthenticationFailure(
	 * 	javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
	 * 	org.springframework.security.core.AuthenticationException)
	 */
	@Override
	public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
			final AuthenticationException exception) throws IOException, ServletException {

		super.onAuthenticationFailure(request, response, exception)
		
		log.debug "${ ExceptionUtils.getStackTrace(exception) }"
	}
}
