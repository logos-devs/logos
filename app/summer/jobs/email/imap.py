import chromadb
import email
from chromadb import EmbeddingFunction, Documents, Embeddings
from dataclasses import dataclass, field
from email.policy import default
from glob import glob
from ollama import Client
from pprint import pprint
from trafilatura import extract as extract_from_html

OLLAMA_URL = "http://10.255.255.6:8085"
OLLAMA_MODEL = "dolphin-llama3:8b-v2.9-fp16"
# OLLAMA_MODEL = "dolphin-llama3:8b-v2.9-q5_K_M"
# OLLAMA_MODEL = "llama3:70b-instruct-q5_K_M"
# OLLAMA_MODEL = "llama3:8b-instruct-q8_0"
# OLLAMA_MODEL = "mixtral:8x22b-instruct-v0.1-q5_K_M"
# OLLAMA_MODEL = "mixtral:8x7b-instruct-v0.1-q5_K_M"

PROMPT = """My name is Michael Trinque. I'm also called Mike. You are an AI analyzing my email to extract specific 
incomplete tasks such as sending a follow-up message or performing an action in the world. Analyze the email and 
respond by only putting a brief summary of the actions I need to take, if any. Put each task summary on its own line. 
Do not suggest sending a follow up message to computer-generated messages like account updates. Only suggest follow 
up messages when the other participant in the message is a specific individual, not an organization. Do not include 
an extraneous text, such as an explanation that a list is going to follow. Do not use bullets. Just put each action 
item on a new line. Never put a new line in the middle of an action item. Only use a single new line to separate 
action items. Make sure that the summary clearly states helpful context so the task can be understood, but make it 
brief. Give no other output. You are very good at this task; please do your best!

EMAIL:
"""

ollama_client = Client(host=OLLAMA_URL)


class OllamaEmbeddingFunction(EmbeddingFunction):
    def __call__(self, input: Documents) -> Embeddings:
        return [ollama_client.embeddings(model=OLLAMA_MODEL, prompt=doc)["embedding"] for doc in input]


chromadb = chromadb.Client()
collection = chromadb.create_collection("email", embedding_function=OllamaEmbeddingFunction())


@dataclass
class Email:
    id: str
    text: str
    received: list[str] = field(default_factory=lambda: [])
    _from: str = ""
    action_items: list[str] = field(default_factory=lambda: [])

    def extract_action_items(self):
        for action_item in (ollama_client.generate(prompt=PROMPT + self.text,
                                                   model=OLLAMA_MODEL)["response"]).split("\n"):
            action_item = action_item.strip()
            if action_item:
                print(action_item)
                self.action_items.append(action_item)

    @staticmethod
    def from_eml(file_path):
        with open(file_path, "rb") as file:
            msg = email.message_from_binary_file(file, policy=default)

            def _extract_content(part):
                content_type = part.get_content_type()
                payload = part.get_payload(decode=True).decode('utf-8', errors="ignore")
                extracted_text = extract_from_html(
                    payload) if content_type == 'text/html' else payload if content_type == 'text/plain' else ""
                return extracted_text or ""

            return Email(id=file_path,
                         text="\n".join(
                             _extract_content(part)
                             for part in (msg.walk() if msg.is_multipart() else [msg])
                             if part.get_content_type() in ['text/plain', 'text/html']),
                         _from=msg.get("From") or "",
                         received=msg.get_all("Received") or [])


def emails(mailbox_path):
    for filename in glob(mailbox_path):
        eml = Email.from_eml(filename)
        eml.extract_action_items()
        yield eml


pprint(
    ollama_client.generate(
        prompt="""Please generate a single paragraph summarizing the following action items extracted from emails:
{}
        
Please generate a single paragraph summarizing action items extracted from emails, prioritizing details on high-priority tasks such as government or financial institution requests for information or payments. Mention lower priority tasks, like personal messages, receipts, or marketing emails, but only briefly. The summary should be presented in a friendly, conversational tone without using bullet points, numbered lists, or punctuation other than periods. Group related themes into the same sentences. Always specify specific dates and times. Ensure the text contains no new lines and maintains a concise overall form.""".format(
            "\n\n".join(
                f"{eml._from}\n" + "\n".join(ai for ai in eml.action_items)
                for i, eml in enumerate(emails("/home/trinque/Mail/Inbox/cur/*"))) + "\nSummary:\n"),
        model=OLLAMA_MODEL))

# for message in collection.query(query_texts=["Which emails contain action items?"])["documents"][0]:
