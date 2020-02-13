create table sections(
	   id integer primary key,
	   name text not null,
	   parent_id integer,
	   user text not null
);

create table sentences(
	   id integer primary key,
	   value text not null,
	   user text not null
);

create table section_sentences(
	   section_id integer,
	   sentence_id integer,
	   primary key(section_id, sentence_id)
);
