package com.wavefront.service;


import com.codahale.metrics.Counter;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wavefront.model.AppInfo;
import com.wavefront.props.AppInfoProperties;
import com.wavefront.utils.MetricsRecorderStatsCounter;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.util.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final AsyncLoadingCache<String, Optional<AppInfo>> cache;

  @Autowired
  private CloudFoundryClient cfClient;

  private final Counter numFetchAppInfo;
  private final Counter numFetchAppInfoError;

  public CachedAppInfoFetcher(MetricsReporter metricsReporter,
                              AppInfoProperties appInfoProperties) {
    cache = Caffeine.newBuilder().
        expireAfterWrite(appInfoProperties.getCacheExpireIntervalHours(), TimeUnit.HOURS).
        maximumSize(appInfoProperties.getAppInfoCacheSize()).
        recordStats(() -> new MetricsRecorderStatsCounter(metricsReporter)).
        buildAsync((key, executor) -> fetchFromPcf(key).toFuture());
    numFetchAppInfo = metricsReporter.registerCounter("fetch-app-info-from-pcf");
    numFetchAppInfoError = metricsReporter.registerCounter("fetch-app-info-error");
  }

  @Override
  public Mono<Optional<AppInfo>> fetch(String applicationId) {
    return Mono.fromFuture(cache.get(applicationId));
  }

  private Mono<Optional<AppInfo>> fetchFromPcf(String applicationId) {
    numFetchAppInfo.inc();
    return getApplication(applicationId).
        then(app -> getSpace(app.getSpaceId()).map(space -> Tuples.of(space, app))).
        then(function((space, app) -> getOrganization(space.getOrganizationId()).
            map(org -> Optional.of(new AppInfo(app.getName(), org.getName(), space.getName()))))).
        timeout(Duration.ofSeconds(PCF_FETCH_TIMEOUT_SECONDS)).
        onErrorResume(t -> {
          numFetchAppInfoError.inc();
          logger.log(Level.WARNING, "Unable to fetch app details for applicationId: " +
              applicationId, t);
          return Mono.just(Optional.empty());
        });
  }

  private Mono<ApplicationEntity> getApplication(String applicationId) {
    return cfClient.applicationsV2().
        get(GetApplicationRequest.builder().applicationId(applicationId).build()).
        map(ResourceUtils::getEntity);
  }

  private Mono<SpaceEntity> getSpace(String spaceId) {
    return cfClient.spaces().get(GetSpaceRequest.builder().spaceId(spaceId).build()).
        map(ResourceUtils::getEntity);
  }

  private Mono<OrganizationEntity> getOrganization(String orgId) {
    return cfClient.organizations().
        get(GetOrganizationRequest.builder().organizationId(orgId).build()).
        map(ResourceUtils::getEntity);
  }
}
