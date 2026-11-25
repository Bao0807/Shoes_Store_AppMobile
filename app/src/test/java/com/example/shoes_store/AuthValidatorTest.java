package com.example.shoes_store;

import com.example.shoes_store.module.auth.AuthValidator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AuthValidatorTest {

    @Test
    public void validInputsReturnNull() {
        assertNull(AuthValidator.validatePassword("123456"));
        assertNull(AuthValidator.validateConfirmPassword("123456", "123456"));
    }

    @Test
    public void invalidInputsReturnMessages() {
        assertNotNull(AuthValidator.validatePassword("123"));
        assertNotNull(AuthValidator.validateConfirmPassword("123456", "654321"));
    }
}
