package com.example.demo;

import com.example.demo.service.ListingPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListingPolicyTests {
    private final ListingPolicy policy = new ListingPolicy();

    @Test
    void acceptsEveryAdvertisedConditionAndLocation() {
        for (ListingPolicy.ConditionOption condition : policy.conditions()) {
            for (String location : policy.locations()) {
                assertThat(policy.validate("Desk lamp", 20.00, condition.value(), location, "Working"))
                        .isEqualTo(ListingPolicy.ValidationResult.success());
            }
        }
    }

    @Test
    void rejectsOversizedAndOutOfRangeListingValues() {
        assertThat(policy.validate("x".repeat(121), 20, "used", "Scott Library", "Working").message())
                .isEqualTo("Title must be 120 characters or fewer.");
        assertThat(policy.validate("Lamp", 100000.01, "used", "Scott Library", "Working").message())
                .isEqualTo("Price must be between $0.00 and $100,000.00.");
        assertThat(policy.validate("Lamp", 20, "used", "Scott Library", "x".repeat(256)).message())
                .isEqualTo("Description must be 255 characters or fewer.");
    }
}
