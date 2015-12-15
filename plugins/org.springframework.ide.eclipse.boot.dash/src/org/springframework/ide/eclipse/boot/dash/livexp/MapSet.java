/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.livexp;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Represents a Set constructed by applying a function to each element of another
 * set.
 */
public class MapSet<S, T> extends ObservableSet<T> {

	private ObservableSet<S> input;
	private Function<S, T> function;

	public MapSet(ObservableSet<S> input, Function<S, T> function) {
		this.input = input;
		this.function = function;
		dependsOn(input);
	}

	@Override
	protected ImmutableSet<T> compute() {
		Builder<T> builder = new ImmutableSet.Builder<>();
		for (S a : input.getValues()) {
			T v = function.apply(a);
			//Check for null, generally google collections don't allow nulls (which is good)
			// and we can take advantage of returning nulls to combine mapping and filtering with
			// a single function.
			if (v!=null) {
				builder.add(v);
			}
		}
		return builder.build();
	}

}