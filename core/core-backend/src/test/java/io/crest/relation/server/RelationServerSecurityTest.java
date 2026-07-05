package io.crest.relation.server;

import io.crest.auth.CrestApiPath;
import io.crest.auth.CrestPermit;
import io.crest.constant.AuthResourceEnum;
import io.crest.relation.dto.RelationResourceRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelationServerSecurityTest {

    @Test
    void relationServerDeclaresAssociationMenuPermissionScope() {
        CrestApiPath apiPath = RelationServer.class.getAnnotation(CrestApiPath.class);

        assertNotNull(apiPath);
        assertArrayEquals(new String[]{"/relation"}, apiPath.value());
        assertEquals(AuthResourceEnum.ASSOCIATION, apiPath.rt());
    }

    @Test
    void relationReadEndpointsRequireAssociationMenuReadPermission() throws NoSuchMethodException {
        assertPermit(RelationServer.class.getMethod("overview"), "m:read");
        assertPermit(RelationServer.class.getMethod("datasource", Long.class), "m:read");
        assertPermit(RelationServer.class.getMethod("dataset", Long.class), "m:read");
        assertPermit(RelationServer.class.getMethod("dv", Long.class), "m:read");
        assertPermit(RelationServer.class.getMethod("resources", String.class, RelationResourceRequest.class), "m:read");
    }

    private static void assertPermit(Method method, String expected) {
        CrestPermit permit = method.getAnnotation(CrestPermit.class);
        assertNotNull(permit);
        assertArrayEquals(new String[]{expected}, permit.value());
    }
}
