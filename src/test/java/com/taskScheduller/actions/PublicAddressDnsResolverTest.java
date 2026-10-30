package com.taskScheduller.actions;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicAddressDnsResolverTest {

    @Test
    void shouldAcceptPublicIpv4Address() throws UnknownHostException {
        InetAddress address = ip("8.8.8.8");

        InetAddress[] result = resolverFor(address)
                .resolve("example.com");

        assertArrayEquals(new InetAddress[]{address}, result);
    }

    @Test
    void shouldRejectPrivateIpv4Address() throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("10.0.0.1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectLoopbackIpv4Address() throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("127.0.0.1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectLinkLocalIpv4Address() throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("169.254.1.1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectMixedPublicAndPrivateAddresses()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(
                        ip("8.8.8.8"),
                        ip("192.168.1.1")
                ).resolve("example.com")
        );
    }

    @Test
    void shouldAcceptGlobalIpv6Address() throws UnknownHostException {
        InetAddress address = ip("2606:4700:4700::1111");

        InetAddress[] result = resolverFor(address)
                .resolve("example.com");

        assertArrayEquals(new InetAddress[]{address}, result);
    }

    @Test
    void shouldRejectUniqueLocalIpv6Address()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("fc00::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectLinkLocalIpv6Address()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("fe80::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectIpv6DocumentationAddress()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("2001:db8::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectEmptyDnsResponse() {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor().resolve("example.com")
        );
    }

    @Test
    void shouldRejectIpv6DocumentationAddressFromNewPrefix()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("3fff::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectIpv6AddressOutsideGlobalUnicastRange()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("5f00::1"))
                        .resolve("example.com")
        );
    }
    @Test
    void shouldRejectIpv4SpecificGlobalAllocationInsideBlockedRange()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("192.0.0.9"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectIpv6SpecificGlobalAllocationInsideBlockedRange()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("2001:1::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectIpv6BenchmarkingAddress()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("2001:2::1"))
                        .resolve("example.com")
        );
    }

    @Test
    void shouldRejectDeprecatedIpv6OrchidAddress()
            throws UnknownHostException {
        assertThrows(
                UnknownHostException.class,
                () -> resolverFor(ip("2001:10::1"))
                        .resolve("example.com")
        );
    }

    private static PublicAddressDnsResolver resolverFor(
            InetAddress... addresses
    ) {
        DnsResolver delegate = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) {
                return addresses;
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };

        return new PublicAddressDnsResolver(delegate);
    }

    private static InetAddress ip(String value)
            throws UnknownHostException {
        return InetAddress.getByName(value);
    }
}
