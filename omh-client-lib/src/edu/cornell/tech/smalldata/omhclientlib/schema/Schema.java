package edu.cornell.tech.smalldata.omhclientlib.schema;

import edu.cornell.tech.smalldata.omhclientlib.AppConsts;

/**
 * Classes that implement this interface represent a json schema properties definitions by defining
 * static nested classes that implement {@link Property} interface.<BR>
 * Objects instantiated out of classes that implement this interface represent one measurement.<BR> 
 * Classes that implement this interface should override {@link #NAME} constant.
 */
public interface Schema {

	public static final String NAMESPACE = AppConsts.SCHEMA_NAMESPACE;
	public static final String VERSION = AppConsts.SCHEMA_VERSION;
	
	/**
	 * Classes implementing this interface should override this constant with actual schema name 
	 */
	public static final String NAME = "schema-name";
	
}
