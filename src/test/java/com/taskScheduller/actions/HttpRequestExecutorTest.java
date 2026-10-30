
package com.taskScheduller.actions;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestExecutorTest {

    @Test
    void shouldRejectRequestsWhenDisabled() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(false, "example.com");

        assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        Map.of("url", "https://example.com")
                )
        );
    }

    @Test
    void shouldRejectHttpUrls() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of("url", "http://example.com")
                )
        );
    }

    @Test
    void shouldRejectUnknownHosts() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of("url", "https://another-domain.com")
                )
        );
    }

    @Test
    void shouldRejectUrlWithoutHost() {
        HttpRequestExecutor executor = new HttpRequestExecutor(
                true,
                "example.com"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of("url", "https:///path")
                )
        );
    }

    @Test
    void shouldRejectUnapprovedMethods() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of(
                                "url", "https://example.com",
                                "method", "DELETE"
                        )
                )
        );
    }

    @Test
    void shouldRejectUserInfoInUrl() {
        HttpRequestExecutor executor =
                new HttpRequestExecutor(true, "example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        Map.of(
                                "url",
                                "https://user:password@example.com"
                        )
                )
        );
    }

    @Test
    void shouldRejectPrivateResolvedAddressEvenWhenHostIsAllowed()
            throws Exception {
        AtomicBoolean dnsWasCalled = new AtomicBoolean(false);

        DnsResolver fakeResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host)
                    throws UnknownHostException {
                dnsWasCalled.set(true);

                return new InetAddress[]{
                        InetAddress.getByName("127.0.0.1")
                };
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };

        HttpRequestExecutor executor = new HttpRequestExecutor(
                true,
                "example.com",
                new PublicAddressDnsResolver(fakeResolver)
        );

        assertThrows(
                IOException.class,
                () -> executor.execute(
                        Map.of("url", "https://example.com")
                )
        );

        assertTrue(
                dnsWasCalled.get(),
                "The DNS resolver should have been called"
        );
    }
}
