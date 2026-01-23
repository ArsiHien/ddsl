package com.example.blog.blog.domain.repository;

/**
 * Generated interface: PostRepository
 */
public interface PostRepository {

    public Optional<Post> findById(PostId id);

    public void save(Post post);

    public void delete(PostId id);

    public List findByAuthorId(AuthorId authorId);

    public List findByStatus(PostStatus status);

    public List findPublishedPosts();

}
