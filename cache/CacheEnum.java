package com.freecharge.cibil.cache;

public enum CacheEnum {

    VALIDITY_CONFIG_CACHE(CacheConstants.VALIDITY_CONFIG_CACHE),

    PIN_CODE_CITY_CACHE(CacheConstants.PIN_CODE_CITY_CACHE);

    public final String cacheName;

    CacheEnum(String cacheName){
        this.cacheName = cacheName;
    }

    public String getCacheName(){
        return this.cacheName;
    }

    public static class CacheConstants {
        private CacheConstants() {

        }
        public static final String VALIDITY_CONFIG_CACHE = "velocity-cache";

        public static final String PIN_CODE_CITY_CACHE = "pincode-city-cache";
    }

}
