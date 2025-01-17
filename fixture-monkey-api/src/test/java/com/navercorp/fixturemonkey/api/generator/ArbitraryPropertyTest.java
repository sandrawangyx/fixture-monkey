/*
 * Fixture Monkey
 *
 * Copyright (c) 2021-present NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.fixturemonkey.api.generator;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.navercorp.fixturemonkey.api.property.PropertyCache;
import com.navercorp.fixturemonkey.api.property.PropertyNameResolver;
import com.navercorp.fixturemonkey.api.property.RootProperty;
import com.navercorp.fixturemonkey.api.type.TypeReference;

class ArbitraryPropertyTest {
	@Test
	void root() {
		// given
		TypeReference<GenericSample<String>> typeReference = new TypeReference<GenericSample<String>>() {
		};
		RootProperty rootProperty = PropertyCache.getRootProperty(typeReference.getAnnotatedType());

		// when
		ArbitraryProperty actual = new ArbitraryProperty(
			rootProperty,
			PropertyNameResolver.IDENTITY,
			null,
			0.0D,
			null,
			Collections.emptyList(),
			null
		);

		then(actual.isRoot()).isTrue();
		then(actual.getProperty()).isEqualTo(rootProperty);
		then(actual.getNullInject()).isEqualTo(0.0D);
		then(actual.getContainerInfo()).isNull();
	}

	static class GenericSample<T> {
		private T name;

		public T getName() {
			return this.name;
		}
	}
}
