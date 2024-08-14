package jrds.starter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.ConnectionName;
import lombok.Getter;

public class ConnectionInfo<C extends Connection<O>, O> {

    static final private Logger logger = LoggerFactory.getLogger(ConnectionInfo.class);

    private final Object[] args;
    private final Map<String, String> beansValue;
    @Getter
    private final String name;
    private final Class<C> type;

    public ConnectionInfo(Class<C> type, String name, List<Object> args, Map<String, String> beansValue) throws NoSuchMethodException, SecurityException {
        this.args = args.toArray();
        this.beansValue = beansValue;
        if (name == null) {
            Set<ConnectionName> names = ArgFactory.enumerateAnnotation(type, ConnectionName.class, Connection.class);
            this.name = names.stream().findFirst().map(ConnectionName::value).orElse(type.getCanonicalName());
        } else {
            this.name = name.trim();
        }
        this.type = type;
        // Check the constructor
        getConstructor();
    }

    public void register(StarterNode node) throws InvocationTargetException {
        try {
            node.registerStarter(type, name, () -> newConnections(node));
        } catch (UndeclaredThrowableException e) {
            throw (InvocationTargetException) e.getCause();
        }
    }

    private C newConnections(StarterNode node) {
        try {
            C cnx = getConstructor().newInstance(args);
            for (Map.Entry<String, String> e: beansValue.entrySet()) {
                String textValue = Util.parseTemplate(e.getValue(), cnx);
                ArgFactory.beanSetter(cnx, e.getKey(), textValue);
                cnx.log(Level.TRACE, "Setting bean '%s' to value '%s' for %s", e.getKey(), textValue, node);
            }
            cnx.setName(name);
            logger.debug("Connexion registred: {} for {}", cnx, node);
            return cnx;
        } catch (InvocationTargetException ex) {
            String message = Util.resolveThrowableException(ex.getCause());
            throw new UndeclaredThrowableException(new InvocationTargetException(ex.getCause(), "Error during connection creation of type " + type.getName() + " for " + node + ": " + message));
        } catch (Exception ex) {
            String message = Util.resolveThrowableException(ex);
            throw new UndeclaredThrowableException(new InvocationTargetException(ex, "Error during connection creation of type " + type.getName() + " for " + node + ": " + message));
        }
    }
    
    private Constructor<C> getConstructor() throws NoSuchMethodException, SecurityException {
        Class<?>[] constArgsType = new Class[args.length];
        int index = 0;
        for(Object arg: args) {
            constArgsType[index] = arg.getClass();
            index++;
        }
        return type.getConstructor(constArgsType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type.getCanonicalName() + "/" + name;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            ConnectionInfo<?,?> other = (ConnectionInfo<?,?>) obj;
            if (name == null) {
                if(other.name != null)
                    return false;
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (type == null) {
                return other.type == null;
            } else
                return type.equals(other.type);
        }
    }

}
