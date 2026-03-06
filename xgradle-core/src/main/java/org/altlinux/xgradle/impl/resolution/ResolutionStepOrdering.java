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

import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.Ordered;
import org.altlinux.xgradle.interfaces.resolution.PriorityOrdered;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Comparator for {@link ResolutionStep} ordering.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class ResolutionStepOrdering implements Comparator<ResolutionStep> {

    static final ResolutionStepOrdering INSTANCE = new ResolutionStepOrdering();
    private static final String JAKARTA_PRIORITY = "jakarta.annotation.Priority";

    private ResolutionStepOrdering() {
    }

    @Override
    public int compare(ResolutionStep first, ResolutionStep second) {
        boolean firstPriority = first instanceof PriorityOrdered;
        boolean secondPriority = second instanceof PriorityOrdered;
        if (firstPriority != secondPriority) {
            return firstPriority ? -1 : 1;
        }

        int firstOrder = orderOf(first);
        int secondOrder = orderOf(second);
        return Integer.compare(firstOrder, secondOrder);
    }

    private int orderOf(ResolutionStep step) {
        if (step instanceof Ordered) {
            return ((Ordered) step).getOrder();
        }

        Order order = step.getClass().getAnnotation(Order.class);
        if (order != null) {
            return order.value();
        }

        Integer priority = resolvePriority(step.getClass());
        if (priority != null) {
            return priority;
        }

        return Ordered.LOWEST_PRECEDENCE;
    }

    private Integer resolvePriority(Class<?> type) {
        Integer value = resolvePriority(type, JAKARTA_PRIORITY);
        if (value != null) {
            return value;
        }
        return resolvePriority(type, JAKARTA_PRIORITY);
    }

    private Integer resolvePriority(Class<?> type, String className) {
        try {
            Class<?> annotationClass = Class.forName(className, false, type.getClassLoader());
            if (!Annotation.class.isAssignableFrom(annotationClass)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> annType = (Class<? extends Annotation>) annotationClass;
            Annotation annotation = type.getAnnotation(annType);
            if (annotation == null) {
                return null;
            }
            Method method = annotationClass.getMethod("value");
            Object result = method.invoke(annotation);
            return result instanceof Integer ? (Integer) result : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
