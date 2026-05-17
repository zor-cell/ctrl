-- Creates an index for post comparison
CREATE INDEX IF NOT EXISTS idx_posts_public_recency ON posts (created_at DESC) WHERE is_public = true^
CREATE INDEX IF NOT EXISTS idx_posts_private_recency ON posts (created_at DESC) WHERE is_public = false^
CREATE INDEX IF NOT EXISTS idx_posts_public_popularity ON posts (upvotes DESC) WHERE is_public = true^
CREATE INDEX IF NOT EXISTS idx_posts_private_popularity ON posts (upvotes DESC) WHERE is_public = false^
CREATE INDEX IF NOT EXISTS idx_posts_embedding ON posts USING hnsw (embedding vector_cosine_ops)^
CREATE INDEX IF NOT EXISTS idx_topics_embedding ON topics USING hnsw (embedding vector_cosine_ops)^

-- Sets the hnsw index search depth
SET hnsw.ef_search = 100^

--- Creates a trigger for post votes to be visible on the post entity
CREATE OR REPLACE FUNCTION update_post_votes_count()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        IF NEW.vote_type = 'up' THEN
            UPDATE posts SET upvotes = COALESCE(upvotes, 0) + 1 WHERE post_id = NEW.post_id;
        ELSIF NEW.vote_type = 'down' THEN
            UPDATE posts SET downvotes = COALESCE(downvotes, 0) + 1 WHERE post_id = NEW.post_id;
        END IF;
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        IF OLD.vote_type = 'up' THEN
            UPDATE posts SET upvotes = GREATEST(COALESCE(upvotes, 0) - 1, 0) WHERE post_id = OLD.post_id;
        ELSIF OLD.vote_type = 'down' THEN
            UPDATE posts SET downvotes = GREATEST(COALESCE(downvotes, 0) - 1, 0) WHERE post_id = OLD.post_id;
        END IF;
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        IF OLD.vote_type IS DISTINCT FROM NEW.vote_type THEN
            IF OLD.vote_type = 'up' THEN
                UPDATE posts SET upvotes = GREATEST(COALESCE(upvotes, 0) - 1, 0) WHERE post_id = OLD.post_id;
            ELSIF OLD.vote_type = 'down' THEN
                UPDATE posts SET downvotes = GREATEST(COALESCE(downvotes, 0) - 1, 0) WHERE post_id = OLD.post_id;
            END IF;
            IF NEW.vote_type = 'up' THEN
                UPDATE posts SET upvotes = COALESCE(upvotes, 0) + 1 WHERE post_id = NEW.post_id;
            ELSIF NEW.vote_type = 'down' THEN
                UPDATE posts SET downvotes = COALESCE(downvotes, 0) + 1 WHERE post_id = NEW.post_id;
            END IF;
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql^

--- Creates a trigger for source votes to be visible on the post entity
DROP TRIGGER IF EXISTS trg_update_post_votes ON post_votes^
CREATE TRIGGER trg_update_post_votes
    AFTER INSERT OR UPDATE OR DELETE ON post_votes
    FOR EACH ROW EXECUTE FUNCTION update_post_votes_count()^

CREATE OR REPLACE FUNCTION update_source_votes_count()
    RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') THEN
        IF NEW.vote_type = 'up' THEN
            UPDATE sources SET upvotes = COALESCE(upvotes, 0) + 1 WHERE source_id = NEW.source_id;
        ELSIF NEW.vote_type = 'down' THEN
            UPDATE sources SET downvotes = COALESCE(downvotes, 0) + 1 WHERE source_id = NEW.source_id;
        END IF;
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        IF OLD.vote_type = 'up' THEN
            UPDATE sources SET upvotes = GREATEST(COALESCE(upvotes, 0) - 1, 0) WHERE source_id = OLD.source_id;
        ELSIF OLD.vote_type = 'down' THEN
            UPDATE sources SET downvotes = GREATEST(COALESCE(downvotes, 0) - 1, 0) WHERE source_id = OLD.source_id;
        END IF;
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        IF OLD.vote_type IS DISTINCT FROM NEW.vote_type THEN
            IF OLD.vote_type = 'up' THEN
                UPDATE sources SET upvotes = GREATEST(COALESCE(upvotes, 0) - 1, 0) WHERE source_id = OLD.source_id;
            ELSIF OLD.vote_type = 'down' THEN
                UPDATE sources SET downvotes = GREATEST(COALESCE(downvotes, 0) - 1, 0) WHERE source_id = OLD.source_id;
            END IF;
            IF NEW.vote_type = 'up' THEN
                UPDATE sources SET upvotes = COALESCE(upvotes, 0) + 1 WHERE source_id = NEW.source_id;
            ELSIF NEW.vote_type = 'down' THEN
                UPDATE sources SET downvotes = COALESCE(downvotes, 0) + 1 WHERE source_id = NEW.source_id;
            END IF;
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql^

DROP TRIGGER IF EXISTS trg_update_source_votes ON source_votes^
CREATE TRIGGER trg_update_source_votes
    AFTER INSERT OR UPDATE OR DELETE ON source_votes
    FOR EACH ROW EXECUTE FUNCTION update_source_votes_count()^