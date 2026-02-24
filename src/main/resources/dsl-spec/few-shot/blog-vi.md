---
id: ddsl-fewshot-002
category: FEW_SHOT
subcategory: informal_behaviors
language: vi
complexity: intermediate
---
This example shows how to translate very informal behavior descriptions into valid DDSL operations syntax. Input uses Vietnamese-style shorthand and compressed sentence descriptions.

User input (highly informal / sentence-style behaviors):
```
BoundedContext BlogManagement {
    domain {
        Aggregate Post {
            post id is UUID for identity
            title is text, required, max 200
            content is text, required
            author is Author type
            comments is list of Comment
            published at is optional datetime
            status is text

            operations {
                publishing post {
                    title and content must not be empty, change status to "PUBLISHED", set publishedAt to now, fire PostPublished event with postId and title
                }
                adding comment with author and text {
                    text cannot be empty, create new Comment, add it to comments list
                }
            }
        }

        Entity Comment {
            comment id is UUID for identity
            author is text, mandatory
            text is text, mandatory
            created at is timestamp
        }

        ValueObject Author {
            name is text, required
            email is text, must be valid email
        }
    }
    events {
        DomainEvent PostPublished {
            post id is UUID
            title is text
            published at is timestamp
        }
    }
    repositories {
        Repository PostRepository for Post {
            findById(id: UUID): Post?
            findByAuthor(authorName: String): List<Post>
            save(post: Post): Void
            delete(post: Post): Void
        }
    }
}
```

Corrected DDSL output:
```
BoundedContext BlogManagement {
    domain {
        Aggregate Post {
            @identity postId: UUID
            title: String @required @maxLength(200)
            content: String @required
            author: Author
            comments: List<Comment>
            publishedAt: DateTime?
            status: String

            operations {
                when publishing post {
                    require that title is not empty
                    require that content is not empty
                    then set status to "PUBLISHED"
                    then set publishedAt to now
                    emit event PostPublished with postId, title
                }
                when adding comment with author, text {
                    require that text is not empty
                    then create new Comment
                    then add it to comments list
                }
            }
        }

        Entity Comment {
            @identity commentId: UUID
            author: String @required
            text: String @required
            createdAt: DateTime
        }

        ValueObject Author {
            name: String @required
            email: String @email
        }
    }
    events {
        DomainEvent PostPublished {
            postId: UUID
            title: String
            publishedAt: DateTime
        }
    }
    repositories {
        Repository PostRepository for Post {
            findById(id: UUID): Post?
            findByAuthor(authorName: String): List<Post>
            save(post: Post): Void
            delete(post: Post): Void
        }
    }
}
```

Key transformations:
- Compressed sentence `title and content must not be empty, change status to "PUBLISHED", set publishedAt to now, fire PostPublished event with postId and title` → expanded into separate clauses: `require that` + `then set` + `emit event`
- `fire PostPublished event with postId and title` → `emit event PostPublished with postId, title` — "fire"→"emit", "and"→comma
- `adding comment with author and text` → `when adding comment with author, text` — add "when", "and"→comma in param list
- `text cannot be empty, create new Comment, add it to comments list` → 3 separate clauses with `require that` / `then`
- `post id is UUID for identity` → `@identity postId: UUID`
- `title is text, required, max 200` → `title: String @required @maxLength(200)` — "max 200"→@maxLength(200)
- `author is Author type` → `author: Author` — drop "type" suffix
- `comments is list of Comment` → `comments: List<Comment>`
- `published at is optional datetime` → `publishedAt: DateTime?` — "optional"→?
- `must be valid email` → `@email`
- `mandatory` → `@required`
