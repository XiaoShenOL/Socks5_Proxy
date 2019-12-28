public class Main {
    public static void main(String[] args) {
        try (ProxyServer proxyServer = new ProxyServer(5555)) {
            proxyServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
