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

    public K resolve(String collectKey);
}
