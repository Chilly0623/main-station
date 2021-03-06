package com.rongzi.ms.step_definitions;

import com.rongzi.ms.helpers.BinaryType;
import com.rongzi.ms.helpers.Env;
import org.apache.log4j.Logger;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.openqa.selenium.Proxy.ProxyType.MANUAL;
import static org.openqa.selenium.remote.CapabilityType.PROXY;

/**
 * Created by lining on 2017/7/2.
 */
final class WebDriverFactory {

    private static Logger logger = Logger.getLogger(WebDriverFactory.class);

    private static Map<String, String> binaries;

    private WebDriverFactory() {

    }

    static {
        binaries = new HashMap<>();

        for (BinaryType binaryType : BinaryType.values()) {

            for (String filename : binaryType.getBinaryFilenames()) {
                binaries.put(filename, binaryType.getDriverSystemProperty());
            }

        }


    }

    private static void initEnv() {

        String directory = Env.getProperty("binary.directory", "selenium_standalone");
        try {
            Path dir = Paths.get(directory);

            logger.debug("binary directory:" + dir.toAbsolutePath().toString());

            Files.walk(dir)
                    .filter(path -> Files.isRegularFile(path))
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        if (binaries.containsKey(filename)) {
                            String binaryPath = path.toAbsolutePath().toString();
                            System.setProperty(binaries.get(filename), binaryPath);
                            logger.debug("set system property:" + binaryPath);
                        }

                    });
        } catch (IOException e) {
            logger.error(e);
        }
    }

    static WebDriver create() {

        initEnv();

        String webDriverProperty = Env.getProperty("webdriver");

        if (webDriverProperty == null || webDriverProperty.isEmpty()) {
            throw new IllegalStateException("The webdriver system property must be set");
        }

        try {
            Proxy proxy = null;

            if (Boolean.valueOf(Env.getProperty("proxy", "false"))) {
                String proxyDetails = String.format("%s:%d", Env.getProperty("proxyHost"), Integer.valueOf(Env.getProperty("proxyPort")));
                proxy = new Proxy();
                proxy.setProxyType(MANUAL);
                proxy.setHttpProxy(proxyDetails);
                proxy.setSslProxy(proxyDetails);
            }
            return Drivers.valueOf(webDriverProperty.toUpperCase()).newDriver(proxy);
        } catch (IllegalArgumentException e) {
            String msg = String.format("The webdriver system property '%s' did not match any " +
                            "existing browser or the browser was not supported on your operating system. " +
                            "Valid values are %s",
                    webDriverProperty, Arrays.stream(Drivers
                            .values())
                            .map(Enum::name)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()));

            throw new IllegalStateException(msg, e);
        }
    }

    private enum Drivers {
        FIREFOX {
            @Override
            public WebDriver newDriver() {
                return newDriver(null);
            }

            @Override
            public WebDriver newDriver(Proxy proxy) {
                DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                if (proxy != null) {
                    capabilities.setCapability(PROXY, proxy);
                }
                return new ChromeDriver(capabilities);
            }
        }, CHROME {
            @Override
            public WebDriver newDriver() {
                return newDriver(null);
            }

            @Override
            public WebDriver newDriver(Proxy proxy) {
                DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                if (proxy != null) {
                    capabilities.setCapability(PROXY, proxy);
                }
                return new ChromeDriver(capabilities);
            }
        }, PHANTOMJS {
            @Override
            public WebDriver newDriver() {
                return newDriver(null);
            }

            @Override
            public WebDriver newDriver(Proxy proxy) {
                DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
                if (proxy != null) {
                    List<String> cliArguments = new ArrayList<>();
                    cliArguments.add("--proxy-type=http");
                    cliArguments.add("--proxy=" + proxy.getHttpProxy());
                    capabilities.setCapability("phantomjs.cli.args", cliArguments);
                }
                return new PhantomJSDriver(capabilities);
            }
        }, IE {
            @Override
            public WebDriver newDriver() {
                return newDriver(null);
            }

            @Override
            public WebDriver newDriver(Proxy proxy) {
                DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
                if (proxy != null) {
                    capabilities.setCapability(PROXY, proxy);
                }
                return new InternetExplorerDriver(capabilities);
            }
        };

        public abstract org.openqa.selenium.WebDriver newDriver();

        public abstract org.openqa.selenium.WebDriver newDriver(Proxy proxy);

    }
}
