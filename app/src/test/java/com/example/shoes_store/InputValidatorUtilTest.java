package com.example.shoes_store;

import com.example.shoes_store.core.InputValidatorUtil;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InputValidatorUtilTest {

    @Test
    public void validInputsReturnNull() {
        assertNull(InputValidatorUtil.validateFullName("Minh Quân"));
        assertNull(InputValidatorUtil.validatePhone("0912 345 678"));
        assertNull(InputValidatorUtil.validateEmail("runner@shoes.vn"));
    }

    @Test
    public void invalidInputsReturnMessages() {
        assertNotNull(InputValidatorUtil.validateFullName("A"));
        assertNotNull(InputValidatorUtil.validatePhone("12345"));
        assertNotNull(InputValidatorUtil.validateEmail("invalid-email"));
    }
}
