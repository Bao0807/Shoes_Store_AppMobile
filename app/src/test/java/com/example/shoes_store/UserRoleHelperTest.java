package com.example.shoes_store;

import com.example.shoes_store.core.UserRoleHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserRoleHelperTest {

    @Test
    public void isAdminRoleRecognizesCaseInsensitiveAdminAndNormalizesUsers() {
        assertTrue(UserRoleHelper.isAdminRole("admin"));
        assertTrue(UserRoleHelper.isAdminRole("ADMIN"));
        assertTrue(UserRoleHelper.isAdminRole(" admin "));
        assertFalse(UserRoleHelper.isAdminRole("users"));
        assertFalse(UserRoleHelper.isAdminRole("CUSTOMER"));
        assertFalse(UserRoleHelper.isAdminRole(null));

        assertEquals("admin", UserRoleHelper.normalizeRole("admin"));
        assertEquals("admin", UserRoleHelper.normalizeRole("ADMIN"));
        assertEquals("users", UserRoleHelper.normalizeRole("users"));
        assertEquals("users", UserRoleHelper.normalizeRole(null));

        assertTrue(UserRoleHelper.isDefaultAdminEmail("admin@gmail.com"));
        assertEquals("admin", UserRoleHelper.resolveRole("users", "admin@gmail.com"));
        assertEquals("users", UserRoleHelper.resolveRole("users", "bao123@gmail.com"));
    }
}
