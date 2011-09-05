package jrds.factories;

import java.lang.reflect.InvocationTargetException;

import jrds.Filter;
import jrds.FilterXml;
import jrds.factories.xml.CompiledXPath;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;

public class FilterBuilder extends ObjectBuilder {
	static final private Logger logger = Logger.getLogger(FilterBuilder.class);

	@Override
	Object build(JrdsNode n) throws InvocationTargetException {
		try {
			return makeFilter(n);
		} catch (SecurityException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalArgumentException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (NoSuchMethodException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (IllegalAccessException e) {
			throw new InvocationTargetException(e, FilterBuilder.class.getName());
		} catch (InstantiationException e) {
            throw new InvocationTargetException(e, FilterBuilder.class.getName());
        }
	}

	public Filter makeFilter(JrdsNode n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		FilterXml f = new FilterXml();
		n.setMethod(f, CompiledXPath.get("/filter/name"), "setName");
		n.setMethod(f, CompiledXPath.get("/filter/path"), "addPath", false);
		n.setMethod(f, CompiledXPath.get("/filter/tag"), "addTag", false);
		n.setMethod(f, CompiledXPath.get("/filter/qualifiedname"), "addGraph", false);
		doACL(f, n, CompiledXPath.get("/filter/role"));
		logger.trace("Filter loaded: " + f.getName());
		return f;
	}

}
