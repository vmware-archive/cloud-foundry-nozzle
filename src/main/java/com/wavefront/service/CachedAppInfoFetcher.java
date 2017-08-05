package com.wavefront.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wavefront.model.AppInfo;
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
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

/**
 * AppInfoFetcher implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public class CachedAppInfoFetcher implements AppInfoFetcher {

  private static final Logger logger = Logger.getLogger(CachedAppInfoFetcher.class.getCanonicalName());
  /**
   * We can tune this constant if needed ...
   */
  private static final int NUM_PCF_APPS = 5000;
  /**
   * AppData is mutable, so refresh the data every 1 hour
   */
  private static final int REFRESH_INTERVAL_HOUR = 1;
  private final Cache<String, AppInfo> cache = CacheBuilder.newBuilder().
          expireAfterWrite(REFRESH_INTERVAL_HOUR, TimeUnit.HOURS).maximumSize(NUM_PCF_APPS).build();

  @Autowired
  private CloudFoundryClient cfClient;

  @Override
  public Mono<Optional<AppInfo>> fetch(String applicationId) {
    // cache.get is thread safe
    AppInfo appInfo = cache.getIfPresent(applicationId);
    if (appInfo == null) {
      // not present in the cache ...
      return fetchFromPcf(applicationId).doOnNext(optionalAppInfo -> {
        if (optionalAppInfo.isPresent()) {
          // cache.put is thread safe
          cache.put(applicationId, optionalAppInfo.get());
        }
      });
    } else {
      // return cached value ...
      return Mono.just(Optional.of(appInfo));
    }
  }

  private Mono<Optional<AppInfo>> fetchFromPcf(String applicationId) {
    return getApplication(applicationId).
            then(app -> getSpace(app.getSpaceId()).map(space -> Tuples.of(space, app))).
            then(function((space, app) -> getOrganization(space.getOrganizationId()).
                    map(org -> Optional.of(new AppInfo(app.getName(), org.getName(), space.getName()))))).
            otherwise(t -> {
              logger.log(Level.WARNING, "Unable to fetch app details for applicationId:" + applicationId, t);
              return Mono.just(Optional.empty());
            });
  }

  private Mono<ApplicationEntity> getApplication(String applicationId) {
    return cfClient.applicationsV2()
            .get(GetApplicationRequest.builder().applicationId(applicationId).build())
            .map(ResourceUtils::getEntity);
  }

  private Mono<SpaceEntity> getSpace(String spaceid) {
    return cfClient.spaces()
            .get(GetSpaceRequest.builder().spaceId(spaceid).build())
            .map(ResourceUtils::getEntity);
  }

  private Mono<OrganizationEntity> getOrganization(String orgId) {
    return cfClient.organizations()
            .get(GetOrganizationRequest.builder().organizationId(orgId).build())
            .map(ResourceUtils::getEntity);
  }
}
