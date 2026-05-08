-- Creates an index for the cosine distance comparison
CREATE INDEX IF NOT EXISTS posts_embedding_idx ON posts USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS topics_embedding_idx ON topics USING hnsw (embedding vector_cosine_ops);