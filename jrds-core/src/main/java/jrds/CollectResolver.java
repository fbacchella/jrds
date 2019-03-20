package jrds;

public interface CollectResolver<K> {
    
    public class StringResolver implements CollectResolver<String> {
        @Override
        public String resolve(String collectKey) {
            return collectKey;
        }
    }
    
    public class NoneResolver implements CollectResolver<String> {
        @Override
        public String resolve(String collectKey) {
            return null;
        }
    }

    /**
     * @param collectKey
     * @return
     * @throws IllegalArgumentException when resolution failed
     */
    public K resolve(String collectKey);
}
