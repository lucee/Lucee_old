/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
/**
 * Implements the CFML Function isuserinrole
 */
package lucee.runtime.functions.decision;

import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.security.Credential;
import lucee.runtime.security.CredentialImpl;

public final class IsUserInRole implements Function {
	public static boolean call(PageContext pc , Object object) throws PageException {
	    String[] givenRoles = CredentialImpl.toRole(object);
	    Credential ru = pc.getRemoteUser();
	    if(ru==null) return false;
	    String[] roles = ru.getRoles();
	    for(int i=0;i<roles.length;i++) {
	        for(int y=0;y<givenRoles.length;y++) {
	            if(roles[i].equalsIgnoreCase(givenRoles[y])) return true;
	        }
	    }
	    return false;
	}
}