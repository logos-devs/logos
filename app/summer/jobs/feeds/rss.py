import json
import uuid
from _socket import gethostbyname_ex
from datetime import date
from json import JSONDecodeError
from os import environ
from pprint import pprint
from sys import stdout

import boto3
import botocore.session
import feedparser
import openai
import psycopg
from psycopg import cursor, connection
from psycopg.rows import namedtuple_row
from trafilatura import extract, fetch_url


def get_secret(secret_name):
    return json.loads(
        boto3.client('secretsmanager').get_secret_value(
            SecretId="arn:aws:secretsmanager:{}:{}:secret:{}".format(
                botocore.session.get_session().get_config_variable('region'),
                boto3.client('sts').get_caller_identity()['Account'],
                secret_name
            )
        )['SecretString'])


openai.api_key = get_secret('dev/external/openai')['key']

DB_NAME = "logos"
DB_URL = (environ.get("STORAGE_PG_BACKEND_JDBC_URL") or "jdbc:postgresql://localhost:15432/logos")[5:]
DB_USER = environ.get("STORAGE_PG_BACKEND_USER") or "storage"
DB_HOST = (environ.get("STORAGE_PG_BACKEND_HOST") or
           next(h for h in gethostbyname_ex('db-rw.logos.dev')[1]
                if h.endswith('us-east-2.rds.amazonaws.com')))
DB_PORT = 5432


SUMMARY_PROMPT = """You are a news summarization system which consumes an article and produces a valid JSON object with the following fields:
* name : A concise and explanatory title. If the original article's title is a joke or otherwise obscure, replace it with a simple, informative title.
* body : The shortest possible summary that includes the important factual claims of the article. Aim for two sentences or fewer.
* slug : A concise and explanatory URL identifier for the article consisting only of lowercase letters, numbers, and dashes.
* tags : An array of descriptive single-word tags consisting only of lowercase letters and dashes for the article listed in order from least to most specific. Include all named entities in the article."""


AGGREGATION_PROMPT = "Take the following abstract of a news article and use it to revise a running summary of the topic. Return only the updated summary which incorporates any new facts discussed in the article. Make sure to edit the summary to deduplicate facts. Make the running summary as concise and well-written as possible. Your output should resemble an encyclopedia entry on the topic, drawing factual information from each article I provide. Do not editorialize. Merely brief the user on all known facts on the topic in a readable, comprehensible manner."

def connect_db() -> connection:
    rds = boto3.client('rds')
    token = rds.generate_db_auth_token(DB_HOST, DB_PORT, DB_USER)
    return psycopg.connect(
        DB_URL,
        user=DB_USER,
        password=token,
        row_factory=namedtuple_row,
        autocommit=True
    )


def query_feeds(c: cursor) -> cursor:
    return c.execute("select id, name, url, image_url, favicon_url, synced_at "
                     "from summer.source_rss")


def summary_exists(c: psycopg.cursor, article_link: str) -> cursor:
    return c.execute("select exists(select from summer.entry where link_url = %s)",
                     (article_link,)).fetchone()[0]


def create_entry(c: cursor, summary: dict) -> cursor:
    c.execute("insert into summer.entry (name, body, link_url, tags, published_at, source_rss_id) "
              "values (%(name)s, %(body)s, %(link_url)s, %(tags)s, %(published_at)s, %(source_rss_id)s)",
              summary)


def get_topics(c: cursor) -> cursor:
    return c.execute("select id, name, summary, tags from summer.topic")


def get_unconsumed_entries_for_topic(c: cursor, topic_id: uuid) -> cursor:
    return c.execute("""
        select e.*
        
        from (select id, name, body, lower(unnest(tags)) as tag, created_at from summer.entry) e,
             (select id, lower(unnest(tags)) as tag, name, summary from summer.topic) t
        
        where e.tag = t.tag
          and not exists(
            select
            from summer.topic_entry
            where entry_id = e.id
              and topic_id = %s)
              
        order by e.created_at
    """, (topic_id,))


def summarize(c: cursor, source_rss_id, article_link: str, article_text: str, published_at: str):
    model = "gpt-3.5-turbo"
    last_shot = False
    for j in range(0, 10):
        try:
            response = openai.ChatCompletion.create(
                model=model,
                messages=[
                    {"role": "system", "content": SUMMARY_PROMPT},
                    {"role": "user", "content": article_text}
                ],  # temperature=0,
                # max_tokens=120,
            )
        except openai.error.InvalidRequestError:
            if last_shot:
                return

            model = "gpt-3.5-turbo-16k"
            last_shot = True
            continue

        try:
            summary = json.loads(response["choices"][0]["message"]["content"])
            summary["link_url"] = article_link
            summary["published_at"] = published_at
            summary["source_rss_id"] = source_rss_id
            pprint(summary)
        except JSONDecodeError:
            if last_shot:
                return
            continue

        create_entry(c, summary)
        print(article_link)
        break


def aggregate(topic_summary: str, article_date: date, article_text: str):
    model = "gpt-4-1106-preview"
    response = openai.ChatCompletion.create(
        model=model,
        messages=[
            {"role": "system", "content": AGGREGATION_PROMPT},
            {"role": "system", "name": "Topic-Summary", "content": topic_summary},
            {"role": "system", "name": "New-Article", "content": "{}\n{}".format(article_date, article_text)},
        ],
    )
    return response['choices'][0]["message"]["content"]


def main():
    with connect_db() as db:
        with db.cursor() as c:
            for topic in list(get_topics(c)):
                topic_summary = topic.summary or ""
                for entry in list(get_unconsumed_entries_for_topic(c, topic.id)):
                    topic_summary = aggregate(topic_summary, entry.created_at.date(), entry.body)

                print(topic_summary)
                return
            for feed in list(query_feeds(c)):
                for entry in feedparser.parse(feed.url).entries:
                    if summary_exists(c, entry.link): continue
                    pprint(entry)
                    stdout.flush()
                    article_text = extract(fetch_url(entry.link))
                    published_at = entry.published if 'published' in entry else entry.updated
                    summarize(c, feed.id, entry.link, article_text, published_at)


if __name__ == '__main__':
    main()