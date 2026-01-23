    package com.example.blog.blog.application.dto;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

            import import java.util.List<CommentDto>;;


/**
* Generated Domain Entity: PostDetailDto
    * Generated class: PostDetailDto
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


class PostDetailDto
 {

@EqualsAndHashCode.Include
@Getter
private final PostDetailDtoId id;

            @Getter
            private final String postId;

            @Getter
            private final String title;

            @Getter
            private final String content;

            @Getter
            private final String status;

            @Getter
            private final String authorId;

            @Getter
            private final List comments;

            @Getter
            private final String publishedAt;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public PostDetailDto(String postId, String title, String content, String status, String authorId, List comments, String publishedAt) {
                this.postId = postId;
                this.title = title;
                this.content = content;
                this.status = status;
                this.authorId = authorId;
                this.comments = comments;
                this.publishedAt = publishedAt;
            }


            /**
            * Business method: getPostId
            */
            public String getPostId()
             {
                validateBusinessRules();

                    return postId;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetPostIdEvent(this.id));
                }

            /**
            * Business method: getTitle
            */
            public String getTitle()
             {
                validateBusinessRules();

                    return title;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetTitleEvent(this.id));
                }

            /**
            * Business method: getContent
            */
            public String getContent()
             {
                validateBusinessRules();

                    return content;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetContentEvent(this.id));
                }

            /**
            * Business method: getStatus
            */
            public String getStatus()
             {
                validateBusinessRules();

                    return status;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetStatusEvent(this.id));
                }

            /**
            * Business method: getAuthorId
            */
            public String getAuthorId()
             {
                validateBusinessRules();

                    return authorId;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetAuthorIdEvent(this.id));
                }

            /**
            * Business method: getComments
            */
            public List getComments()
             {
                validateBusinessRules();

                    return comments;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetCommentsEvent(this.id));
                }

            /**
            * Business method: getPublishedAt
            */
            public String getPublishedAt()
             {
                validateBusinessRules();

                    return publishedAt;

                enforceInvariants();

                    raiseDomainEvent(new PostDetailDtoGetPublishedAtEvent(this.id));
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
                if ((content == null) || (content.trim().isEmpty())) {
                throw new IllegalArgumentException("content cannot be null or empty");
                }
                if ((status == null) || (status.trim().isEmpty())) {
                throw new IllegalArgumentException("status cannot be null or empty");
                }
                if ((authorId == null) || (authorId.trim().isEmpty())) {
                throw new IllegalArgumentException("authorId cannot be null or empty");
                }
                if ((publishedAt == null) || (publishedAt.trim().isEmpty())) {
                throw new IllegalArgumentException("publishedAt cannot be null or empty");
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
    PostDetailDto that = (PostDetailDto) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "PostDetailDto{"
    + "id=" + id
                + ", postId=" + postId
                + ", title=" + title
                + ", content=" + content
                + ", status=" + status
                + ", authorId=" + authorId
                + ", comments=" + comments
                + ", publishedAt=" + publishedAt
    + '}';
    }
    }
