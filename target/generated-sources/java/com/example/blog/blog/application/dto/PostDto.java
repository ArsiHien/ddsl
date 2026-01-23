    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: PostDto
    * Generated class: PostDto
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


class PostDto
 {

@EqualsAndHashCode.Include
@Getter
private final PostDtoId id;

            @Getter
            private final String postId;

            @Getter
            private final String title;

            @Getter
            private final String status;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public PostDto(String postId, String title, String status) {
                this.postId = postId;
                this.title = title;
                this.status = status;
            }


            /**
            * Business method: getPostId
            */
            public String getPostId()
             {
                validateBusinessRules();

                    return postId;

                enforceInvariants();

                    raiseDomainEvent(new PostDtoGetPostIdEvent(this.id));
                }

            /**
            * Business method: getTitle
            */
            public String getTitle()
             {
                validateBusinessRules();

                    return title;

                enforceInvariants();

                    raiseDomainEvent(new PostDtoGetTitleEvent(this.id));
                }

            /**
            * Business method: getStatus
            */
            public String getStatus()
             {
                validateBusinessRules();

                    return status;

                enforceInvariants();

                    raiseDomainEvent(new PostDtoGetStatusEvent(this.id));
                }


    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
                if ((postId == null) || (postId.trim().isEmpty())) {
                throw new IllegalArgumentException("postId cannot be null or empty");
                }
                if ((title == null) || (title.trim().isEmpty())) {
                throw new IllegalArgumentException("title cannot be null or empty");
                }
                if ((status == null) || (status.trim().isEmpty())) {
                throw new IllegalArgumentException("status cannot be null or empty");
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

        protected void raiseDomainEvent(DomainEvent event) {
        domainEvents.add(event);
        }

        public void clearDomainEvents() {
        domainEvents.clear();
        }


    @Override
    public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    PostDto that = (PostDto) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "PostDto{"
    + "id=" + id
                + ", postId=" + postId
                + ", title=" + title
                + ", status=" + status
    + '}';
    }
    }
