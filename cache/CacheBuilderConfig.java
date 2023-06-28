package com.freecharge.cibil.cache;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Slf4j
@EnableCaching
@Configuration
public class CacheBuilderConfig implements CachingConfigurer {

	private static final String CACHE_MANAGER = "cacheManager";

	@Value("${cache.validity.size}")
	private Integer velocityCacheSize;

	@Value("${cache.pincode.city.size}")
	private Integer pinCodeCityCacheSize;

	@Override
	@Bean(name = CACHE_MANAGER)
	public CacheManager cacheManager() {
		log.debug("Initialising Cache ");
		final SimpleCacheManager cacheManager = new SimpleCacheManager();

		final Cache velocityCache = new GuavaCache(CacheEnum.VALIDITY_CONFIG_CACHE.getCacheName(),
				CacheBuilder.newBuilder().maximumSize(velocityCacheSize).build());

		final Cache pinCodeCityMappingCache = new GuavaCache(CacheEnum.PIN_CODE_CITY_CACHE.getCacheName(),
				CacheBuilder.newBuilder().maximumSize(pinCodeCityCacheSize).build());

		cacheManager.setCaches(Arrays.asList(velocityCache, pinCodeCityMappingCache));

        log.debug("Cache Manager is {}", cacheManager);
		return cacheManager;
	}

	@Override
	public CacheResolver cacheResolver() {
		return null;
	}

	@Bean
	@Override
	public KeyGenerator keyGenerator() {
		return new SimpleKeyGenerator();
	}

	@Override
	public CacheErrorHandler errorHandler() {
		return null;
	}
}
