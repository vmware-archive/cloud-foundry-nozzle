package com.wavefront.service;

import com.wavefront.model.AppInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Service to fetch PCF Application details for given applicationId
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Service
public interface AppInfoFetcher {
  Mono<Optional<AppInfo>> fetch(String applicationId);
}
