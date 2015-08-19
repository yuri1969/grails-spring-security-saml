/* Copyright 2006-2010 the original author or authors.
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
package es.salenda.grails.plugins.springsecurity.saml

import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import org.springframework.beans.BeanUtils
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.springframework.dao.DataAccessException

/**
 * A {@link GormUserDetailsService} extension to read attributes from a LDAP-backed 
 * SAML identity provider. It also reads roles from database
 *
 * @author alvaro.sanchez
 */
class SpringSamlUserDetailsService extends GormUserDetailsService implements SAMLUserDetailsService {
	// Spring bean injected configuration parameters
	String authorityClassName
	String authorityJoinClassName
	String authorityNameField
	Boolean samlAutoCreateActive
	Boolean samlAutoAssignAuthorities = true
	String samlAutoCreateKey
	Map samlUserAttributeMappings
	Map samlUserGroupToRoleMapping
	String samlUserGroupAttribute
	String userDomainClassName

	public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

		if (credential) {
			String username = getSamlUsername(credential)
			
			if (!username) {
				throw new UsernameNotFoundException("No username supplied in saml response.")
			}

			def user = generateSecurityUser(username)
			user = mapAdditionalAttributes(credential, user)

			if (user) {
				log.debug "Loading database roles for $username..."
				def authorities = getAuthoritiesForUser(credential)

				def grantedAuthorities = []

				if (samlAutoCreateActive) {
					user = saveUser(user.class, user, authorities)

					//TODO move to function
					Map whereClause = [:]
					whereClause.put "user", user
					Class<?> UserRoleClass = grailsApplication.getDomainClass(authorityJoinClassName)?.clazz
					UserRoleClass.withTransaction {
						def auths = UserRoleClass.findAllWhere(whereClause).collect { it.role }

						auths.each { authority ->
							grantedAuthorities.add(new SimpleGrantedAuthority(authority."$authorityNameField"))
						}
					}
				}
				else {
					grantedAuthorities = authorities
				}

				return createUserDetails(user, grantedAuthorities)
			} else {
				throw new InstantiationException('could not instantiate new user')
			}
		}
	}

	/**
	 * 
	 * Obtain username of logged user from SAML message. 
	 * 
	 * When the samlUserAttributeMappings contains key 'username' - use the SAML Attribute value. Otherwise use 'saml2:NameID' value.
	 * 
	 * @param credential
	 * @return
	 */
	protected String getSamlUsername(credential) {
		if (samlUserAttributeMappings?.username) {
			return credential.getAttributeAsString(samlUserAttributeMappings.username)
		} else {
			// if no mapping provided for username attribute then assume it is the returned subject in the assertion
			return credential.nameID?.value
		}
	}

	/**
	 * 
	 * Assign obtained SAML Attributes to the Spring Security user domain class fields. 
	 * 
	 * Keys in samlUserAttributeMappings are equal to fields in the user domain class. Values in the map are equal to 'Name' attributes in 'saml2:Attribute'.
	 * 
	 * @param credential
	 * @param user
	 * @return
	 */
	protected Object mapAdditionalAttributes(credential, user) {
		samlUserAttributeMappings.each { key, value ->
			// Note that check "user."$key" instanceof String" will fail when field value is null.
			//  Instead, we have to check field type
			Class keyType = grailsApplication.getDomainClass(userDomainClassName).properties.find { prop -> prop.name == "$key" }.type
			
			if (keyType != null && (keyType.isArray() || Collection.class.isAssignableFrom(keyType))) {
				def attributes = credential.getAttributeAsStringArray(value)
				
				attributes?.each() { attrValue ->
					if (!user."$key") {
						user."$key" = []
					}
					user."$key" << attrValue
				}
			} else {
				def attrValue = credential.getAttributeAsString(value)
				user."$key" = attrValue
			}
		}
		
		return user
	}

	protected Collection<GrantedAuthority> getAuthoritiesForUser(SAMLCredential credential) {
		Set<GrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>()

		def samlGroups = getSamlGroups(credential)

		samlGroups.each { groupName ->
			log.debug "Getting authorities for group '$groupName'..."

			// get mapped role from parsed incoming group
			def role = samlUserGroupToRoleMapping.get(groupName)
			def authority = getRole(role)

			if (authority) {
				authorities.add(new SimpleGrantedAuthority(authority."$authorityNameField"))
			}
		}

		// add a filler to empty authorities
		if ( authorities.size() == 0 ) {
			authorities.add(GormUserDetailsService.NO_ROLE)
		}

		return authorities
	}

	/**
	 * Extract the groups that the user is a member of from the saml assertion.
	 * Expects the saml.userGroupAttribute to specify the saml assertion attribute that holds 
	 * returned group membership data.
	 *
	 * Expects the group strings to be of the format "CN=groupName,someOtherParam=someOtherValue"
	 *
	 * @param credential
	 * @return list of groups
	 */
	protected List getSamlGroups(SAMLCredential credential) {
		def userGroups = []

		if (samlUserGroupAttribute) {
			def attributeValues = credential.getAttributeAsStringArray(samlUserGroupAttribute)
			attributeValues.each { groupString ->
				def groupStringValue = groupString

				if (groupString.startsWith("CN")) {
					groupString?.tokenize(',').each { token ->
						def keyValuePair = token.tokenize('=')
						if (keyValuePair.first() == 'CN') {
							groupStringValue = keyValuePair.last()
						}
					}
				}
				userGroups << groupStringValue
			}

		}

		return userGroups
	}

	private Object generateSecurityUser(username) {
		if (userDomainClassName) {
			Class<?> UserClass = grailsApplication.getDomainClass(userDomainClassName)?.clazz
			
			if (UserClass) {
				def user = BeanUtils.instantiateClass(UserClass)
				user.username = username
				user.password = "password"
				
				return user
			} else {
				throw new ClassNotFoundException("domain class ${userDomainClassName} not found")
			}
		} else {
			throw new ClassNotFoundException("security user domain class undefined")
		}
	}

	private def saveUser(userClazz, user, authorities) {
		if (userClazz && samlAutoCreateActive && samlAutoCreateKey && authorityNameField && authorityJoinClassName) {

			Map whereClause = [:]
			whereClause.put "$samlAutoCreateKey".toString(), user."$samlAutoCreateKey"
			Class<?> joinClass = grailsApplication.getDomainClass(authorityJoinClassName)?.clazz

			userClazz.withTransaction {
				def existingUser = userClazz.findWhere(whereClause)
				if (!existingUser) {
					if (!user.save()) {
						def save_errors=""
						user.errors.each { save_errors+=it }
						throw new UsernameNotFoundException("Could not save user ${user} - ${save_errors}");
					}
				} else {
					user = updateUserProperties(existingUser, user)

					if (samlAutoAssignAuthorities) {
						joinClass.removeAll user
					}
					user.save()
				}
				if (samlAutoAssignAuthorities) {
					authorities.each { grantedAuthority ->
						def role = getRole(grantedAuthority."${authorityNameField}")
						joinClass.create(user, role)
					}
				}

			}
		}
		
		return user
	}

	private Object updateUserProperties(existingUser, user) {
		samlUserAttributeMappings.each { key, value ->
			existingUser."$key" = user."$key"
		}
		
		return existingUser
	}

	/**
	 * 
	 * Create role object containing proper authority.
	 * 
	 * @param authority
	 * @return
	 */
	private Object getRole(String authority) {
		if (authority && authorityNameField && authorityClassName) {
			log.debug "Setting authority '$authority' for role domain class '$authorityClassName' with field '$authorityNameField'..."

			Class<?> Role = grailsApplication.getDomainClass(authorityClassName).clazz
			if (Role) {
				Map whereClause = [:]
				whereClause.put "$authorityNameField".toString(), authority
				Role.findWhere(whereClause)
			} else {
				throw new ClassNotFoundException("domain class ${authorityClassName} not found")
			}
		}
	}
}
