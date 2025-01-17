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

package com.navercorp.fixturemonkey;

import static com.navercorp.fixturemonkey.Constants.HEAD_NAME;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators.F3;
import net.jqwik.api.Combinators.F4;

import com.navercorp.fixturemonkey.api.expression.ExpressionGenerator;
import com.navercorp.fixturemonkey.api.property.FieldProperty;
import com.navercorp.fixturemonkey.api.property.PropertyNameResolver;
import com.navercorp.fixturemonkey.api.random.Randoms;
import com.navercorp.fixturemonkey.api.type.TypeReference;
import com.navercorp.fixturemonkey.api.type.Types;
import com.navercorp.fixturemonkey.arbitrary.AbstractArbitrarySet;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryApply;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryExpression;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryExpressionManipulator;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryNode;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryNullity;
import com.navercorp.fixturemonkey.arbitrary.ArbitrarySet;
import com.navercorp.fixturemonkey.arbitrary.ArbitrarySetArbitrary;
import com.navercorp.fixturemonkey.arbitrary.ArbitrarySetPostCondition;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryTraverser;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryTree;
import com.navercorp.fixturemonkey.arbitrary.ArbitraryType;
import com.navercorp.fixturemonkey.arbitrary.BuilderManipulator;
import com.navercorp.fixturemonkey.arbitrary.ContainerSizeConstraint;
import com.navercorp.fixturemonkey.arbitrary.ContainerSizeManipulator;
import com.navercorp.fixturemonkey.arbitrary.MetadataManipulator;
import com.navercorp.fixturemonkey.arbitrary.PostArbitraryManipulator;
import com.navercorp.fixturemonkey.customizer.ArbitraryCustomizer;
import com.navercorp.fixturemonkey.customizer.ArbitraryCustomizers;
import com.navercorp.fixturemonkey.customizer.ExpressionSpec;
import com.navercorp.fixturemonkey.customizer.WithFixtureCustomizer;
import com.navercorp.fixturemonkey.generator.ArbitraryGenerator;
import com.navercorp.fixturemonkey.validator.ArbitraryValidator;

public final class ArbitraryBuilder<T> {
	private final ArbitraryTree<T> tree;
	private final ArbitraryTraverser traverser;
	private final List<BuilderManipulator> builderManipulators;
	private final List<BuilderManipulator> usedManipulators;
	@SuppressWarnings("rawtypes")
	private final ArbitraryValidator validator;
	private final Map<Class<?>, ArbitraryGenerator> generatorMap;

	private ArbitraryGenerator generator;
	private ArbitraryCustomizers arbitraryCustomizers;
	private boolean validOnly = true;

	@SuppressWarnings({"unchecked", "rawtypes"})
	ArbitraryBuilder(
		Class<T> clazz,
		ArbitraryOption options,
		ArbitraryGenerator generator,
		ArbitraryValidator validator,
		ArbitraryCustomizers arbitraryCustomizers,
		Map<Class<?>, ArbitraryGenerator> generatorMap
	) {
		this(
			new ArbitraryTree<>(
				ArbitraryNode.builder()
					.type(new ArbitraryType(clazz))
					.propertyName(HEAD_NAME)
					.build()
			),
			new ArbitraryTraverser(options),
			generator,
			validator,
			arbitraryCustomizers,
			new ArrayList<>(),
			new ArrayList<>(),
			generatorMap
		);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	ArbitraryBuilder(
		TypeReference<T> typeReference,
		ArbitraryOption options,
		ArbitraryGenerator generator,
		ArbitraryValidator validator,
		ArbitraryCustomizers arbitraryCustomizers,
		Map<Class<?>, ArbitraryGenerator> generatorMap
	) {
		this(
			new ArbitraryTree<>(
				ArbitraryNode.builder()
					.type(new ArbitraryType(typeReference))
					.propertyName(HEAD_NAME)
					.build()
			),
			new ArbitraryTraverser(options),
			generator,
			validator,
			arbitraryCustomizers,
			new ArrayList<>(),
			new ArrayList<>(),
			generatorMap
		);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	ArbitraryBuilder(
		Supplier<T> valueSupplier,
		ArbitraryTraverser traverser,
		ArbitraryGenerator generator,
		ArbitraryValidator validator,
		ArbitraryCustomizers arbitraryCustomizers,
		Map<Class<?>, ArbitraryGenerator> generatorMap
	) {
		this(
			new ArbitraryTree<>(
				ArbitraryNode.builder()
					.value(valueSupplier)
					.propertyName(HEAD_NAME)
					.build()
			),
			traverser,
			generator,
			validator,
			arbitraryCustomizers,
			new ArrayList<>(),
			new ArrayList<>(),
			generatorMap
		);
	}

	@SuppressWarnings("rawtypes")
	private ArbitraryBuilder(
		ArbitraryTree<T> tree,
		ArbitraryTraverser traverser,
		ArbitraryGenerator generator,
		ArbitraryValidator validator,
		ArbitraryCustomizers arbitraryCustomizers,
		List<BuilderManipulator> builderManipulators,
		List<BuilderManipulator> usedManipulators,
		Map<Class<?>, ArbitraryGenerator> generatorMap
	) {
		this.tree = tree;
		this.traverser = traverser;
		this.generator = getGenerator(generator, arbitraryCustomizers);
		this.validator = validator;
		this.arbitraryCustomizers = arbitraryCustomizers;
		this.builderManipulators = builderManipulators;
		this.usedManipulators = usedManipulators;
		this.generatorMap = generatorMap.entrySet().stream()
			.map(it -> new SimpleEntry<Class<?>, ArbitraryGenerator>(
				it.getKey(),
				getGenerator(it.getValue(), arbitraryCustomizers))
			)
			.collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
	}

	public ArbitraryBuilder<T> validOnly(boolean validOnly) {
		this.validOnly = validOnly;
		return this;
	}

	public ArbitraryBuilder<T> generator(ArbitraryGenerator generator) {
		this.generator = getGenerator(generator, arbitraryCustomizers);
		return this;
	}

	public Arbitrary<T> build() {
		ArbitraryBuilder<T> buildArbitraryBuilder = this.copy();
		return buildArbitraryBuilder.tree.result(() -> {
			ArbitraryTree<T> buildTree = buildArbitraryBuilder.tree;

			buildArbitraryBuilder.traverser.traverse(
				buildTree,
				false,
				(PropertyNameResolver)property -> buildArbitraryBuilder.generator.resolveFieldName(
					((FieldProperty)property).getField())
			);

			List<BuilderManipulator> actualManipulators = buildArbitraryBuilder.getActiveManipulators();

			buildArbitraryBuilder.apply(actualManipulators);
			buildTree.update(buildArbitraryBuilder.generator, generatorMap);
			return buildTree.getArbitrary();
		}, this.validator, this.validOnly);
	}

	public T sample() {
		return this.build().sample();
	}

	public List<T> sampleList(int size) {
		return this.sampleStream().limit(size).collect(toList());
	}

	public Stream<T> sampleStream() {
		return this.build().sampleStream();
	}

	public ArbitraryBuilder<T> spec(ExpressionSpec expressionSpec) {
		this.builderManipulators.addAll(expressionSpec.getBuilderManipulators());
		return this;
	}

	public ArbitraryBuilder<T> specAny(ExpressionSpec... specs) {
		if (specs == null || specs.length == 0) {
			return this;
		}

		ExpressionSpec spec = Arrays.asList(specs).get(Randoms.nextInt(specs.length));
		return this.spec(spec);
	}

	@SuppressWarnings("unchecked")
	public ArbitraryBuilder<T> set(String expression, @Nullable Object value) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		if (value == null) {
			return this.setNull(expression);
		} else if (value instanceof Arbitrary) {
			this.builderManipulators.add(new ArbitrarySetArbitrary<>(arbitraryExpression, (Arbitrary<T>)value));
			return this;
		} else if (value instanceof ArbitraryBuilder) {
			return this.setBuilder(expression, (ArbitraryBuilder<?>)value);
		} else if (value instanceof ExpressionSpec) {
			return this.setSpec(expression, (ExpressionSpec)value);
		}
		this.builderManipulators.add(new ArbitrarySet<>(arbitraryExpression, value));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> set(ExpressionGenerator expressionGenerator, @Nullable Object value) {
		return this.set(resolveExpression(expressionGenerator), value);
	}

	public ArbitraryBuilder<T> set(String expression, Object value, long limit) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		if (value == null) {
			return this.setNull(expression);
		} else if (value instanceof Arbitrary) {
			this.builderManipulators.add(new ArbitrarySetArbitrary<>(arbitraryExpression, (Arbitrary<T>)value, limit));
			return this;
		} else if (value instanceof ArbitraryBuilder) {
			return this.setBuilder(expression, (ArbitraryBuilder<?>)value, limit);
		} else if (value instanceof ExpressionSpec) {
			return this.setSpec(expression, (ExpressionSpec)value);
		}
		this.builderManipulators.add(new ArbitrarySet<>(arbitraryExpression, value, limit));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> set(ExpressionGenerator expressionGenerator, Object value, long limit) {
		return this.set(resolveExpression(expressionGenerator), value, limit);
	}

	public ArbitraryBuilder<T> set(@Nullable Object value) {
		return this.set(HEAD_NAME, value);
	}

	private ArbitraryBuilder<T> setBuilder(String expression, ArbitraryBuilder<?> builder) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitrarySetArbitrary<>(arbitraryExpression, builder.build()));
		return this;
	}

	private ArbitraryBuilder<T> setBuilder(String expression, ArbitraryBuilder<?> builder, long limit) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitrarySetArbitrary<>(arbitraryExpression, builder.build(), limit));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	private ArbitraryBuilder<T> setBuilder(
		ExpressionGenerator expressionGenerator,
		ArbitraryBuilder<?> builder,
		long limit
	) {
		return this.setBuilder(resolveExpression(expressionGenerator), builder, limit);
	}

	public ArbitraryBuilder<T> setNull(String expression) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitraryNullity(arbitraryExpression, true));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> setNull(ExpressionGenerator expressionGenerator) {
		return this.setNull(resolveExpression(expressionGenerator));
	}

	public ArbitraryBuilder<T> setNotNull(String expression) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitraryNullity(arbitraryExpression, false));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> setNotNull(ExpressionGenerator expressionGenerator) {
		return this.setNotNull(resolveExpression(expressionGenerator));
	}

	public ArbitraryBuilder<T> setPostCondition(Predicate<T> filter) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(HEAD_NAME);
		this.builderManipulators.add(new ArbitrarySetPostCondition<>(tree.getClazz(), arbitraryExpression, filter));
		return this;
	}

	public <U> ArbitraryBuilder<T> setPostCondition(String expression, Class<U> clazz, Predicate<U> filter) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitrarySetPostCondition<>(clazz, arbitraryExpression, filter));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public <U> ArbitraryBuilder<T> setPostCondition(
		ExpressionGenerator expressionGenerator,
		Class<U> clazz,
		Predicate<U> filter
	) {
		return this.setPostCondition(resolveExpression(expressionGenerator), clazz, filter);
	}

	public <U> ArbitraryBuilder<T> setPostCondition(
		String expression,
		Class<U> clazz,
		Predicate<U> filter,
		long limit
	) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		this.builderManipulators.add(new ArbitrarySetPostCondition<>(clazz, arbitraryExpression, filter, limit));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public <U> ArbitraryBuilder<T> setPostCondition(
		ExpressionGenerator expressionGenerator,
		Class<U> clazz,
		Predicate<U> filter,
		long limit
	) {
		return this.setPostCondition(resolveExpression(expressionGenerator), clazz, filter, limit);
	}

	public ArbitraryBuilder<T> size(String expression, int size) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		builderManipulators.add(new ContainerSizeManipulator(arbitraryExpression, size, size));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> size(ExpressionGenerator expressionGenerator, int size) {
		return this.size(resolveExpression(expressionGenerator), size);
	}

	public ArbitraryBuilder<T> size(String expression, int min, int max) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		builderManipulators.add(new ContainerSizeManipulator(arbitraryExpression, min, max));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> size(ExpressionGenerator expressionGenerator, int min, int max) {
		return this.size(resolveExpression(expressionGenerator), min, max);
	}

	public ArbitraryBuilder<T> minSize(String expression, int min) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		builderManipulators.add(
			new ContainerSizeManipulator(arbitraryExpression, min, null)
		);
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> minSize(ExpressionGenerator expressionGenerator, int min) {
		return this.minSize(resolveExpression(expressionGenerator), min);
	}

	public ArbitraryBuilder<T> maxSize(String expression, int max) {
		ArbitraryExpression arbitraryExpression = ArbitraryExpression.from(expression);
		builderManipulators.add(new ContainerSizeManipulator(arbitraryExpression, null, max));
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	public ArbitraryBuilder<T> maxSize(ExpressionGenerator expressionGenerator, int max) {
		return this.maxSize(resolveExpression(expressionGenerator), max);
	}

	public ArbitraryBuilder<T> customize(Class<T> type, ArbitraryCustomizer<T> customizer) {
		this.arbitraryCustomizers = this.arbitraryCustomizers.mergeWith(
			Collections.singletonMap(type, customizer)
		);

		if (this.generator instanceof WithFixtureCustomizer) {
			this.generator = ((WithFixtureCustomizer)this.generator).withFixtureCustomizers(arbitraryCustomizers);
		}
		return this;
	}

	public <U> ArbitraryBuilder<U> map(Function<T, U> mapper) {
		return new ArbitraryBuilder<>(() -> mapper.apply(this.sample()),
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.generatorMap
		);
	}

	public <U, R> ArbitraryBuilder<R> zipWith(
		ArbitraryBuilder<U> other,
		BiFunction<T, U, R> combinator
	) {
		return new ArbitraryBuilder<>(() -> combinator.apply(this.sample(), other.sample()),
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.generatorMap
		);
	}

	public <U, V, R> ArbitraryBuilder<R> zipWith(
		ArbitraryBuilder<U> other,
		ArbitraryBuilder<V> another,
		F3<T, U, V, R> combinator
	) {
		return new ArbitraryBuilder<>(() -> combinator.apply(
			this.sample(),
			other.sample(),
			another.sample()
		),
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.generatorMap);
	}

	public <U, V, W, R> ArbitraryBuilder<R> zipWith(
		ArbitraryBuilder<U> other,
		ArbitraryBuilder<V> another,
		ArbitraryBuilder<W> theOther,
		F4<T, U, V, W, R> combinator
	) {
		return new ArbitraryBuilder<>(() -> combinator.apply(
			this.sample(),
			other.sample(),
			another.sample(),
			theOther.sample()
		),
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.generatorMap);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <U> ArbitraryBuilder<U> zipWith(
		List<ArbitraryBuilder<?>> others,
		Function<List<?>, U> combinator
	) {
		return new ArbitraryBuilder<>(() -> {
			List combinedList = new ArrayList<>();
			combinedList.add(this.sample());
			for (ArbitraryBuilder<?> other : others) {
				combinedList.add(other.sample());
			}
			return combinator.apply(combinedList);
		},
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.generatorMap
		);
	}

	public ArbitraryBuilder<T> apply(BiConsumer<T, ArbitraryBuilder<T>> biConsumer) {
		ArbitraryApply<T> arbitraryApply = new ArbitraryApply<>(this, biConsumer);
		this.builderManipulators.clear(); // toSampleArbitraryBuilder have all manipulators before apply
		this.builderManipulators.add(arbitraryApply);
		return this;
	}

	public ArbitraryBuilder<T> acceptIf(Predicate<T> predicate, Consumer<ArbitraryBuilder<T>> consumer) {
		return this.apply((obj, builder) -> {
			if (predicate.test(obj)) {
				consumer.accept(builder);
			}
		});
	}

	public ArbitraryBuilder<T> fixed() {
		T sample = this.sample();
		setCurrentBuilderManipulatorsAsUsed();
		this.apply(new ArbitrarySet<>(ArbitraryExpression.from(HEAD_NAME), sample));
		return this;
	}

	@Deprecated // would be removed when isDirty is removed
	private void setCurrentBuilderManipulatorsAsUsed() {
		this.usedManipulators.clear();
		this.usedManipulators.addAll(this.builderManipulators);
	}

	@Deprecated // would be removed when isDirty is removed
	private List<BuilderManipulator> getActiveManipulators() {
		List<BuilderManipulator> activeManipulators = new ArrayList<>();
		for (int i = 0; i < builderManipulators.size(); i++) {
			BuilderManipulator builderManipulator = builderManipulators.get(i);
			if (i < usedManipulators.size()) {
				BuilderManipulator appliedManipulator = usedManipulators.get(i);
				if (builderManipulator.equals(appliedManipulator)) {
					continue;
				}
			}

			activeManipulators.add(builderManipulator);
		}
		return activeManipulators;
	}

	@SuppressWarnings("unchecked")
	public <R> ArbitraryBuilder<T> apply(BuilderManipulator builderManipulator) {
		if (builderManipulator instanceof ArbitraryApply) {
			apply((ArbitraryApply<T>)builderManipulator);
		} else if (builderManipulator instanceof ContainerSizeManipulator) {
			apply((ContainerSizeManipulator)builderManipulator);
		} else if (builderManipulator instanceof AbstractArbitrarySet) {
			apply((AbstractArbitrarySet<T>)builderManipulator);
		} else if (builderManipulator instanceof ArbitraryNullity) {
			apply((ArbitraryNullity)builderManipulator);
		} else if (builderManipulator instanceof PostArbitraryManipulator) {
			apply((PostArbitraryManipulator<R>)builderManipulator);
		} else {
			throw new IllegalArgumentException(
				"Unimplemented manipulator type : " + builderManipulator.getClass().toGenericString()
			);
		}
		return this;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private ArbitraryBuilder<T> apply(ContainerSizeManipulator containerSizeManipulator) {
		ArbitraryExpression arbitraryExpression = containerSizeManipulator.getArbitraryExpression();

		Collection<ArbitraryNode> foundNodes = this.findNodesByExpression(arbitraryExpression);
		for (ArbitraryNode foundNode : foundNodes) {
			if (!foundNode.getType().isContainer()) {
				throw new IllegalArgumentException("Only Container can set size");
			}
			foundNode.setContainerSizeConstraint(
				new ContainerSizeConstraint(containerSizeManipulator.getMin(), containerSizeManipulator.getMax())
			);
			traverser.traverse(
				foundNode,
				false,
				(PropertyNameResolver)property -> this.generator.resolveFieldName(
					((FieldProperty)property).getField())
			); // regenerate subtree
		}
		return this;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void apply(AbstractArbitrarySet<T> fixtureSet) {
		Collection<ArbitraryNode> foundNodes = this.findNodesByExpression(fixtureSet.getArbitraryExpression());

		for (ArbitraryNode<T> foundNode : foundNodes) {
			if (fixtureSet.isApplicable()) {
				foundNode.apply(fixtureSet);
				if (fixtureSet instanceof ArbitrarySet) {
					traverser.traverse(foundNode, foundNode.isKeyOfMapStructure(), generator);
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private ArbitraryBuilder<T> apply(ArbitraryNullity arbitraryNullity) {
		ArbitraryExpression arbitraryExpression = arbitraryNullity.getArbitraryExpression();
		Collection<ArbitraryNode> foundNodes = this.findNodesByExpression(arbitraryExpression);
		for (ArbitraryNode foundNode : foundNodes) {
			foundNode.apply(arbitraryNullity);
		}
		return this;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <R> ArbitraryBuilder<T> apply(PostArbitraryManipulator<R> postArbitraryManipulator) {
		Collection<ArbitraryNode> foundNodes = this.findNodesByExpression(
			postArbitraryManipulator.getArbitraryExpression()
		);
		if (!foundNodes.isEmpty()) {
			for (ArbitraryNode<R> foundNode : foundNodes) {
				if (postArbitraryManipulator.isMappableTo(foundNode)) {
					foundNode.addPostArbitraryOperation(postArbitraryManipulator);
				}
			}
		}
		return this;
	}

	@API(since = "0.4.0", status = Status.EXPERIMENTAL)
	private ArbitraryBuilder<T> apply(ArbitraryApply<T> arbitraryApply) {
		ArbitraryBuilder<T> toSampleArbitraryBuilder = arbitraryApply.getToSampleArbitraryBuilder();
		BiConsumer<T, ArbitraryBuilder<T>> builderBiConsumer = arbitraryApply.getBuilderBiConsumer();
		T sample = toSampleArbitraryBuilder.sample();
		ArbitrarySet<T> toFixSampled = new ArbitrarySet<>(ArbitraryExpression.from(HEAD_NAME), sample);
		toSampleArbitraryBuilder.builderManipulators.clear();
		toSampleArbitraryBuilder.builderManipulators.add(toFixSampled);
		builderBiConsumer.accept(sample, toSampleArbitraryBuilder);
		this.apply(toSampleArbitraryBuilder.builderManipulators);
		return this;
	}

	@API(since = "0.4.0", status = Status.INTERNAL)
	@SuppressWarnings("rawtypes")
	private void apply(List<BuilderManipulator> arbitraryManipulators) {
		List<MetadataManipulator> metadataManipulators = this.extractMetadataManipulatorsFrom(arbitraryManipulators);
		List<BuilderManipulator> orderedArbitraryManipulators =
			this.extractOrderedManipulatorsFrom(arbitraryManipulators);
		List<PostArbitraryManipulator> postArbitraryManipulators =
			this.extractPostArbitraryManipulatorsFrom(arbitraryManipulators);

		metadataManipulators.stream().sorted().forEachOrdered(it -> it.accept(this));
		orderedArbitraryManipulators.forEach(it -> it.accept(this));
		postArbitraryManipulators.forEach(it -> it.accept(this));
	}

	@Deprecated
	public boolean isDirty() {
		return usedManipulators.size() != builderManipulators.size();
	}

	public ArbitraryBuilder<T> copy() {
		ArbitraryBuilder<T> copied = new ArbitraryBuilder<>(
			this.tree.copy(),
			this.traverser,
			this.generator,
			this.validator,
			this.arbitraryCustomizers,
			this.builderManipulators.stream().map(BuilderManipulator::copy).collect(toList()),
			this.usedManipulators.stream().map(BuilderManipulator::copy).collect(toList()),
			this.generatorMap
		);
		copied.validOnly(this.validOnly);
		return copied;
	}

	private ArbitraryBuilder<T> setSpec(String expression, ExpressionSpec expressionSpec) {
		for (BuilderManipulator builderManipulator : expressionSpec.getBuilderManipulators()) {
			if (builderManipulator instanceof ArbitraryExpressionManipulator) {
				ArbitraryExpressionManipulator arbitraryExpressionManipulator =
					(ArbitraryExpressionManipulator)builderManipulator;
				arbitraryExpressionManipulator.addPrefix(expression);
			}
		}
		spec(expressionSpec);
		return this;
	}

	private List<BuilderManipulator> extractOrderedManipulatorsFrom(List<BuilderManipulator> manipulators) {
		return manipulators.stream()
			.filter(it -> !(it instanceof MetadataManipulator))
			.filter(it -> !(it instanceof PostArbitraryManipulator))
			.collect(toList());
	}

	private List<MetadataManipulator> extractMetadataManipulatorsFrom(List<BuilderManipulator> manipulators) {
		return manipulators.stream()
			.filter(MetadataManipulator.class::isInstance)
			.map(MetadataManipulator.class::cast)
			.sorted()
			.collect(toList());
	}

	@SuppressWarnings("rawtypes")
	private List<PostArbitraryManipulator> extractPostArbitraryManipulatorsFrom(
		List<BuilderManipulator> manipulators
	) {
		return manipulators.stream()
			.filter(PostArbitraryManipulator.class::isInstance)
			.map(PostArbitraryManipulator.class::cast)
			.collect(toList());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Collection<ArbitraryNode> findNodesByExpression(ArbitraryExpression arbitraryExpression) {
		Collection<ArbitraryNode> foundNodes = tree.findAll(arbitraryExpression);
		ArbitraryNode resetNode = tree.findFirstResetNode();

		if (resetNode != null && !resetNode.isLeafNode()) {
			traverser.traverse(
				resetNode,
				resetNode.isKeyOfMapStructure(),
				(PropertyNameResolver)property -> generator.resolveFieldName(((FieldProperty)property).getField())
			);
			foundNodes = tree.findAll(arbitraryExpression);
		}

		return foundNodes;
	}

	private ArbitraryGenerator getGenerator(ArbitraryGenerator generator, ArbitraryCustomizers customizers) {
		if (generator instanceof WithFixtureCustomizer) {
			generator = ((WithFixtureCustomizer)generator).withFixtureCustomizers(customizers);
		}
		return generator;
	}

	// Temporary adapter for legacy FieldNameResolver
	@Deprecated
	private String resolveExpression(ExpressionGenerator expressionGenerator) {
		return expressionGenerator.generate(property -> {
			Class<?> type = Types.getActualType(property.getType());
			ArbitraryGenerator generator = this.generatorMap.getOrDefault(type, this.generator);
			return generator.resolvePropertyName(property);
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ArbitraryBuilder<?> that = (ArbitraryBuilder<?>)obj;
		Class<?> generateClazz = tree.getClazz();
		Class<?> thatGenerateClazz = that.tree.getClazz();

		return generateClazz.equals(thatGenerateClazz)
			&& builderManipulators.equals(that.builderManipulators);
	}

	@Override
	public int hashCode() {
		Class<?> generateClazz = tree.getClazz();
		return Objects.hash(generateClazz, builderManipulators);
	}
}
