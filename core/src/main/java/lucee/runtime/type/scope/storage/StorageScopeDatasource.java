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
package lucee.runtime.type.scope.storage;

import java.sql.SQLException;

import lucee.commons.io.log.Log;
import lucee.runtime.PageContext;
import lucee.runtime.PageContextImpl;
import lucee.runtime.config.Config;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.db.DataSource;
import lucee.runtime.db.DatasourceConnection;
import lucee.runtime.db.DatasourceConnectionPool;
import lucee.runtime.debug.DebuggerUtil;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Query;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.type.dt.DateTimeImpl;
import lucee.runtime.type.scope.ScopeContext;
import lucee.runtime.type.scope.storage.db.SQLExecutionFactory;
import lucee.runtime.type.scope.storage.db.SQLExecutor;
import lucee.runtime.type.util.KeyConstants;

/**
 * client scope that store it's data in a datasource
 */
public abstract class StorageScopeDatasource extends StorageScopeImpl {

	private static final long serialVersionUID = 239179599401918216L;

	public static final String PREFIX = "cf";
	
	private String datasourceName;

	private String appName;

	private String cfid;
	
	
	/**
	 * Constructor of the class
	 * @param pc
	 * @param name
	 * @param sct
	 * @param b 
	 */
	protected StorageScopeDatasource(PageContext pc,String datasourceName, String strType,int type,Struct sct) { 
		super(
				sct,
				doNowIfNull(pc,Caster.toDate(sct.get(TIMECREATED,null),false,pc.getTimeZone(),null)),
				doNowIfNull(pc,Caster.toDate(sct.get(LASTVISIT,null),false,pc.getTimeZone(),null)),
				-1, 
				type==SCOPE_CLIENT?Caster.toIntValue(sct.get(HITCOUNT,"1"),1):0,
				strType,type);

		this.datasourceName=datasourceName; 
		appName=pc.getApplicationContext().getName();
		cfid=pc.getCFID();
	}

	/**
	 * Constructor of the class, clone existing
	 * @param other
	 */
	protected StorageScopeDatasource(StorageScopeDatasource other,boolean deepCopy) {
		super(other,deepCopy);
		this.datasourceName=other.datasourceName;
	}
	
	private static DateTime doNowIfNull(PageContext pc,DateTime dt) {
		if(dt==null)return new DateTimeImpl(pc.getConfig());
		return dt;
	}
	
	
	
	
	protected static Struct _loadData(PageContext pc, String datasourceName,String strType,int type, Log log, boolean mxStyle) throws PageException	{
		ConfigImpl config = (ConfigImpl)pc.getConfig();
		DatasourceConnectionPool pool = config.getDatasourceConnectionPool();
		DatasourceConnection dc=pool.getDatasourceConnection(config,pc.getDataSource(datasourceName),null,null);
		SQLExecutor executor=SQLExecutionFactory.getInstance(dc);
		
		
		Query query;
		
		try {
			if(!dc.getDatasource().isStorage()) 
				throw new ApplicationException("storage usage for this datasource is disabled, you can enable this in the Lucee administrator.");
			query = executor.select(pc.getConfig(),pc.getCFID(),pc.getApplicationContext().getName(), dc, type,log, true);
		} 
		catch (SQLException se) {
			throw Caster.toPageException(se);
		}
	    finally {
	    	if(dc!=null) pool.releaseDatasourceConnection(config,dc,true);
	    }
	    
	    if(query!=null && pc.getConfig().debug()) {
	    	boolean debugUsage=DebuggerUtil.debugQueryUsage(pc,query);
	    	pc.getDebugger().addQuery(debugUsage?query:null,datasourceName,"",query.getSql(),query.getRecordcount(),((PageContextImpl)pc).getCurrentPageSource(null),query.getExecutionTime());
	    }
	    boolean _isNew = query.getRecordcount()==0;
	    
	    if(_isNew) {
	    	ScopeContext.info(log,"create new "+strType+" scope for "+pc.getApplicationContext().getName()+"/"+pc.getCFID()+" in datasource ["+datasourceName+"]");
			return null;
	    }
	    String str=Caster.toString(query.get(KeyConstants._data));
	    if(mxStyle) return null;
	    Struct s = (Struct)pc.evaluate(str);
	    ScopeContext.info(log,"load existing data from ["+datasourceName+"."+PREFIX+"_"+strType+"_data] to create "+strType+" scope for "+pc.getApplicationContext().getName()+"/"+pc.getCFID());
		
	    return s;
	}

	@Override
	public void touchAfterRequest(PageContext pc) {
		setTimeSpan(pc);
		super.touchAfterRequest(pc); 
		
		store(pc.getConfig());
	}
	
	@Override
	public void store(Config config) {
		//if(!super.hasContent()) return;
		
		DatasourceConnection dc = null;
		ConfigImpl ci = (ConfigImpl)config;
		DatasourceConnectionPool pool = ci.getDatasourceConnectionPool();
		Log log=((ConfigImpl)config).getLog("scope");
		try {
			PageContext pc = ThreadLocalPageContext.get();// FUTURE change method interface
			DataSource ds;
			if(pc!=null) ds=pc.getDataSource(datasourceName);
			else ds=config.getDataSource(datasourceName);
			dc=pool.getDatasourceConnection(null,ds,null,null);
			SQLExecutor executor=SQLExecutionFactory.getInstance(dc);
			executor.update(config, cfid,appName, dc, getType(), sct,getTimeSpan(),log);
		} 
		catch (Throwable t) {
			ScopeContext.error(log, t);
		}
		finally {
			if(dc!=null) pool.releaseDatasourceConnection(config,dc,true);
		}
	}
	
	@Override
	public void unstore(Config config) {
		ConfigImpl ci=(ConfigImpl) config;
		DatasourceConnection dc = null;
		
		
		DatasourceConnectionPool pool = ci.getDatasourceConnectionPool();
		Log log=((ConfigImpl)config).getLog("scope");
		try {
			PageContext pc = ThreadLocalPageContext.get();// FUTURE change method interface
			DataSource ds;
			if(pc!=null) ds=pc.getDataSource(datasourceName);
			else ds=config.getDataSource(datasourceName);
			dc=pool.getDatasourceConnection(null,ds,null,null);
			SQLExecutor executor=SQLExecutionFactory.getInstance(dc);
			executor.delete(config, cfid,appName, dc, getType(),log);
		} 
		catch (Throwable t) {
			ScopeContext.error(log, t);
		}
		finally {
			if(dc!=null) pool.releaseDatasourceConnection(ci,dc,true);
		}
	}
	
	

	
	
	
	
	
	

	@Override
	public void touchBeforeRequest(PageContext pc) {
		setTimeSpan(pc);
		super.touchBeforeRequest(pc);
	}
	
	@Override
	public String getStorageType() {
		return "Datasource";
	}

	/**
	 * @return the datasourceName
	 */
	public String getDatasourceName() {
		return datasourceName;
	}
}