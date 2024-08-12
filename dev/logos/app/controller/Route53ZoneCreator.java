package dev.logos.app.controller;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.Optional;
import java.util.function.Supplier;

public class Route53ZoneCreator {
    private final Route53Client route53Client =
            Route53Client.builder().region(
                    new DefaultAwsRegionProviderChain().getRegion()
            ).credentialsProvider(DefaultCredentialsProvider.create()).build();

    public Optional<String> createZoneIfNotExists(String domainName, String txtValue) {
        return findHostedZoneId(domainName).or(() -> createZoneAndAddTxtRecord(domainName, txtValue));
    }

    private Optional<String> findHostedZoneId(String domainName) {
        for (HostedZone zone : route53Client.listHostedZones(ListHostedZonesRequest.builder().build()).hostedZones()) {
            if (zone.name().equals(domainName + ".")) {
                return Optional.of(zone.id());
            }
        }
        return Optional.empty();
    }

    private java.util.stream.Stream<HostedZone> streamAllHostedZones(Supplier<ListHostedZonesResponse> listZones) {
        return java.util.stream.Stream.iterate(
                listZones.get(),
                ListHostedZonesResponse::isTruncated,
                response -> listZones.get().toBuilder()
                        .marker(response.nextMarker())
                        .build()
        ).flatMap(response -> response.hostedZones().stream());
    }

    private Optional<String> createZoneAndAddTxtRecord(String domainName, String txtValue) {
        CreateHostedZoneResponse response = route53Client.createHostedZone(CreateHostedZoneRequest.builder()
                .name(domainName)
                .callerReference(String.valueOf(System.currentTimeMillis()))
                .build());

        String zoneId = response.hostedZone().id();
        System.out.println("Created hosted zone with ID: " + zoneId);

        addTxtRecord(zoneId, domainName, txtValue);

        return Optional.of(zoneId);
    }

    private void addTxtRecord(String zoneId, String domainName, String txtValue) {
        String recordName = "external-dns." + domainName;

        ChangeBatch changeBatch = ChangeBatch.builder()
                .changes(Change.builder()
                        .action(ChangeAction.CREATE)
                        .resourceRecordSet(ResourceRecordSet.builder()
                                .name(recordName)
                                .type(RRType.TXT)
                                .ttl(300L)
                                .resourceRecords(ResourceRecord.builder()
                                        .value("\"" + txtValue + "\"")
                                        .build())
                                .build())
                        .build())
                .build();

        route53Client.changeResourceRecordSets(ChangeResourceRecordSetsRequest.builder()
                .hostedZoneId(zoneId)
                .changeBatch(changeBatch)
                .build());

        System.out.println("Added TXT record: " + recordName);
    }
}
