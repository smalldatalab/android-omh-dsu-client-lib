package edu.cornell.tech.smalldata.omhclientlib.schema;

import edu.cornell.tech.smalldata.omhclientlib.OmhClientLibConsts;

/**
 * Classes that implement this interface represent a json schema properties definitions by defining
 * static nested classes that implement {@link Property} interface.<BR>
 * Objects instantiated out of classes that implement this interface represent one measurement.<BR> 
 */
public interface Schema {

	public static final String NAMESPACE = OmhClientLibConsts.SCHEMA_NAMESPACE;
	public static final String VERSION = OmhClientLibConsts.SCHEMA_VERSION;
	
	public String getSchemaName();
	
}
