package com.example.blog.blog.domain.model;

import lombok.Value;
import lombok.Builder;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;


/**
 * Generated Domain Value Object: PostContent
 * 
 * Generated class: PostContent
 * 
 *
 * This value object follows DDD tactical patterns:
 * - Immutability (no setters)
 * - Value-based equality
 * - Self-validation
 * - Rich behavior methods
 * - No identity
 */
@Value
@Builder
public class PostContent {

    String value;

    /**
     * Creates a new PostContent with validation.
     * All value objects must be created in a valid state.
     */
    private PostContent(String value) {
        this.value = value;

        // Self-validation - Value objects must always be in valid state
        validate();
    }

    /**
     * Factory method for creating validated PostContent instances.
     */
    public static PostContent of(String value) {
        return new PostContent(value);
    }

    /**
     * Business behavior: getExcerpt
     * 
     *
     * Value objects should contain rich business behavior related to their data.
     */
    public String getExcerpt(int maxLength) {
        if (value.length() <= maxLength) {
                  return value;
                }
                return value.substring(0, maxLength) + "...";
    }

    /**
     * Business behavior: equals
     * 
     *
     * Value objects should contain rich business behavior related to their data.
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
                if (obj == null || getClass() != obj.getClass()) return false;
                // TODO: Implement proper equals method
                return true;
    }

    /**
     * Business behavior: hashCode
     * 
     *
     * Value objects should contain rich business behavior related to their data.
     */
    public int hashCode() {
        // TODO: Implement proper hashCode method
                return Objects.hash(value);
    }

    /**
     * Validation method - ensures value object is always in valid state.
     * Throws IllegalArgumentException if invalid.
     */
    private void validate() {
        if ((value == null) || (value.trim().isEmpty())) {
            throw new IllegalArgumentException("PostContent.value cannot be null or empty");
        }

        // Custom business validation rules
        validateBusinessRules();
    }

    /**
     * Custom business validation rules specific to PostContent.
     * Override this method to add domain-specific validation.
     */
    private void validateBusinessRules() {
        // Add custom validation logic here
        // Example: email format validation, phone number format, etc.
    }

    /**
     * Check if this value object is empty/has default values.
     */
    public boolean isEmpty() {
        return (value == null) || (value.trim().isEmpty());
    }

    /**
     * Rich toString with all value object data.
     */
    @Override
    public String toString() {
        return "PostContent{" +
            "value=" + value
 +
            '}';
    }
}
