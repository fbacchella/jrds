
public class CatchPlay {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.println("done");
            throw new RuntimeException("toto");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("well done");

        }
    }

}
