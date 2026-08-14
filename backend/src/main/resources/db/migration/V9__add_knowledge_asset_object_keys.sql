alter table knowledge_assets add column if not exists original_object_key varchar(1200);
alter table knowledge_asset_pages add column if not exists preview_object_key varchar(1200);
alter table knowledge_asset_media add column if not exists object_key varchar(1200);
alter table knowledge_asset_media add column if not exists thumbnail_object_key varchar(1200);
