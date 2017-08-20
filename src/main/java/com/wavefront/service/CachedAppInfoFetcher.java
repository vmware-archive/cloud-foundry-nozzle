package com.wavefront.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wavefront.model.AppInfo;
import com.wavefront.props.AppInfoProperties;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.util.ResourceUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

/**
 * AppInfoFetcher implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public class CachedAppInfoFetcher implements AppInfoFetcher {

  private static final Logger logger = Logger.getLogger(
      CachedAppInfoFetcher.class.getCanonicalName());

  /**
   * How long to wait to receive AppInfo from PCF
   */
  private static final int PCF_FETCH_TIMEOUT_SECONDS = 120;
  private final Cache<String, Optional<AppInfo>> cache;

  private final ConnectionContext connectionContext;

  private final TokenProvider tokenProvider;

  public CachedAppInfoFetcher(AppInfoProperties appInfoProperties,
                              ConnectionContext connectionContext,
                              TokenProvider tokenProvider) {
    cache = Caffeine.newBuilder().
        expireAfterWrite(appInfoProperties.getCacheExpireIntervalHours(), TimeUnit.HOURS).
        maximumSize(appInfoProperties.getAppInfoCacheSize()).build();
    this.connectionContext = connectionContext;
    this.tokenProvider = tokenProvider;
  }

  @Override
  public Mono<Optional<AppInfo>> fetch(String applicationId) {
    // Note - cache.get is thread safe
    Optional<AppInfo> optionalAppInfo = cache.getIfPresent(applicationId);
    if (optionalAppInfo == null) {
      // not present in the cache ...
      return fetchFromPcf(applicationId).doOnNext(item -> {
        // Put optionalAppInfo in the cache only if it is present
        if (item.isPresent()) {
          // Note - cache.put is thread safe
          cache.put(applicationId, item);
        }
      });
    } else {
      // return cached value ...
      return Mono.just(optionalAppInfo);
    }
  }

  private Mono<Optional<AppInfo>> fetchFromPcf(String applicationId) {
    // A CloudFoundryClient can be instantiated on the fly with no real penalty and is totally
    // stateless, simply combining a ConnectionContext and a TokenProvider to make a REST call.
    CloudFoundryClient cfClient = ReactorCloudFoundryClient.builder().
        connectionContext(connectionContext).tokenProvider(tokenProvider).build();
    return getApplication(cfClient, applicationId).
        then(app -> getSpace(cfClient, app.getSpaceId()).map(space -> Tuples.of(space, app))).
        then(function((space, app) -> getOrganization(cfClient, space.getOrganizationId()).
            map(org -> Optional.of(new AppInfo(app.getName(), org.getName(), space.getName()))))).
        timeout(Duration.ofSeconds(PCF_FETCH_TIMEOUT_SECONDS)).
        otherwise(t -> {
          logger.log(Level.WARNING, "Unable to fetch app details for applicationId:" +
              applicationId, t);
          return Mono.just(Optional.empty());
        });
  }

  private Mono<ApplicationEntity> getApplication(CloudFoundryClient cfClient,
                                                 String applicationId) {
    return cfClient.applicationsV2().
        get(GetApplicationRequest.builder().applicationId(applicationId).build()).
        map(ResourceUtils::getEntity);
  }

  private Mono<SpaceEntity> getSpace(CloudFoundryClient cfClient,
                                     String spaceid) {
    return cfClient.spaces().get(GetSpaceRequest.builder().spaceId(spaceid).build()).
        map(ResourceUtils::getEntity);
  }

  private Mono<OrganizationEntity> getOrganization(CloudFoundryClient cfClient,
                                                   String orgId) {
    return cfClient.organizations().
        get(GetOrganizationRequest.builder().organizationId(orgId).build()).
        map(ResourceUtils::getEntity);
  }
}
