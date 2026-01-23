    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: GetPostQuery
    * Generated class: GetPostQuery
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


class GetPostQuery
 {

@EqualsAndHashCode.Include
@Getter
private final GetPostQueryId id;

            @Getter
            private final String postId;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public GetPostQuery(String postId) {
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

                    raiseDomainEvent(new GetPostQueryGetPostIdEvent(this.id));
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
    GetPostQuery that = (GetPostQuery) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "GetPostQuery{"
    + "id=" + id
                + ", postId=" + postId
    + '}';
    }
    }
