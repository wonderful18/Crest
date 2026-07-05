package io.crest.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.WebDataBinder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class GlobalBindingGuardTest {

    @Test
    void disallowsClassMetaProperties() {
        WebDataBinder binder = new WebDataBinder(new Object());

        new GlobalBindingGuard().initBinder(binder);

        assertArrayEquals(
                new String[]{"class.*", "Class.*", "*.class.*", "*.Class.*"},
                binder.getDisallowedFields()
        );
    }
}
