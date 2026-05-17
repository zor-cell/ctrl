CREATE EXTENSION IF NOT EXISTS vector;

--Function to compute the wilson score
--https://insightful-data-lab.com/2025/08/20/wilson-score-interval/
CREATE OR REPLACE FUNCTION wilson_score(upvotes integer, downvotes integer)
    RETURNS numeric AS $$
DECLARE
    n integer;
    p numeric;
    z numeric := 1.96;     -- z-score for 95% confidence
    z2 numeric := 3.8416;  -- z^2
BEGIN
    n := COALESCE(upvotes, 0) + COALESCE(downvotes, 0);

    IF n = 0 THEN
        RETURN 0.0;
    END IF;

    p := COALESCE(upvotes, 0)::numeric / n;

    RETURN (
        (p + z2 / (2 * n) - z * SQRT((p * (1 - p)) / n + z2 / (4 * n * n)))
            / (1 + z2 / n)
        );
END;
$$ LANGUAGE plpgsql IMMUTABLE;