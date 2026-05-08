import org.mindrot.jbcrypt.BCrypt;
public class GenHash {
    public static void main(String[] args) {
        String password = "Admin@1234";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("HASH:" + hash);
        boolean ok = BCrypt.checkpw(password, hash);
        System.out.println("VERIFY:" + ok);
    }
}
