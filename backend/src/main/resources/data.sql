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

-- messages.system: NOT NULL without DEFAULT fails on existing rows via Hibernate ddl-auto
ALTER TABLE messages ADD COLUMN IF NOT EXISTS system BOOLEAN NOT NULL DEFAULT false^

-- Soft-delete for chat membership (delete chat for self without breaking the other user)
ALTER TABLE chat_members ADD COLUMN IF NOT EXISTS left_at TIMESTAMP^

-- Session history: older local databases used varchar ids and different duration/start columns.
ALTER TABLE session_history ADD COLUMN IF NOT EXISTS started_at TIMESTAMP^
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'session_history'
          AND column_name = 'start'
    ) THEN
        EXECUTE 'UPDATE session_history SET started_at = COALESCE(started_at, start, NOW()) WHERE started_at IS NULL';
    ELSE
        EXECUTE 'UPDATE session_history SET started_at = COALESCE(started_at, NOW()) WHERE started_at IS NULL';
    END IF;
END $$^
ALTER TABLE session_history ADD COLUMN IF NOT EXISTS seconds BIGINT^
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'session_history'
          AND column_name = 'minutes'
    ) THEN
        EXECUTE 'UPDATE session_history SET seconds = COALESCE(seconds, (minutes::BIGINT * 60), 0) WHERE seconds IS NULL';
    ELSE
        EXECUTE 'UPDATE session_history SET seconds = COALESCE(seconds, 0) WHERE seconds IS NULL';
    END IF;
END $$^
ALTER TABLE session_history ADD COLUMN IF NOT EXISTS posts_viewed INTEGER^
UPDATE session_history SET posts_viewed = COALESCE(posts_viewed, 0) WHERE posts_viewed IS NULL^
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'session_history'
          AND column_name = 'session_id'
          AND udt_name <> 'uuid'
    ) THEN
        EXECUTE 'ALTER TABLE session_history ALTER COLUMN session_id TYPE UUID USING session_id::UUID';
    END IF;
END $$^

-- Reactions: older local databases used one reaction per user/message.
-- The chat now supports multiple emojis per user/message, so content must be part of the key.
ALTER TABLE reactions ADD COLUMN IF NOT EXISTS content VARCHAR(255)^
UPDATE reactions SET content = '👍' WHERE content IS NULL^
ALTER TABLE reactions DROP CONSTRAINT IF EXISTS reactions_pkey^
ALTER TABLE reactions ADD CONSTRAINT reactions_pkey PRIMARY KEY (from_user_id, message_id, content)^
DELETE FROM reactions r
USING (
    SELECT ctid,
           ROW_NUMBER() OVER (
               PARTITION BY from_user_id, message_id
               ORDER BY created_at DESC NULLS LAST, content DESC
           ) AS rn
    FROM reactions
) ranked
WHERE r.ctid = ranked.ctid
  AND ranked.rn > 1^
ALTER TABLE reactions DROP CONSTRAINT IF EXISTS reactions_one_per_user_message^
ALTER TABLE reactions ADD CONSTRAINT reactions_one_per_user_message UNIQUE (from_user_id, message_id)^

UPDATE chats c
SET owner_id = (
    SELECT cm.user_id
    FROM chat_members cm
    WHERE cm.chat_id = c.chat_id
    ORDER BY cm.joined_at ASC NULLS LAST
    LIMIT 1
)
WHERE c.name IS NOT NULL
  AND c.owner_id IS NULL^
