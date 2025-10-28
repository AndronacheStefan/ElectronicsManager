// ...existing code...
public class WebServerLauncher {
    public static void main(String[] args) {
        try {
            // optional DB connectivity check
            TestDB.main(args);
        } catch (Exception e) {
            System.out.println("Warning: TestDB check failed: " + e.getMessage());
        }

        try {
            MainAPP.citireDinBD();
        } catch (Exception e) {
            System.out.println("Warning: could not load DB on startup: " + e.getMessage());
        }

        WebApiServer server = new WebApiServer();
        int startedPort = -1;
        for (int port = 8080; port <= 8090; port++) {
            try {
                server.start(port);
                startedPort = port;
                break;
            } catch (Exception e) {
                System.out.println("Port " + port + " unavailable: " + e.getMessage());
            }
        }

        if (startedPort == -1) {
            System.err.println("Failed to start web server on ports 8080-8090");
            return;
        }

        final int portToUse = startedPort;

        // write chosen port to file for easy discovery
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("server.port"), String.valueOf(portToUse));
        } catch (Exception ex) {
            System.out.println("Warning: unable to write server.port: " + ex.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down web server...");
            server.stop();
        }));

        System.out.println("Server is running on port " + portToUse + ". Open j.html (file://) or visit http://localhost:" + portToUse + "/api/equipment");

        // block forever
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }
}
