    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: AddCommentCommand
    * Generated class: AddCommentCommand
*
* This entity follows DDD tactical patterns:
* - Identity-based equality
* - Rich domain behavior
* - Business rule enforcement
* - Immutable value objects
*/

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

public


class AddCommentCommand
 {

@EqualsAndHashCode.Include
@Getter
private final AddCommentCommandId id;

            @Getter
            private final String postId;

            @Getter
            private final String authorName;

            @Getter
            private final String authorEmail;

            @Getter
            private final String commentText;



            /**
            * Domain entity constructor with business validation
            */
            public AddCommentCommand(String postId, String authorName, String authorEmail, String commentText) {
                this.postId = postId;
                this.authorName = authorName;
                this.authorEmail = authorEmail;
                this.commentText = commentText;
            }


            /**
            * Business method: getPostId
            */
            public String getPostId()
             {
                validateBusinessRules();

                    return postId;

                enforceInvariants();

                }

            /**
            * Business method: getAuthorName
            */
            public String getAuthorName()
             {
                validateBusinessRules();

                    return authorName;

                enforceInvariants();

                }

            /**
            * Business method: getAuthorEmail
            */
            public String getAuthorEmail()
             {
                validateBusinessRules();

                    return authorEmail;

                enforceInvariants();

                }

            /**
            * Business method: getCommentText
            */
            public String getCommentText()
             {
                validateBusinessRules();

                    return commentText;

                enforceInvariants();

                }


    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
                if ((postId == null) || (postId.trim().isEmpty())) {
                throw new IllegalArgumentException("postId cannot be null or empty");
                }
                if ((authorName == null) || (authorName.trim().isEmpty())) {
                throw new IllegalArgumentException("authorName cannot be null or empty");
                }
                if ((authorEmail == null) || (authorEmail.trim().isEmpty())) {
                throw new IllegalArgumentException("authorEmail cannot be null or empty");
                }
                if ((commentText == null) || (commentText.trim().isEmpty())) {
                throw new IllegalArgumentException("commentText cannot be null or empty");
                }
    }

    /**
    * Enforce entity invariants
    */
    private void enforceInvariants() {
    if (id == null) {
    throw new IllegalStateException("Entity must have a valid identity");
    }
    }

    private void validateConstructorArguments() {
    // Custom constructor validation
    }


    @Override
    public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    AddCommentCommand that = (AddCommentCommand) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "AddCommentCommand{"
    + "id=" + id
                + ", postId=" + postId
                + ", authorName=" + authorName
                + ", authorEmail=" + authorEmail
                + ", commentText=" + commentText
    + '}';
    }
    }
