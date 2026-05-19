create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    first_name text,
    last_name text,
    display_email text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.collections (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.algorithm_sheets (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    collection_id text not null,
    name text not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.algorithms (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    sheet_id text not null,
    name text not null,
    sequence text not null,
    position integer not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.scrambles (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    collection_id text not null,
    name text not null,
    sequence text not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.tags (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.sheet_tags (
    sheet_id text not null,
    tag_id text not null,
    owner_id uuid not null references auth.users(id) on delete cascade,
    updated_at bigint not null,
    deleted_at bigint,
    primary key (sheet_id, tag_id)
);

create table if not exists public.scramble_tags (
    scramble_id text not null,
    tag_id text not null,
    owner_id uuid not null references auth.users(id) on delete cascade,
    updated_at bigint not null,
    deleted_at bigint,
    primary key (scramble_id, tag_id)
);

create table if not exists public.timer_entries (
    id text primary key,
    owner_id uuid not null references auth.users(id) on delete cascade,
    duration_millis bigint not null,
    solved_at bigint not null,
    updated_at bigint not null,
    deleted_at bigint
);

create table if not exists public.user_preferences (
    owner_id uuid primary key references auth.users(id) on delete cascade,
    app_appearance text not null,
    cube_theme text not null,
    local_cube_cache_enabled boolean not null,
    session_persistence_enabled boolean not null,
    updated_at bigint not null,
    deleted_at bigint
);

alter table public.profiles enable row level security;
alter table public.collections enable row level security;
alter table public.algorithm_sheets enable row level security;
alter table public.algorithms enable row level security;
alter table public.scrambles enable row level security;
alter table public.tags enable row level security;
alter table public.sheet_tags enable row level security;
alter table public.scramble_tags enable row level security;
alter table public.timer_entries enable row level security;
alter table public.user_preferences enable row level security;

create policy "profiles own row" on public.profiles
    for all using (id = auth.uid()) with check (id = auth.uid());

create policy "collections owner" on public.collections
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "algorithm_sheets owner" on public.algorithm_sheets
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "algorithms owner" on public.algorithms
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "scrambles owner" on public.scrambles
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "tags owner" on public.tags
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "sheet_tags owner" on public.sheet_tags
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "scramble_tags owner" on public.scramble_tags
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "timer_entries owner" on public.timer_entries
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());
create policy "user_preferences owner" on public.user_preferences
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

grant usage on schema public to authenticated;
grant select, insert, update, delete on
    public.profiles,
    public.collections,
    public.algorithm_sheets,
    public.algorithms,
    public.scrambles,
    public.tags,
    public.sheet_tags,
    public.scramble_tags,
    public.timer_entries,
    public.user_preferences
to authenticated;
