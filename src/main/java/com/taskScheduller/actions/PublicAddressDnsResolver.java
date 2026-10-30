package com.taskScheduller.actions;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

final class PublicAddressDnsResolver implements DnsResolver {

    private static final CidrBlock[] IPV4_GLOBAL_EXCLUSIONS = {
            cidr("0.0.0.0", 8),
            cidr("10.0.0.0", 8),
            cidr("100.64.0.0", 10),
            cidr("127.0.0.0", 8),
            cidr("169.254.0.0", 16),
            cidr("172.16.0.0", 12),

            cidr("192.0.0.0", 24),

            cidr("192.0.2.0", 24),
            cidr("192.88.99.0", 24),
            cidr("192.168.0.0", 16),
            cidr("198.18.0.0", 15),
            cidr("198.51.100.0", 24),
            cidr("203.0.113.0", 24),
            cidr("224.0.0.0", 4),
            cidr("240.0.0.0", 4)
    };

    private static final List<CidrBlock> IPV6_GLOBAL_EXCLUSIONS = List.of(
            cidr("2001::", 23),

            cidr("2001:2::", 48),

            cidr("2001:10::", 28),

            cidr("2001:db8::", 32),
            cidr("2002::", 16),
            cidr("3fff::", 20)
    );

    private static final CidrBlock IPV6_GLOBAL_UNICAST =
            cidr("2000::", 3);

    private final DnsResolver delegate;

    PublicAddressDnsResolver() {
        this(new SystemDefaultDnsResolver());
    }

    PublicAddressDnsResolver(DnsResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = delegate.resolve(host);

        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException(
                    "No IP addresses found for destination"
            );
        }

        boolean hasNonPublicAddress = Arrays.stream(addresses)
                .anyMatch(address ->
                        address == null || !isPublicAddress(address)
                );

        if (hasNonPublicAddress) {
            throw new UnknownHostException(
                    "Destination resolves to a non-public IP address"
            );
        }

        return addresses.clone();
    }

    @Override
    public String resolveCanonicalHostname(String host)
            throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }

    static boolean isPublicAddress(InetAddress address) {
        byte[] bytes = address.getAddress();

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        if (bytes.length == 4) {
            return Arrays.stream(IPV4_GLOBAL_EXCLUSIONS)
                    .noneMatch(block -> block.contains(bytes));
        }

        if (bytes.length == 16) {
            if (!IPV6_GLOBAL_UNICAST.contains(bytes)) {
                return false;
            }

            return IPV6_GLOBAL_EXCLUSIONS.stream()
                    .noneMatch(block -> block.contains(bytes));
        }

        return false;
    }

    private static CidrBlock cidr(String address, int prefixLength) {
        try {
            return new CidrBlock(
                    InetAddress.getByName(address).getAddress(),
                    prefixLength
            );
        } catch (UnknownHostException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private record CidrBlock(byte[] network, int prefixLength) {

        private boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }

            int completeBytes = prefixLength / 8;

            for (int i = 0; i < completeBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }

            int remainingBits = prefixLength % 8;

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);

            return (address[completeBytes] & mask)
                    == (network[completeBytes] & mask);
        }
    }
}
