package com.navercorp.fixturemonkey.arbitrary;

import static com.navercorp.fixturemonkey.Constants.ALL_INDEX_EXP_INDEX;
import static com.navercorp.fixturemonkey.Constants.ALL_INDEX_STRING;
import static com.navercorp.fixturemonkey.Constants.NO_OR_ALL_INDEX_INTEGER_VALUE;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArbitraryExpression implements Comparable<ArbitraryExpression> {
	private final List<Exp> expList;

	private ArbitraryExpression(String expression) {
		expList = Arrays.stream(expression.split("\\."))
			.map(Exp::new)
			.collect(toList());
	}

	public static ArbitraryExpression from(String expression) {
		return new ArbitraryExpression(expression);
	}

	public ArbitraryExpression appendLeft(String expression) {
		String newStringExpression = expression + "." + this;
		return new ArbitraryExpression(newStringExpression);
	}

	public ArbitraryExpression appendRight(String expression) {
		String newStringExpression = this + "." + expression;
		return new ArbitraryExpression(newStringExpression);
	}

	public List<Exp> getExpList() {
		return expList;
	}

	public int size() {
		return expList.size();
	}

	public Exp getFieldExp() {
		return expList.get(expList.size() - 1);
	}

	// expression(fieldName + index)이 아닌 fieldName의 일치만 확인합니다
	public boolean hasField(String fieldName) {
		return expList.stream()
			.map(Exp::getName)
			.anyMatch(fieldName::equals);
	}

	public boolean isFieldName(String fieldName) {
		return getFieldExp().equals(new Exp(fieldName));
	}

	public Exp get(int index) {
		return expList.get(index);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ArbitraryExpression fe = (ArbitraryExpression)obj;
		return expList.equals(fe.expList);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.expList);
	}

	public String toString() {
		return expList.stream()
			.map(Exp::toString)
			.collect(Collectors.joining("."));
	}

	public String toString(int size) {
		if (size >= this.expList.size()) {
			return toString();
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; i++) {
			sb.append(expList.get(i).toString());
			if (i != size - 1) {
				sb.append(".");
			}
		}
		return sb.toString();
	}

	@Override
	public int compareTo(ArbitraryExpression arbitraryExpression) {
		List<Exp> expList = this.getExpList();
		List<Exp> oExpList = arbitraryExpression.getExpList();

		int expLength = Math.min(oExpList.size(), expList.size());

		for (int i = 0; i < expLength; i++) {
			Exp exp = expList.get(i);
			Exp oExp = oExpList.get(i);
			int expCompare = exp.compareTo(oExp);
			if (expCompare != 0) {
				return expCompare;
			}
		}

		return Integer.compare(oExpList.size(), expList.size());
	}

	public static class ExpIndex implements Comparable<ExpIndex> {
		private final int index;

		public ExpIndex(int index) {
			this.index = index;
		}

		public int getIndex() {
			return index;
		}

		public boolean equalsIgnoreAllIndex(ExpIndex expIndex) {
			return this.index == expIndex.index;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ExpIndex expIndex = (ExpIndex)obj;
			return index == expIndex.index || index == NO_OR_ALL_INDEX_INTEGER_VALUE
				|| expIndex.index == NO_OR_ALL_INDEX_INTEGER_VALUE;
		}

		@Override
		public int hashCode() {
			return 0; // allIndex 처리를 위해 hash 값을 0으로 초기화, equals로 판별
		}

		public String toString() {
			return index == NO_OR_ALL_INDEX_INTEGER_VALUE ? ALL_INDEX_STRING : String.valueOf(index);
		}

		@Override
		public int compareTo(ExpIndex expIndex) {
			return Integer.compare(this.index, expIndex.index);
		}
	}

	public static class Exp implements Comparable<Exp> {
		private final String name;
		private final List<ExpIndex> index = new ArrayList<>();

		public Exp(String expression) {
			int li = expression.indexOf('[');
			int ri = expression.indexOf(']');

			if ((li != -1 && ri == -1) || (li == -1 && ri != -1)) {
				throw new IllegalArgumentException("expression is invalid. expression : " + expression);
			}

			if (li == -1) {
				this.name = expression;
			} else {
				this.name = expression.substring(0, li);
				while (li != -1 && ri != -1) {
					if (ri - li > 1) {
						String indexString = expression.substring(li + 1, ri);
						final int indexValue =
							indexString.equals(ALL_INDEX_STRING)
								? NO_OR_ALL_INDEX_INTEGER_VALUE : Integer.parseInt(indexString);
						this.index.add(new ExpIndex(indexValue));
					}
					expression = expression.substring(ri + 1);
					li = expression.indexOf('[');
					ri = expression.indexOf(']');
				}
			}
		}

		public boolean hasAllIndex() {
			return !index.isEmpty() && index.stream().anyMatch(it -> it.equalsIgnoreAllIndex(ALL_INDEX_EXP_INDEX));
		}

		public boolean equalsIgnoreAllIndex(Exp exp) {
			int length = index.size();
			int expLength = exp.index.size();

			if (length != expLength) {
				return false;
			}

			for (int i = 0; i < length; i++) {
				if (!index.get(i).equalsIgnoreAllIndex(exp.index.get(i))) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Exp exp = (Exp)obj;
			return name.equals(exp.name) && index.equals(exp.index);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, index);
		}

		public String getName() {
			return name;
		}

		public List<ExpIndex> getIndex() {
			return index;
		}

		public String toString() {
			String indexBrackets = index.stream()
				.map(i -> "[" + i.toString() + "]")
				.collect(Collectors.joining());
			return name + indexBrackets;
		}

		@Override
		public int compareTo(Exp exp) {
			List<ExpIndex> indices = this.getIndex();
			List<ExpIndex> oIndices = exp.getIndex();

			if (exp.name.equals(this.name)) {
				int indexLength = Math.min(oIndices.size(), indices.size());
				for (int i = 0; i < indexLength; i++) {
					ExpIndex index = indices.get(i);
					ExpIndex oIndex = oIndices.get(i);
					// name이 같을 때는 index가 작은 순서
					int indexCompare = oIndex.compareTo(index);
					if (indexCompare != 0) {
						return indexCompare;
					}
				}
			}
			return Integer.compare(indices.size(), oIndices.size());
		}
	}
}