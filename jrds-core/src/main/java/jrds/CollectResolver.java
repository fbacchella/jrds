package jrds;

public interface CollectResolver<K> {
    
    class StringResolver implements CollectResolver<String> {
        @Override
        public String resolve(String collectKey) {
            return collectKey;
        }
    }
    
    class NoneResolver implements CollectResolver<String> {
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
    K resolve(String collectKey);
}
