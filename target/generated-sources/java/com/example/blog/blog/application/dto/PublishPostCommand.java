    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: PublishPostCommand
    * Generated class: PublishPostCommand
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


class PublishPostCommand
 {

@EqualsAndHashCode.Include
@Getter
private final PublishPostCommandId id;

            @Getter
            private final String postId;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public PublishPostCommand(String postId) {
                this.postId = postId;
            }


            /**
            * Business method: getPostId
            */
            public String getPostId()
             {
                validateBusinessRules();

                    return postId;

                enforceInvariants();

                    raiseDomainEvent(new PublishPostCommandGetPostIdEvent(this.id));
                }


    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
                if ((postId == null) || (postId.trim().isEmpty())) {
                throw new IllegalArgumentException("postId cannot be null or empty");
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
    PublishPostCommand that = (PublishPostCommand) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "PublishPostCommand{"
    + "id=" + id
                + ", postId=" + postId
    + '}';
    }
    }
