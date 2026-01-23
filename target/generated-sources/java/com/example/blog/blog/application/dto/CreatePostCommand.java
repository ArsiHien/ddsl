    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: CreatePostCommand
    * Generated class: CreatePostCommand
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


class CreatePostCommand
 {

@EqualsAndHashCode.Include
@Getter
private final CreatePostCommandId id;

            @Getter
            private final String authorId;

            @Getter
            private final String title;

            @Getter
            private final String content;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public CreatePostCommand(String authorId, String title, String content) {
                this.authorId = authorId;
                this.title = title;
                this.content = content;
            }


            /**
            * Business method: getAuthorId
            */
            public String getAuthorId()
             {
                validateBusinessRules();

                    return authorId;

                enforceInvariants();

                    raiseDomainEvent(new CreatePostCommandGetAuthorIdEvent(this.id));
                }

            /**
            * Business method: getTitle
            */
            public String getTitle()
             {
                validateBusinessRules();

                    return title;

                enforceInvariants();

                    raiseDomainEvent(new CreatePostCommandGetTitleEvent(this.id));
                }

            /**
            * Business method: getContent
            */
            public String getContent()
             {
                validateBusinessRules();

                    return content;

                enforceInvariants();

                    raiseDomainEvent(new CreatePostCommandGetContentEvent(this.id));
                }


    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
                if ((authorId == null) || (authorId.trim().isEmpty())) {
                throw new IllegalArgumentException("authorId cannot be null or empty");
                }
                if ((title == null) || (title.trim().isEmpty())) {
                throw new IllegalArgumentException("title cannot be null or empty");
                }
                if ((content == null) || (content.trim().isEmpty())) {
                throw new IllegalArgumentException("content cannot be null or empty");
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
    CreatePostCommand that = (CreatePostCommand) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "CreatePostCommand{"
    + "id=" + id
                + ", authorId=" + authorId
                + ", title=" + title
                + ", content=" + content
    + '}';
    }
    }
