CREATE TABLE repositories (
    id            BIGSERIAL     PRIMARY KEY,
    github_id     BIGINT        NOT NULL,
    name          VARCHAR(255)  NOT NULL,
    full_name     VARCHAR(512)  NOT NULL,
    description   TEXT,
    owner_login   VARCHAR(255)  NOT NULL,
    language      VARCHAR(100),
    html_url      TEXT          NOT NULL,
    stars         INTEGER       NOT NULL DEFAULT 0,
    forks         INTEGER       NOT NULL DEFAULT 0,
    watchers      INTEGER       NOT NULL DEFAULT 0,
    open_issues   INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ,
    last_updated  TIMESTAMPTZ   NOT NULL,
    pushed_at     TIMESTAMPTZ,
    fetched_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_repositories_github_id UNIQUE (github_id)
);

CREATE INDEX idx_repositories_stars ON repositories (stars DESC);
CREATE INDEX idx_repositories_language_lower ON repositories (lower(language));
