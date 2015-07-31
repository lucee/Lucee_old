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
package lucee.runtime.functions.cache;

import java.io.IOException;

import lucee.runtime.PageContext;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;
import lucee.runtime.op.Caster;

/**
 * 
 */
public final class CacheKeyExists implements Function {
	
	private static final long serialVersionUID = -5656876871645994195L;

	public static boolean call(PageContext pc, String key) throws PageException {
		return call(pc, key, null);
	}
	
	public static boolean call(PageContext pc, String key,String cacheName) throws PageException {
		try {
			return Util.getCache(pc,cacheName,Config.CACHE_TYPE_OBJECT).contains(Util.key(key));
		} catch (IOException e) {
			throw Caster.toPageException(e);
		}
	}
	
}