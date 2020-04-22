-- postgres sql database structure for the DBUserSystemService implementation.

CREATE TABLE public.ta_user (
  userid character varying(255) NOT NULL,
  enabled smallint NOT NULL,
  descritpion VARCHAR(255),

  CONSTRAINT ta_user_pkey PRIMARY KEY (userid)
);

CREATE TABLE public.ta_user_group (
  userid character varying(255) NOT NULL,
  groupid character varying(255)
);

CREATE TABLE public.ta_user_skill (
  userid character varying(255) NOT NULL,
  skillid character varying(255)
);

ALTER TABLE public.ta_user
  OWNER TO jbpm;

ALTER TABLE public.ta_user_group
  OWNER TO jbpm;

ALTER TABLE public.ta_user_skill
  OWNER TO jbpm;