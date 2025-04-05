package com.bokkurin.trackery.areas;

import lombok.Getter;

@Getter
public enum InsertType {
	SIDO("sido"), SIGUNGU("sigungu");

	private final String value;

	InsertType(String value) {
		this.value = value;
	}
}
