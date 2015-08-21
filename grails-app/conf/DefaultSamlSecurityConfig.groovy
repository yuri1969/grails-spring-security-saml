security {
	saml {
		active = true
		afterLoginUrl = '/'
		afterLogoutUrl = '/'
		loginFormUrl = '/saml/login'
		authority { 
			nameField = '' // Name of field holding the authority. It is located in a domain class specified by authority.className. Eg.: authority
			className = '' // Domain class holding the Spring Security authority. Eg.: com.foo.bar.Role 
		}
		userLookup { 
			authorityJoinClassName = '' // Domain class joining Spring Security user with authority. Eg.: com.foo.bar.UserRole
			userDomainClassName = '' // Domain class holding the Spring Security user. Eg.: com.foo.bar.User 
		}
		userAttributeMappings = [:] // Map holding various 'Name' attributes inside 'saml2:Attribute' elements. Key called 'username' is used to map Attribute holding username ('saml2:NameID' otherwise). Other keys are equal to fields in Spring Security user class and are mapped from 'saml2:Attribute' elements.
		userGroupAttribute = "memberOf" // Value of 'Name' attribute inside 'saml2:Attribute' element where the user's groups are received. Eg.: 'http://wso2.org/claims/role'. It expects the group strings inside 'saml2:Attribute' to be of the LDAP format: "CN=groupName,someOtherParam=someOtherValue" or just plain "groupName,otherGroupName"
		userGroupToRoleMapping = [:] // Map holding group names and matching attributes. Eg.: [ 'groupName' : 'ROLE_FOO' ]
		responseSkew = 60 // seconds; Specify time window between the timestamps in the SAML Response and real time.
		maxAssertionTime = 3000 // seconds
		maxAuthenticationAge = 7200 // seconds
		failureHandler {
			defaultFailureUrl = '/login/authfail?login_error=1'
			useForward = false
			ajaxAuthFailUrl = '/login/authfail?ajax=true'
			exceptionMappings = [:]
		} 
		autoCreate {
			active =  false // Generate users in the DB as they are authenticated via SAML message.
			key = 'username'
			assignAuthorities = true // Assign the authorities that come from the SAML message.
		}
		metadata {
			generatorEnabled = false // enable metadata GUI generator
			defaultIdp = 'ping' // Default IdP provider
			url = '/saml/metadata'
			providers = [ ping :'security/idp-local.xml'] // Providers and path to their metadata file
			sp {
				file = 'security/sp.xml'
				defaults = [
					local: true, 
					alias: 'test',
					securityProfile: 'metaiop',
					signingKey: 'ping',
					encryptionKey: 'ping', 
					tlsKey: 'ping',
					requireArtifactResolveSigned: false,
					requireLogoutRequestSigned: false, 
					requireLogoutResponseSigned: false ]
			}
		}
		keyManager {
			storeFile = 'classpath:security/keystore.jks'
			storePass = 'nalle123'
			passwords = [ ping: 'ping123' ]
			defaultKey = 'ping'
		}
	}
}
