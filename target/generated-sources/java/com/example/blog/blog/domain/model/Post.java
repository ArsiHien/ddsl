    package com.example.blog.blog.domain.model;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

            import import com.example.blog.blog.domain.model.PostId;;
            import import java.time.Instant;;
            import import java.util.List<Comment>;;
            import java.text.Normalizer;


/**
* Generated Domain Entity: Post
    * Generated class: Post
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


class Post
 {

@EqualsAndHashCode.Include
@Getter
private final PostId id;

            @Getter
            private final AuthorId authorId;

            @Getter
            private final PostTitle title;

            @Getter
            private final PostContent content;

            @Getter
            private final PostStatus status = PostStatus.DRAFT;

            @Getter
            private final List comments = new ArrayList<>();

            @Getter
            private final Instant publishedAt;

            @Getter
            private final Instant createdAt;

            @Getter
            private final Instant updatedAt;


@Getter
private final List<DomainEvent> domainEvents = new ArrayList<>();


            /**
            * Domain entity constructor with business validation
            */
            public Post(PostId id, AuthorId authorId, PostTitle title, PostContent content, PostStatus status, List comments, Instant publishedAt, Instant createdAt, Instant updatedAt) {
                this.id = id;
                this.authorId = authorId;
                this.title = title;
                this.content = content;
                this.status = status;
                this.comments = comments;
                this.publishedAt = publishedAt;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
            }


            /**
            * Business method: updateTitle
            */
            public void updateTitle(PostTitle newTitle)
             {
                validateBusinessRules();

                    if (status == PostStatus.PUBLISHED) {
                  throw new PostException("Cannot update title of published post");
                }
                this.title = Objects.requireNonNull(newTitle);
                this.updatedAt = Instant.now();

                enforceInvariants();

                    raiseDomainEvent(new PostUpdateTitleEvent(this.id));
                }

            /**
            * Business method: updateContent
            */
            public void updateContent(PostContent newContent)
             {
                validateBusinessRules();

                    if (status == PostStatus.ARCHIVED) {
                  throw new PostException("Cannot update archived post");
                }
                this.content = Objects.requireNonNull(newContent);
                this.updatedAt = Instant.now();

                enforceInvariants();

                    raiseDomainEvent(new PostUpdateContentEvent(this.id));
                }

            /**
            * Business method: publish
            */
            public void publish()
             {
                validateBusinessRules();

                    if (status == PostStatus.PUBLISHED) {
                  throw new PostException("Post is already published");
                }
                checkInvariants();
                this.status = PostStatus.PUBLISHED;
                this.publishedAt = Instant.now();
                this.updatedAt = Instant.now();
                raiseEvent(new PostPublishedEvent(id, title, publishedAt));

                enforceInvariants();

                    raiseDomainEvent(new PostPublishEvent(this.id));
                }

            /**
            * Business method: archive
            */
            public void archive()
             {
                validateBusinessRules();

                    if (status == PostStatus.ARCHIVED) {
                  throw new PostException("Post is already archived");
                }
                this.status = PostStatus.ARCHIVED;
                this.updatedAt = Instant.now();
                raiseEvent(new PostArchivedEvent(id));

                enforceInvariants();

                    raiseDomainEvent(new PostArchiveEvent(this.id));
                }

            /**
            * Business method: addComment
            */
            public void addComment(String authorName, Email authorEmail, String commentText)
             {
                validateBusinessRules();

                    if (status != PostStatus.PUBLISHED) {
                  throw new PostException("Can only comment on published posts");
                }
                Comment comment = new Comment(
                  CommentId.generate(),
                  authorName,
                  authorEmail,
                  commentText,
                  Instant.now()
                );
                comments.add(comment);
                this.updatedAt = Instant.now();
                checkInvariants();
                raiseEvent(new CommentAddedEvent(id, comment.getId()));

                enforceInvariants();

                    raiseDomainEvent(new PostAddCommentEvent(this.id));
                }

            /**
            * Business method: removeComment
            */
            public void removeComment(CommentId commentId)
             {
                validateBusinessRules();

                    boolean removed = comments.removeIf(c -> c.getId().equals(commentId));
                if (!removed) {
                  throw new CommentNotFoundException(commentId);
                }
                this.updatedAt = Instant.now();

                enforceInvariants();

                    raiseDomainEvent(new PostRemoveCommentEvent(this.id));
                }

            /**
            * Business method: getAuthorId
            */
            public AuthorId getAuthorId()
             {
                validateBusinessRules();

                    return authorId;

                enforceInvariants();

                    raiseDomainEvent(new PostGetAuthorIdEvent(this.id));
                }

            /**
            * Business method: getTitle
            */
            public PostTitle getTitle()
             {
                validateBusinessRules();

                    return title;

                enforceInvariants();

                    raiseDomainEvent(new PostGetTitleEvent(this.id));
                }

            /**
            * Business method: getContent
            */
            public PostContent getContent()
             {
                validateBusinessRules();

                    return content;

                enforceInvariants();

                    raiseDomainEvent(new PostGetContentEvent(this.id));
                }

            /**
            * Business method: getStatus
            */
            public PostStatus getStatus()
             {
                validateBusinessRules();

                    return status;

                enforceInvariants();

                    raiseDomainEvent(new PostGetStatusEvent(this.id));
                }

            /**
            * Business method: getComments
            */
            public List getComments()
             {
                validateBusinessRules();

                    return comments;

                enforceInvariants();

                    raiseDomainEvent(new PostGetCommentsEvent(this.id));
                }

            /**
            * Business method: getPublishedAt
            */
            public Instant getPublishedAt()
             {
                validateBusinessRules();

                    return publishedAt;

                enforceInvariants();

                    raiseDomainEvent(new PostGetPublishedAtEvent(this.id));
                }

            /**
            * Business method: getCreatedAt
            */
            public Instant getCreatedAt()
             {
                validateBusinessRules();

                    return createdAt;

                enforceInvariants();

                    raiseDomainEvent(new PostGetCreatedAtEvent(this.id));
                }

            /**
            * Business method: getUpdatedAt
            */
            public Instant getUpdatedAt()
             {
                validateBusinessRules();

                    return updatedAt;

                enforceInvariants();

                    raiseDomainEvent(new PostGetUpdatedAtEvent(this.id));
                }

            /**
            * Business method: equals
            */
            public boolean equals(Object obj)
             {
                validateBusinessRules();

                    if (this == obj) return true;
                if (obj == null || getClass() != obj.getClass()) return false;
                Post that = (Post) obj;
                return Objects.equals(id, that.id);

                enforceInvariants();

                    raiseDomainEvent(new PostEqualsEvent(this.id));
                }

            /**
            * Business method: hashCode
            */
            public int hashCode()
             {
                validateBusinessRules();

                    return Objects.hash(id);

                enforceInvariants();

                    raiseDomainEvent(new PostHashCodeEvent(this.id));
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
    Post that = (Post) obj;
    return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
    return Objects.hash(id);
    }

    @Override
    public String toString() {
    return "Post{"
    + "id=" + id
                + ", authorId=" + authorId
                + ", title=" + title
                + ", content=" + content
                + ", status=" + status
                + ", comments=" + comments
                + ", publishedAt=" + publishedAt
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
    + '}';
    }
    }
