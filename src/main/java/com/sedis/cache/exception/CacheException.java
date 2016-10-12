package com.sedis.cache.exception;

public class CacheException extends RuntimeException {

	private static final long serialVersionUID = 3406835391011102379L;

	public CacheException() {
		super();
	}

	public CacheException(String message) {
		super(message);
	}

	public CacheException(String message, Throwable cause) {
		super(message, cause);
	}

	public CacheException(Throwable cause) {
		super(cause);
	}

}
