package com.civic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Opens the home page in your default browser once the app has fully started,
 * so you don't have to copy the URL. Turn it off with app.open-browser=false.
 */
@Component
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);
    /** Set after the first launch so DevTools restarts don't open a new tab every time you save. */
    private static final String OPENED_FLAG = "civic.browser.opened";

    @Value("${app.open-browser:true}")
    private boolean enabled;

    @Value("${server.port:8080}")
    private int port;

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        if (!enabled || System.getProperty(OPENED_FLAG) != null) {
            return;
        }
        System.setProperty(OPENED_FLAG, "true");
        String url = "http://localhost:" + port;
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command;
        if (os.contains("win")) {
            command = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (os.contains("mac")) {
            command = new String[]{"open", url};
        } else {
            command = new String[]{"xdg-open", url};
        }
        try {
            new ProcessBuilder(command).start();
            log.info("Opened {} in your browser", url);
        } catch (Exception e) {
            log.info("Could not open the browser automatically. Open {} yourself.", url);
        }
    }
}
