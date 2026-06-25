package io.crest.runtime;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.Map;

final class CrestRuntimeRoleCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnCrestRuntimeRole.class.getName()
        );
        if (attributes == null) {
            return true;
        }

        CrestRuntimeRole[] roles = (CrestRuntimeRole[]) attributes.get("value");
        CrestRuntimeRole currentRole = CrestRuntimeRole.from(context.getEnvironment());
        return Arrays.asList(roles).contains(currentRole);
    }
}
