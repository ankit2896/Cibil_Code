package com.freecharge.cibil.controller;

import com.freecharge.cibil.annotations.Logged;
import com.freecharge.cibil.annotations.Marked;
import com.freecharge.cibil.annotations.Timed;
import com.freecharge.cibil.cache.CacheEnum;
import com.freecharge.cibil.model.response.ServiceResponse;
import org.apache.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheManagerController {

	@Logged
	@Timed
	@Marked
	@PostMapping("/api/cibil/v1/delete/evictCache")
	@CacheEvict(value = CacheEnum.CacheConstants.VALIDITY_CONFIG_CACHE, allEntries = true)
	public ServiceResponse<Integer> evictCache() {
		return new ServiceResponse<>(HttpStatus.SC_OK, true);
	}

	@Logged
	@Timed
	@Marked
	@PostMapping("/api/cibil/v1/delete/evictCache/pinCode")
	@CacheEvict(value = CacheEnum.CacheConstants.PIN_CODE_CITY_CACHE, allEntries = true)
	public ServiceResponse<Integer> evictCachePinCode() {
		return new ServiceResponse<>(HttpStatus.SC_OK, true);
	}
}
