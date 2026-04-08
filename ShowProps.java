public class ShowProps {
  public static void main(String[] args) {
    System.out.println("user.home=" + System.getProperty("user.home"));
    System.out.println("maven.repo.local=" + System.getProperty("maven.repo.local"));
  }
}
