    package com.example.blog.blog.domain.factory;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;



/**
* Generated Domain Entity: PostFactory
    * Generated class: PostFactory
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


class PostFactory
 {

@EqualsAndHashCode.Include
@Getter
private final PostFactoryId id;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();



            /**
            * Business method: createDraft
            */
            public Post createDraft(AuthorId authorId, PostTitle title, PostContent content)
             {
                validateBusinessRules();

                    PostId id = PostId.generate();
                Instant now = Instant.now();
                return new Post(
                  id,
                  authorId,
                  title,
                  content,
                  PostStatus.DRAFT,
                  now,
                  now
                );

                enforceInvariants();

                    raiseDomainEvent(new PostFactoryCreateDraftEvent(this.id));
                }


    /**
    * Business rule validation
    */
    private void validateBusinessRules() {
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
    PostFactory that = (PostFactory) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "PostFactory{"
    + "id=" + id
    + '}';
    }
    }
