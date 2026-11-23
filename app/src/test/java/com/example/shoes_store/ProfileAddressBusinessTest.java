package com.example.shoes_store;

import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.profile.ProfileAddressService;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfileAddressBusinessTest {

    @Test
    public void firstAddressOrRequestedDefaultBecomesDefault() {
        assertTrue(ProfileAddressService.shouldMarkAsDefault(false, Collections.emptyList()));
        assertTrue(ProfileAddressService.shouldMarkAsDefault(false, null));

        List<ProfileAddressModel> existing = Collections.singletonList(
                new ProfileAddressModel("a1", "u1", "Bảo", "0901", "Q1", "Nhà", true)
        );
        assertFalse(ProfileAddressService.shouldMarkAsDefault(false, existing));
        assertTrue(ProfileAddressService.shouldMarkAsDefault(true, existing));
    }

    @Test
    public void deletingDefaultAddressPromotesNextAvailableAddress() {
        List<ProfileAddressModel> list = Arrays.asList(
                new ProfileAddressModel("a1", "u1", "Bảo", "0901", "Q1", "Nhà", true),
                new ProfileAddressModel("a2", "u1", "Bảo", "0901", "Q3", "Cơ quan", false)
        );
        assertEquals("a2", ProfileAddressService.findReplacementDefaultId("a1", list));
        assertNull(ProfileAddressService.findReplacementDefaultId("a2", list));
        assertNull(ProfileAddressService.findReplacementDefaultId("a1", Collections.singletonList(list.get(0))));
    }
}
