/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.logging.log4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.kohsuke.randname.RandomNameGenerator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class PrependRandomName extends Recipe {
    private static final MethodMatcher logStatement = new MethodMatcher("org.apache.log4j.Category *(Object, ..)");

    private final int seed;

    @JsonIgnore
    private transient final RandomNameGenerator randomName;

    public PrependRandomName(int seed) {
        this.seed = seed;
        randomName = new RandomNameGenerator(seed);
    }

    @Override
    public String getDisplayName() {
        return "Prepend a random name to each Log4J statement";
    }

    @Override
    public String getDescription() {
        return "To make finding the callsite of a logging statement easier in code search.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(logStatement), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                Object parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
                //noinspection ConstantConditions
                if (parent instanceof J.MethodInvocation &&
                        logStatement.matches((J.MethodInvocation) parent) &&
                       JavaType.Primitive.String == literal.getType() &&
                        !literal.getValue().toString().startsWith("<")) {
                    String value = "<" + randomName.next() + "> " + literal.getValue().toString();
                    return literal.withValue(value)
                            .withValueSource("\"" + value + "\"");
                }
                return super.visitLiteral(literal, ctx);
            }
        });
    }
}
