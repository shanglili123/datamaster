-- ============================================================
-- PostgreSQL 备份: db=datamaster_movie host=192.168.93.174:5432
-- 导出时间: 2026-09-12 21:07:06
-- 生成工具: backup_database.py (psycopg2, 无 pg_dump 依赖)
-- 建议以 superuser 回放: python run_migration.py <本文件> --db datamaster_movie
-- ============================================================
SET statement_timeout = 0;
SET client_encoding = 'UTF8';

CREATE TABLE "dm_actor" (
    "id" bigint NOT NULL,
    "actor_no" character varying(20) NOT NULL,
    "name" character varying(50) NOT NULL,
    "gender" character varying(10) NOT NULL,
    "birth_date" date NOT NULL,
    "nationality" character varying(20) NOT NULL,
    "debut_year" integer NOT NULL,
    CONSTRAINT dm_actor_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_actor_id_seq";
ALTER SEQUENCE "dm_actor_id_seq" OWNED BY "dm_actor"."id";
ALTER TABLE "dm_actor" ALTER COLUMN "id" SET DEFAULT nextval('dm_actor_id_seq'::regclass);
SELECT setval('dm_actor_id_seq', 30, TRUE);

ALTER TABLE "dm_actor" ADD CONSTRAINT "dm_actor_no_uk" UNIQUE (actor_no);

COMMENT ON TABLE "dm_actor" IS '演员(电影库-演员)';
COMMENT ON COLUMN "dm_actor"."actor_no" IS '演员编号';
COMMENT ON COLUMN "dm_actor"."debut_year" IS '出道年份';

INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (1, 'A001', '陈杰', '男', '1994-04-01'::date, '中国', 1999);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (2, 'A002', 'John Williams', '女', '1977-01-15'::date, '美国', 1984);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (3, 'A003', 'Alexander Moore', '男', '1987-01-30'::date, '英国', 1994);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (4, 'A004', '李怡', '女', '1984-01-20'::date, '日本', 1985);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (5, 'A005', 'Leo Williams', '男', '1984-10-05'::date, '法国', 1996);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (6, 'A006', '郑欣', '女', '1976-03-22'::date, '韩国', 2014);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (7, 'A007', '赵涵', '男', '1963-02-13'::date, '中国', 1980);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (8, 'A008', 'Thomas Taylor', '女', '1963-07-31'::date, '美国', 1992);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (9, 'A009', 'John Jackson', '男', '1972-06-20'::date, '英国', 2007);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (10, 'A010', '郑琳', '女', '1967-04-18'::date, '日本', 2001);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (11, 'A011', '孙杰', '男', '1990-01-23'::date, '中国', 1995);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (12, 'A012', 'Robert King', '女', '1988-06-25'::date, '美国', 1988);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (13, 'A013', 'George Wilson', '男', '1967-05-01'::date, '英国', 2007);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (14, 'A014', '马婷', '女', '1988-09-15'::date, '日本', 2013);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (15, 'A015', 'Thomas Thomas', '男', '1994-06-19'::date, '法国', 1981);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (16, 'A016', '黄娜', '女', '1974-02-24'::date, '韩国', 2003);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (17, 'A017', '周磊', '男', '1969-06-18'::date, '中国', 2014);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (18, 'A018', 'Henry Davis', '女', '1989-05-26'::date, '美国', 2009);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (19, 'A019', 'Leo Lee', '男', '1966-05-29'::date, '英国', 1994);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (20, 'A020', '陈霞', '女', '1993-06-01'::date, '日本', 2013);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (21, 'A021', '林明', '男', '1993-07-05'::date, '中国', 2015);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (22, 'A022', 'Oscar Young', '女', '1977-11-30'::date, '美国', 2001);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (23, 'A023', 'Thomas Jones', '男', '1982-11-09'::date, '英国', 2009);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (24, 'A024', '张娜', '女', '1964-12-01'::date, '日本', 1987);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (25, 'A025', 'William Martin', '男', '1986-10-02'::date, '法国', 1982);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (26, 'A026', '马燕', '女', '1986-09-24'::date, '韩国', 2007);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (27, 'A027', '何明', '男', '1984-10-25'::date, '中国', 1978);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (28, 'A028', 'John Allen', '女', '1993-09-06'::date, '美国', 1995);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (29, 'A029', 'Henry Brown', '男', '1973-03-01'::date, '英国', 2005);
INSERT INTO "dm_actor" ("id", "actor_no", "name", "gender", "birth_date", "nationality", "debut_year") VALUES (30, 'A030', '杨怡', '女', '1960-02-23'::date, '日本', 1994);

CREATE TABLE "dm_cast" (
    "id" bigint NOT NULL,
    "movie_no" character varying(20) NOT NULL,
    "actor_no" character varying(20) NOT NULL,
    "role_name" character varying(50) NOT NULL,
    "billing_order" integer NOT NULL,
    CONSTRAINT dm_cast_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_cast_id_seq";
ALTER SEQUENCE "dm_cast_id_seq" OWNED BY "dm_cast"."id";
ALTER TABLE "dm_cast" ALTER COLUMN "id" SET DEFAULT nextval('dm_cast_id_seq'::regclass);
SELECT setval('dm_cast_id_seq', 181, TRUE);

ALTER TABLE "dm_cast" ADD CONSTRAINT "dm_cast_uk" UNIQUE (movie_no, actor_no);

COMMENT ON TABLE "dm_cast" IS '演员表(电影库-演员表)';
COMMENT ON COLUMN "dm_cast"."movie_no" IS '电影编号(与 dm_movie.movie_no 对齐)';
COMMENT ON COLUMN "dm_cast"."actor_no" IS '演员编号(与 dm_actor.actor_no 对齐)';
COMMENT ON COLUMN "dm_cast"."role_name" IS '角色名';
COMMENT ON COLUMN "dm_cast"."billing_order" IS '番位: 1=主演';

INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (1, 'M001', 'A026', '何灵', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (2, 'M001', 'A023', '罗毅', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (3, 'M001', 'A017', '苏哲', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (4, 'M001', 'A015', '袁野', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (5, 'M002', 'A018', '吴悠', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (6, 'M002', 'A008', '萧然', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (7, 'M002', 'A030', '苏芮', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (8, 'M003', 'A015', '秦朗', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (9, 'M003', 'A005', '阿泽', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (10, 'M003', 'A026', '萧然', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (11, 'M004', 'A022', '夏栀', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (12, 'M004', 'A017', '秦朗', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (13, 'M004', 'A018', '夏栀', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (14, 'M004', 'A020', '吴悠', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (15, 'M004', 'A011', '郑凯', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (16, 'M004', 'A025', '吴川', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (17, 'M005', 'A020', '叶芸', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (18, 'M005', 'A027', '韩东', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (19, 'M005', 'A024', '林晚', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (20, 'M005', 'A017', '叶城', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (21, 'M005', 'A014', '苏芮', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (22, 'M005', 'A018', '秦雨', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (23, 'M006', 'A029', '叶城', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (24, 'M006', 'A006', '陆薇', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (25, 'M006', 'A024', '罗曼', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (26, 'M006', 'A016', '萧然', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (27, 'M006', 'A015', '韩东', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (28, 'M006', 'A009', '方野', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (29, 'M007', 'A027', '郑凯', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (30, 'M007', 'A021', '杜衡', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (31, 'M007', 'A009', '方野', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (32, 'M007', 'A025', '夏阳', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (33, 'M008', 'A021', '林深', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (34, 'M008', 'A008', '郑妍', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (35, 'M008', 'A009', '沈舟', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (36, 'M008', 'A015', '阿泽', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (37, 'M008', 'A003', '秦朗', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (38, 'M008', 'A023', '袁野', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (39, 'M009', 'A008', '江北', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (40, 'M009', 'A009', '叶城', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (41, 'M009', 'A011', '夏阳', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (42, 'M009', 'A029', '秦朗', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (43, 'M009', 'A018', '唐宁', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (44, 'M010', 'A005', '叶城', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (45, 'M010', 'A008', '袁梦', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (46, 'M010', 'A013', '杜衡', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (47, 'M011', 'A023', '叶城', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (48, 'M011', 'A007', '罗毅', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (49, 'M011', 'A003', '林深', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (50, 'M011', 'A014', '陈曦', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (51, 'M012', 'A011', '陆沉', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (52, 'M012', 'A018', '韩雪', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (53, 'M012', 'A015', '程野', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (54, 'M012', 'A014', '唐宁', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (55, 'M012', 'A002', '韩雪', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (56, 'M012', 'A007', '陆沉', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (57, 'M013', 'A013', '吴川', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (58, 'M013', 'A029', '何骏', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (59, 'M013', 'A025', '萧然', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (60, 'M013', 'A019', '叶城', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (61, 'M013', 'A023', '夏阳', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (62, 'M013', 'A001', '江北', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (63, 'M014', 'A016', '罗曼', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (64, 'M014', 'A001', '苏哲', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (65, 'M014', 'A012', '温言', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (66, 'M014', 'A010', '许萌', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (67, 'M014', 'A025', '韩东', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (68, 'M014', 'A013', '萧然', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (69, 'M015', 'A018', '唐宁', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (70, 'M015', 'A024', '夏栀', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (71, 'M015', 'A029', '方野', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (72, 'M015', 'A030', '程语', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (73, 'M015', 'A026', '萧然', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (74, 'M015', 'A020', '韩雪', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (75, 'M016', 'A016', '顾念', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (76, 'M016', 'A008', '温言', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (77, 'M016', 'A009', '沈舟', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (78, 'M016', 'A014', '苏芮', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (79, 'M017', 'A001', '老马', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (80, 'M017', 'A013', '袁野', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (81, 'M017', 'A011', '夏阳', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (82, 'M017', 'A022', '唐宁', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (83, 'M017', 'A027', '顾远', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (84, 'M017', 'A029', '沈舟', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (85, 'M018', 'A027', '沈舟', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (86, 'M018', 'A015', '袁野', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (87, 'M018', 'A030', '何灵', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (88, 'M018', 'A005', '方野', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (89, 'M019', 'A030', '温言', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (90, 'M019', 'A013', '何骏', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (91, 'M019', 'A019', '江北', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (92, 'M020', 'A003', '郑凯', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (93, 'M020', 'A021', '陆沉', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (94, 'M020', 'A014', '顾念', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (95, 'M021', 'A028', '陆薇', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (96, 'M021', 'A015', '陆沉', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (97, 'M021', 'A006', '韩雪', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (98, 'M021', 'A002', '罗曼', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (99, 'M022', 'A013', '顾远', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (100, 'M022', 'A011', '陆沉', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (101, 'M022', 'A007', '林深', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (102, 'M022', 'A015', '杜衡', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (103, 'M022', 'A025', '夏阳', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (104, 'M023', 'A009', '周锋', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (105, 'M023', 'A025', '方野', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (106, 'M023', 'A027', '陈默', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (107, 'M023', 'A014', '苏晴', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (108, 'M023', 'A030', '许萌', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (109, 'M023', 'A003', '陆沉', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (110, 'M024', 'A001', '杜衡', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (111, 'M024', 'A024', '沈瑶', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (112, 'M024', 'A018', '袁梦', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (113, 'M024', 'A002', '何灵', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (114, 'M024', 'A012', '顾念', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (115, 'M024', 'A008', '林晚', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (116, 'M025', 'A025', '何骏', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (117, 'M025', 'A021', '陆沉', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (118, 'M025', 'A002', '何灵', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (119, 'M026', 'A008', '何灵', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (120, 'M026', 'A007', '罗毅', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (121, 'M026', 'A027', '韩东', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (122, 'M027', 'A020', '秦雨', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (123, 'M027', 'A005', '陈默', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (124, 'M027', 'A008', '萧然', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (125, 'M028', 'A016', '何灵', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (126, 'M028', 'A022', '顾念', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (127, 'M028', 'A004', '陈曦', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (128, 'M028', 'A019', '程野', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (129, 'M029', 'A015', '叶城', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (130, 'M029', 'A023', '阿泽', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (131, 'M029', 'A009', '何骏', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (132, 'M029', 'A025', '许峰', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (133, 'M030', 'A006', '林可', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (134, 'M030', 'A020', '苏晴', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (135, 'M030', 'A024', '沈瑶', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (136, 'M030', 'A023', '周锋', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (137, 'M030', 'A004', '吴悠', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (138, 'M031', 'A010', '程语', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (139, 'M031', 'A004', '陈曦', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (140, 'M031', 'A019', '秦朗', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (141, 'M031', 'A001', '老马', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (142, 'M032', 'A019', '夏阳', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (143, 'M032', 'A022', '江北', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (144, 'M032', 'A030', '杜若', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (145, 'M032', 'A013', '郑凯', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (146, 'M032', 'A023', '郑凯', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (147, 'M033', 'A003', '秦朗', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (148, 'M033', 'A019', '江北', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (149, 'M033', 'A023', '程野', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (150, 'M033', 'A027', '罗毅', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (151, 'M034', 'A004', '夏栀', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (152, 'M034', 'A023', '陆沉', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (153, 'M034', 'A025', '何骏', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (154, 'M034', 'A010', '江北', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (155, 'M035', 'A026', '程语', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (156, 'M035', 'A019', '何骏', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (157, 'M035', 'A002', '杜若', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (158, 'M036', 'A018', '苏晴', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (159, 'M036', 'A014', '杜若', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (160, 'M036', 'A022', '温言', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (161, 'M036', 'A012', '顾念', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (162, 'M036', 'A003', '沈舟', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (163, 'M037', 'A001', '许峰', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (164, 'M037', 'A028', '陆薇', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (165, 'M037', 'A014', '萧然', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (166, 'M037', 'A027', '吴川', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (167, 'M037', 'A016', '陈曦', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (168, 'M038', 'A014', '秦雨', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (169, 'M038', 'A012', '韩雪', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (170, 'M038', 'A021', '顾远', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (171, 'M039', 'A023', '夏阳', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (172, 'M039', 'A005', '阿泽', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (173, 'M039', 'A014', '秦雨', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (174, 'M039', 'A006', '林晚', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (175, 'M039', 'A024', '江北', 5);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (176, 'M039', 'A017', '罗毅', 6);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (177, 'M040', 'A020', '唐宁', 1);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (178, 'M040', 'A026', '杜若', 2);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (179, 'M040', 'A030', '何灵', 3);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (180, 'M040', 'A018', '程语', 4);
INSERT INTO "dm_cast" ("id", "movie_no", "actor_no", "role_name", "billing_order") VALUES (181, 'M040', 'A025', '陈默', 5);

CREATE TABLE "dm_director" (
    "id" bigint NOT NULL,
    "director_no" character varying(20) NOT NULL,
    "name" character varying(50) NOT NULL,
    "gender" character varying(10) NOT NULL,
    "birth_date" date NOT NULL,
    "nationality" character varying(20) NOT NULL,
    CONSTRAINT dm_director_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_director_id_seq";
ALTER SEQUENCE "dm_director_id_seq" OWNED BY "dm_director"."id";
ALTER TABLE "dm_director" ALTER COLUMN "id" SET DEFAULT nextval('dm_director_id_seq'::regclass);
SELECT setval('dm_director_id_seq', 12, TRUE);

ALTER TABLE "dm_director" ADD CONSTRAINT "dm_director_no_uk" UNIQUE (director_no);

COMMENT ON TABLE "dm_director" IS '导演(电影库-导演)';
COMMENT ON COLUMN "dm_director"."director_no" IS '导演编号';
COMMENT ON COLUMN "dm_director"."nationality" IS '国籍';

INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (1, 'D001', '郑军', '男', '1951-02-14'::date, '中国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (2, 'D002', 'Daniel Wilson', '女', '1960-01-06'::date, '美国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (3, 'D003', 'David Brown', '男', '1980-05-10'::date, '英国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (4, 'D004', 'George Williams', '女', '1976-06-27'::date, '日本');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (5, 'D005', 'Oscar Johnson', '男', '1951-05-04'::date, '法国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (6, 'D006', '张娟', '女', '1960-06-08'::date, '中国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (7, 'D007', 'Albert King', '男', '1951-03-11'::date, '美国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (8, 'D008', 'George Davis', '女', '1982-02-13'::date, '韩国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (9, 'D009', 'George Martin', '男', '1959-11-21'::date, '意大利');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (10, 'D010', '胡婧', '女', '1962-06-24'::date, '中国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (11, 'D011', 'James Miller', '男', '1981-04-26'::date, '德国');
INSERT INTO "dm_director" ("id", "director_no", "name", "gender", "birth_date", "nationality") VALUES (12, 'D012', '朱梅', '女', '1962-06-19'::date, '中国');

CREATE TABLE "dm_movie" (
    "id" bigint NOT NULL,
    "movie_no" character varying(20) NOT NULL,
    "title" character varying(100) NOT NULL,
    "category" character varying(20) NOT NULL,
    "language" character varying(20) NOT NULL,
    "country" character varying(20) NOT NULL,
    "release_year" integer NOT NULL,
    "duration" integer NOT NULL,
    "rating" numeric(3,1),
    "box_office" numeric(12,2),
    "director_no" character varying(20) NOT NULL,
    "status" character varying(20) NOT NULL,
    CONSTRAINT dm_movie_pkey PRIMARY KEY ("id")
);

CREATE SEQUENCE IF NOT EXISTS "dm_movie_id_seq";
ALTER SEQUENCE "dm_movie_id_seq" OWNED BY "dm_movie"."id";
ALTER TABLE "dm_movie" ALTER COLUMN "id" SET DEFAULT nextval('dm_movie_id_seq'::regclass);
SELECT setval('dm_movie_id_seq', 40, TRUE);

ALTER TABLE "dm_movie" ADD CONSTRAINT "dm_movie_no_uk" UNIQUE (movie_no);

COMMENT ON TABLE "dm_movie" IS '电影(电影库-电影)';
COMMENT ON COLUMN "dm_movie"."movie_no" IS '电影编号';
COMMENT ON COLUMN "dm_movie"."category" IS '类型: 剧情/喜剧/动作/科幻/爱情/犯罪/动画/悬疑/战争/奇幻';
COMMENT ON COLUMN "dm_movie"."rating" IS '评分(10分制)';
COMMENT ON COLUMN "dm_movie"."box_office" IS '票房(亿元)';
COMMENT ON COLUMN "dm_movie"."director_no" IS '导演编号(与 dm_director.director_no 对齐)';
COMMENT ON COLUMN "dm_movie"."status" IS '上映状态: 上映中/已下映/待上映';

INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (1, 'M001', '海上钢琴师', '科幻', '英语', '英国', 2022, 109, 8.1, 35.32, 'D006', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (2, 'M002', '花样年华', '剧情', '英语', '英国', 2024, 104, 7.0, 33.14, 'D006', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (3, 'M003', '花样年华', '喜剧', '法语', '法国', 2005, 158, 7.7, 4.79, 'D003', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (4, 'M004', '海上钢琴师', '动画', '英语', '英国', 2020, 117, 8.8, 14.95, 'D009', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (5, 'M005', '卧虎藏龙', '悬疑', '日语', '日本', 2009, 105, 8.9, 24.38, 'D004', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (6, 'M006', '千与千寻', '科幻', '日语', '日本', 1994, 165, 7.6, 1.65, 'D004', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (7, 'M007', '岁月无声', '战争', '国语', '中国', 1994, 120, 7.9, 47.35, 'D005', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (8, 'M008', 'Silver Line', '奇幻', '英语', '美国', 2003, 150, 9.5, 39.92, 'D004', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (9, 'M009', 'Golden Hour', '悬疑', '英语', '美国', 2016, 96, 8.7, 19.81, 'D011', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (10, 'M010', 'Midnight Run', '科幻', '英语', '美国', 1993, 114, 8.3, 40.19, 'D004', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (11, 'M011', 'The Last Signal', '悬疑', '英语', '美国', 1998, 160, 8.0, 48.16, 'D002', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (12, 'M012', '平原上的火焰', '动作', '国语', '中国', 2024, 142, 8.5, 53.31, 'D008', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (13, 'M013', 'The Quiet Storm', '爱情', '英语', '美国', 2003, 148, 8.0, 0.62, 'D005', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (14, 'M014', '无名之辈', '科幻', '国语', '中国', 2025, 97, 7.3, 8.94, 'D010', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (15, 'M015', '悬崖之上', '动作', '国语', '中国', 2010, 97, 8.6, 50.61, 'D009', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (16, 'M016', 'The Long Way Home', '动画', '英语', '美国', 2001, 105, 7.9, 32.93, 'D010', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (17, 'M017', '逆流', '科幻', '日语', '日本', 1992, 175, 6.6, 28.99, 'D012', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (18, 'M018', '微光', '悬疑', '韩语', '韩国', 2005, 130, 8.2, 22.07, 'D002', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (19, 'M019', '南方车站', '喜剧', '国语', '中国', 2019, 121, 8.2, 14.95, 'D006', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (20, 'M020', '微光', '奇幻', '法语', '法国', 2000, 173, 7.9, 45.94, 'D009', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (21, 'M021', '天长地久', '喜剧', '国语', '中国', 2025, 103, 8.0, 48.36, 'D012', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (22, 'M022', '彼岸花', '科幻', '英语', '英国', 2007, 177, 7.4, 33.46, 'D011', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (23, 'M023', '深夜食堂', '爱情', '法语', '法国', 2022, 95, 7.6, 3.27, 'D001', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (24, 'M024', '情书', '战争', '韩语', '韩国', 1998, 180, 9.5, 9.31, 'D007', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (25, 'M025', '山河故人', '犯罪', '国语', '中国', 1997, 164, 8.9, 8.62, 'D009', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (26, 'M026', '风之谷', '剧情', '英语', '英国', 2017, 135, 7.1, 2.78, 'D004', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (27, 'M027', '海上钢琴师', '动作', '日语', '日本', 1996, 120, 8.0, 22.65, 'D003', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (28, 'M028', '彼岸花', '动画', '英语', '英国', 2016, 175, 6.0, 10.28, 'D012', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (29, 'M029', '花样年华', '科幻', '日语', '日本', 2007, 115, 7.7, 48.03, 'D008', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (30, 'M030', '雨夜', '动画', '韩语', '韩国', 2009, 132, 7.2, 12.65, 'D005', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (31, 'M031', 'Double Edge', '犯罪', '英语', '美国', 2007, 93, 7.0, 53.97, 'D002', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (32, 'M032', '海上钢琴师', '动画', '法语', '法国', 2001, 134, 6.9, 2.59, 'D012', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (33, 'M033', '深夜食堂', '剧情', '韩语', '韩国', 2017, 156, 6.7, 14.38, 'D009', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (34, 'M034', '彼岸花', '奇幻', '日语', '日本', 2013, 130, 8.4, 4.31, 'D011', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (35, 'M035', 'Double Edge', '动作', '英语', '美国', 2009, 114, 7.2, 38.50, 'D007', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (36, 'M036', '风起长林', '爱情', '国语', '中国', 2001, 126, 6.9, 22.63, 'D004', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (37, 'M037', '孤城', '喜剧', '国语', '中国', 2010, 126, 6.3, 28.36, 'D009', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (38, 'M038', '尽头', '科幻', '韩语', '韩国', 1995, 108, 8.1, 37.17, 'D001', '上映中');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (39, 'M039', '流浪地球', '科幻', '国语', '中国', 2005, 179, 6.4, 25.32, 'D007', '已下映');
INSERT INTO "dm_movie" ("id", "movie_no", "title", "category", "language", "country", "release_year", "duration", "rating", "box_office", "director_no", "status") VALUES (40, 'M040', 'Beyond Horizon', '喜剧', '英语', '美国', 2015, 144, 6.6, 8.54, 'D004', '上映中');

-- 备份完成: 4 表, 0 个函数