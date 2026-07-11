package io.crest.substitute.permissions.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PlatformPermissionScopeSqlSecurityTest {

    @Test
    public void invalidResourceTypeThrowsException() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        try {
            manage.resourceScopeSql("malicious; DROP TABLE--", "t.id", "t.create_by", null);
            fail("Should throw for invalid resource type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("resource type"));
        }
    }

    @Test
    public void invalidResourceTypeWithInjectionThrowsException() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        try {
            manage.resourceScopeSql("dataset' OR '1'='1", "t.id", "t.create_by", null);
            fail("Should throw for injected resource type");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("resource type"));
        }
    }

    @Test
    public void invalidCreatorColumnThrowsException() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        try {
            manage.resourceScopeSql("dataset", "t.id", "t.create_by; DROP TABLE--", null);
            fail("Should throw for invalid creator column");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("creatorColumn"));
        }
    }

    @Test
    public void invalidResourceIdColumnThrowsException() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        try {
            manage.resourceScopeSql("dataset", "t.id' OR '1'='1", "t.create_by", null);
            fail("Should throw for invalid resource id column");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("resourceIdColumn"));
        }
    }

    @Test
    public void invalidOrgColumnThrowsException() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        try {
            manage.resourceScopeSql("panel", "v.id", "v.create_by", "v.org_id; --");
            fail("Should throw for invalid org column");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("orgColumn"));
        }
    }

    @Test
    public void validResourceTypeReturnsNullForNullUser() {
        PlatformPermissionManage manage = new PlatformPermissionManage();
        String result = manage.resourceScopeSql("dataset", "t.id", "t.create_by", null);
        assertNull(result);
    }
}
