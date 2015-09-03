/**
 * Copyright (c) 2014, the Railo Company Ltd.
 * Copyright (c) 2015, Lucee Assosication Switzerland
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
 */
package lucee.runtime;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.map.ReferenceMap;

import lucee.commons.collection.LongKeyList;
import lucee.commons.lang.SystemOut;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.dump.DumpUtil;
import lucee.runtime.dump.Dumpable;
import lucee.runtime.dump.SimpleDumpData;
import lucee.runtime.type.dt.DateTimeImpl;
import lucee.runtime.type.util.ArrayUtil;

import static org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength.SOFT;


/**
 * pool to handle pages
 */
public final class PageSourcePool implements Dumpable {
	
	private Map<Object,PageSource> pageSources=Collections.synchronizedMap(new ReferenceMap<Object,PageSource>(SOFT, SOFT));
	//timeout timeout for files
	private long timeout;
	//max size of the pool cache
	private int maxSize;
		
	/**
	 * constructor of the class
	 */
	public PageSourcePool() {
		this.timeout=10000;
		this.maxSize=1000;
	}
	
	/**
	 * return pages matching to key
	 * @param key key for the page
	 * @param updateAccesTime define if do update access time
	 * @return page
	 */
	public PageSource getPageSource(Object key,boolean updateAccesTime) {
		Object o=pageSources.get(key);
		if(o==null) return null;
		
		PageSource ps=(PageSource) o;
		if(updateAccesTime)ps.setLastAccessTime();
		return ps;
	}

	
	/**
	 * sts a page object to the page pool
	 * @param key key reference to store page object
	 * @param ps pagesource to store
	 */
	public void setPage(Object key, PageSource ps) {
		ps.setLastAccessTime();
		pageSources.put(key,ps);
	}
	
	/**
	 * returns if page object exists
	 * @param key key reference to a page object
	 * @return has page object or not
	 */
	public boolean exists(Object key) {
		return pageSources.containsKey(key);
	}
	
	/**
	 * @return returns a array of all keys in the page pool
	 */
	public Object[] keys() {
		return ArrayUtil.keys(pageSources);
	}
	
	/**
	 * removes a page from the page pool
	 * @param key key reference to page object
	 * @return page object matching to key reference
	 */
	public boolean remove(Object key) {
		return pageSources.remove(key)!=null;
	}
	
	/**
	 * @return returns the size of the pool
	 */
	public int size() {
		return pageSources.size();
	}
	
	/**
	 * @return returns if pool is empty or not
	 */
	public boolean isEmpty() {
		return pageSources.isEmpty();
	}
	
	/**
	 * clear unused pages from page pool
	 */
	public void clearUnused(ConfigImpl config) {
		
		SystemOut.printDate(config.getOutWriter(),"PagePool: "+size()+">("+maxSize+")");
		if(size()>maxSize) {
			Object[] keys=keys();
			LongKeyList list=new LongKeyList();
			for(int i=0;i<keys.length;i++) {
			    PageSource ps= getPageSource(keys[i],false);
				long updateTime=ps.getLastAccessTime();
				if(updateTime+timeout<System.currentTimeMillis()) {
					long add=((ps.getAccessCount()-1)*10000);
					if(add>timeout)add=timeout;
					list.add(updateTime+add,keys[i]);
				}
			}
			while(size()>maxSize) {
				Object key = list.shift();
				if(key==null)break;
				remove(key);
			}
		}
	}

	@Override
	public DumpData toDumpData(PageContext pageContext,int maxlevel, DumpProperties dp) {
		maxlevel--;
		Iterator<Object> it = pageSources.keySet().iterator();
		
		
		DumpTable table = new DumpTable("#FFCC00","#FFFF00","#000000");
		table.setTitle("Page Source Pool");
		table.appendRow(1,new SimpleDumpData("Count"),new SimpleDumpData(pageSources.size()));
		while(it.hasNext()) {
		    PageSource ps= pageSources.get(it.next());
		    DumpTable inner = new DumpTable("#FFCC00","#FFFF00","#000000");
			inner.setWidth("100%");
			inner.appendRow(1,new SimpleDumpData("source"),new SimpleDumpData(ps.getDisplayPath()));
			inner.appendRow(1,new SimpleDumpData("last access"),DumpUtil.toDumpData(new DateTimeImpl(pageContext,ps.getLastAccessTime(),false), pageContext,maxlevel,dp));
			inner.appendRow(1,new SimpleDumpData("access count"),new SimpleDumpData(ps.getAccessCount()));
			table.appendRow(1,new SimpleDumpData("Sources"),inner);
		}
		return table;
	}
	
	/**
	 * remove all Page from Pool using this classloader
	 * @param cl 
	 */
	public void clearPages(ClassLoader cl) {
		synchronized(pageSources){
			Iterator<Entry<Object, PageSource>> it = this.pageSources.entrySet().iterator();
			PageSourceImpl entry;
			while(it.hasNext()) {
				entry = (PageSourceImpl) it.next().getValue();
				if(cl!=null)entry.clear(cl);
				else entry.clear();
			}
		}
	}

	public void clear() {
		pageSources.clear();
	}
	public int getMaxSize() {
		return maxSize;
	}
}