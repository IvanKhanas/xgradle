/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.impl.resolution;

import jakarta.annotation.Priority;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.Ordered;
import org.altlinux.xgradle.interfaces.resolution.PriorityOrdered;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("ResolutionStepOrdering")
class ResolutionStepOrderingTests {

    @Test
    @DisplayName("PriorityOrdered steps come before non-priority steps")
    void priorityOrderedStepsComeFirst() {
        ResolutionStep first = new PriorityStep(100);
        ResolutionStep second = new OrderedStep(1);

        int result = ResolutionStepOrdering.INSTANCE.compare(first, second);

        assertTrue(result < 0);
    }

    @Test
    @DisplayName("Ordered steps are compared by getOrder value")
    void orderedStepsComparedByOrderValue() {
        ResolutionStep low = new OrderedStep(10);
        ResolutionStep high = new OrderedStep(20);

        int result = ResolutionStepOrdering.INSTANCE.compare(low, high);

        assertTrue(result < 0);
    }

    @Test
    @DisplayName("Order annotation value is applied")
    void annotationOrderIsApplied() {
        ResolutionStep annotated = new AnnotatedStep();
        ResolutionStep plain = new PlainStep();

        int result = ResolutionStepOrdering.INSTANCE.compare(annotated, plain);

        assertTrue(result < 0);
    }

    @Test
    @DisplayName("Plain steps fallback to LOWEST_PRECEDENCE")
    void plainStepsFallbackToLowestPrecedence() {
        ResolutionStep one = new PlainStep();
        ResolutionStep two = new PlainStep();

        int result = ResolutionStepOrdering.INSTANCE.compare(one, two);

        assertEquals(0, result);
    }

    @Test
    @DisplayName("Jakarta Priority annotation is considered for order")
    void jakartaPriorityAnnotationIsConsidered() {
        ResolutionStep prioritized = new JakartaPriorityStep();
        ResolutionStep plain = new PlainStep();

        int result = ResolutionStepOrdering.INSTANCE.compare(prioritized, plain);

        assertTrue(result < 0);
    }

    private static final class OrderedStep implements ResolutionStep, Ordered {
        private final int order;

        private OrderedStep(int order) {
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public String name() {
            return "ordered";
        }

        @Override
        public void execute(ResolutionContext ctx) {
        }
    }

    private static final class PriorityStep implements ResolutionStep, PriorityOrdered {
        private final int order;

        private PriorityStep(int order) {
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public String name() {
            return "priority";
        }

        @Override
        public void execute(ResolutionContext ctx) {
        }
    }

    @Order(5)
    private static final class AnnotatedStep implements ResolutionStep {
        @Override
        public String name() {
            return "annotated";
        }

        @Override
        public void execute(ResolutionContext ctx) {
        }
    }

    private static final class PlainStep implements ResolutionStep {
        @Override
        public String name() {
            return "plain";
        }

        @Override
        public void execute(ResolutionContext ctx) {
        }
    }

    @Priority(1)
    private static final class JakartaPriorityStep implements ResolutionStep {
        @Override
        public String name() {
            return "jakarta-priority";
        }

        @Override
        public void execute(ResolutionContext ctx) {
        }
    }
}
