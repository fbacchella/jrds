package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.ConnectionName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ConnectionInfo {
    static final private Logger logger = Logger.getLogger(ConnectionInfo.class);

    private final List<Object> args;
    private final Map<String, String> beansValue;
    private final String name;
    private final Class<? extends Connection<?>> type;

    public ConnectionInfo(Class<? extends Connection<?>> type, String name, List<Object> args, Map<String, String> beansValue) {
        super();
        this.args = args;
        this.beansValue = beansValue;
        if(name == null) {
            Set<ConnectionName> names = ArgFactory.enumerateAnnotation(type, ConnectionName.class, Connection.class);
            if(!names.isEmpty()) {
                this.name = names.iterator().next().value();
            } else {
                this.name = type.getCanonicalName();
            }
        } else {
            this.name = name;
        }
        this.type = type;
    }

    public void register(StarterNode node) throws InvocationTargetException {
        try {
            Class<?>[] constArgsType = new Class[args.size()];
            Object[] constArgsVal = new Object[args.size()];
            int index = 0;
            for(Object arg: args) {
                constArgsType[index] = arg.getClass();
                constArgsVal[index] = arg;
                index++;
            }
            Connection<?> cnx = type.getConstructor(constArgsType).newInstance(constArgsVal);
            for(Map.Entry<String, String> e: beansValue.entrySet()) {
                String textValue = Util.parseTemplate(e.getValue(), cnx);
                ArgFactory.beanSetter(cnx, e.getKey(), textValue);
                cnx.log(Level.TRACE, "Setting bean '%s' to value '%s' for %s", e.getKey(), textValue, node);
            }
            if(name != null && !name.trim().isEmpty())
                cnx.setName(name.trim());
            node.registerStarter(cnx);
            logger.debug(Util.delayedFormatString("Connexion registred: %s for %s", cnx, node));
        } catch (InvocationTargetException ex) {
            throw new InvocationTargetException(ex.getCause(), "Error during connection creation of type " + type.getName() + " for " + node + ": " + ex.getMessage());
        } catch (Exception ex) {
            throw new InvocationTargetException(ex, "Error during connection creation of type " + type.getName() + " for " + node);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type.getCanonicalName() + (name == null ? "" : ("/" + name));
    }

    /**
     * @see java.lang.Object#hashCode()
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            ConnectionInfo other = (ConnectionInfo) obj;
            if (name == null) {
                if(other.name != null)
                    return false;
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
        }
        return true;
    }

}
