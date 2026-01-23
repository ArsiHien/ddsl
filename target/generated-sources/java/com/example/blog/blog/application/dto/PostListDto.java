package com.example.blog.blog.application.dto;

import lombok.Value;
import lombok.Builder;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

import import java.util.List<PostSummaryDto>;;

/**
 * Generated Domain Value Object: PostListDto
 * 
 * Generated class: PostListDto
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
public class PostListDto {

    List posts;

    /**
     * Creates a new PostListDto with validation.
     * All value objects must be created in a valid state.
     */
    private PostListDto(List posts) {
        this.posts = posts;

        // Self-validation - Value objects must always be in valid state
        validate();
    }

    /**
     * Factory method for creating validated PostListDto instances.
     */
    public static PostListDto of(List posts) {
        return new PostListDto(posts);
    }

    /**
     * Business behavior: getPosts
     * 
     *
     * Value objects should contain rich business behavior related to their data.
     */
    public List getPosts() {
        return posts;
    }

    /**
     * Validation method - ensures value object is always in valid state.
     * Throws IllegalArgumentException if invalid.
     */
    private void validate() {
        if ((posts == null) || (posts.isEmpty())) {
            throw new IllegalArgumentException("PostListDto.posts cannot be null or empty");
        }

        // Custom business validation rules
        validateBusinessRules();
    }

    /**
     * Custom business validation rules specific to PostListDto.
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
        return (posts == null) || (posts.isEmpty());
    }

    /**
     * Rich toString with all value object data.
     */
    @Override
    public String toString() {
        return "PostListDto{" +
            "posts=" + posts
 +
            '}';
    }
}
