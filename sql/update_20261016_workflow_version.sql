-- Workflow 发布版本快照。每次发布保留一份完整的定义、节点和连线 JSON，回滚会生成新的草稿版本。
CREATE TABLE IF NOT EXISTS public.ont_action_workflow_version (
    id bigint NOT NULL,
    workflow_id bigint NOT NULL,
    version integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PUBLISHED',
    snapshot_json text NOT NULL,
    creator_id bigint,
    create_by varchar(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updater_id bigint,
    update_by varchar(50),
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ont_action_workflow_version_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_ont_wf_version_workflow_version
    ON public.ont_action_workflow_version (workflow_id, version);

CREATE INDEX IF NOT EXISTS idx_ont_wf_version_workflow
    ON public.ont_action_workflow_version (workflow_id, version DESC);
