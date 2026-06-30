# Googol Distributed Search Engine

A distributed web search engine built in Java for the Distributed Systems course
(third year, second semester) of the Informatics Engineering degree at the
University of Coimbra.

The system crawls web pages, builds a replicated inverted index across multiple
storage nodes, and answers queries through both a command line client and a web
interface. It also supports indexing Hacker News stories and generating optional
AI assisted answers through a local Ollama model.

## Features

- Distributed architecture based on Java RMI with a central coordinating gateway.
- Replicated storage nodes (barrels) holding an inverted index, so search stays
  available when individual nodes fail.
- Concurrent crawlers (downloaders) that fetch pages, extract text and links,
  and feed new URLs back into the crawl queue.
- Search by keywords (results must contain all terms) and search by citations
  (pages that link to a given URL), with pagination.
- Command line client and a Spring Boot web interface.
- Real time administration view over WebSocket showing the top searches,
  average response times, and active barrels.
- Hacker News integration to index top stories and individual user submissions.
- Optional AI answer generation using a local Ollama model
  (llama3:8b-instruct), toggleable at runtime.

## Architecture

The system is composed of independent processes that communicate over RMI.

- Gateway (`search.Gateway`): the entry point and coordinator. It holds the URL
  queue, tracks active barrels and downloaders, routes search requests to a
  barrel, and aggregates statistics.
- Barrel (`search.Barrel`): a storage node that keeps the inverted index
  (word to list of URL ids) and the URL metadata. Multiple barrels can run for
  replication and load distribution. State is persisted under `Files/`.
- Downloader (`search.Downloader`): a crawler that takes URLs from the gateway
  queue, downloads pages with jsoup, extracts content and outgoing links, and
  sends indexed data to the barrels.
- Client (`search.Client`): a command line interface for searching and for
  viewing administration statistics.
- Web server (`search.Meta2Application`): a Spring Boot application that exposes
  the search engine through a browser. It bridges to the RMI layer through
  `ClientService`, pushes live statistics through `WebSocketConfig`, and adds
  Hacker News indexing through `HackerNewsController`.

A typical deployment runs one gateway, one or more barrels, and one or more
downloaders, plus either the command line client or the web server (or both).

## Requirements

- JDK 16
- Apache Maven
- A local Ollama instance listening on `http://localhost:11434` if you want to
  use the optional AI answers (the default model is `llama3:8b-instruct-q4_0`).

## Configuration

Runtime settings live in `config.properties`:

| Key                  | Description                                              |
| -------------------- | -------------------------------------------------------- |
| `GATEWAY_IP_ADDRESS` | Address the gateway binds to and clients connect to.     |
| `MAX_SIZE`           | Maximum size, in bytes, of a downloaded page to index.   |
| `FILE_DIR`           | Directory where barrels and the gateway persist state.   |
| `QUEUE_FILE`         | File name for the persisted crawl queue.                 |
| `INDEX_FILE`         | Suffix for each barrel's indexed items file.             |
| `URL_FILE`           | Suffix for each barrel's URL list file.                  |
| `STATS_FILE`         | Suffix for the persisted statistics file.                |
| `SEARCHES_FILE`      | File name for the top searches data.                     |
| `MAX_RETRIES`        | Number of retries before giving up on a failed action.   |
| `API_KEY`            | API key placeholder for optional AI features.            |

Do not commit real secrets. Keep `API_KEY` set to a placeholder and provide the
real value through a local, untracked file or an environment variable.

## Build

```
make          # build and compile (runs mvn install and mvn compile)
make clean    # remove target/ and run mvn clean
```

## Running

Each component runs in its own terminal. The gateway must be started first.

```
make run_gateway      # start the coordinator
make run_barrel       # start a storage node (run one or more)
make run_downloader   # start a crawler (run one or more)
make run_client       # start the command line client
make run_webserver    # build and start the Spring Boot web interface
```

When the web server is running, open `http://localhost:8080` in a browser. On
the same network, other devices can reach it at `http://<server-ip>:8080`.

### Example: single machine

```
make
make run_gateway
make run_barrel
make run_downloader
make run_webserver
```

### Example: two machines

Machine 1 runs the gateway, a barrel, and a downloader. Machine 2 runs the
client (or web server), another barrel, and another downloader, pointing at the
gateway address configured in `config.properties`.

## Project structure

```
.
├── src/main/java/search/   Java sources (gateway, barrels, downloaders, client, web)
├── src/main/resources/     Spring Boot templates (Thymeleaf), CSS, and images
├── Files/                  Runtime state (index, queue, stats); not tracked in git
├── Exercises/              Course tutorials and lab exercises (see below)
├── docs/                   Project specification PDFs, report, and notes
├── config.properties       Runtime configuration
├── makefile                Build and run targets
├── pom.xml                 Maven build definition
└── README.md
```

## Exercises

The `Exercises/` directory holds the course tutorials and lab work that lead up
to the main project. Each tutorial folder contains its statement PDF together
with `java/` and `python/` reference implementations.

```
Exercises/
├── ficha3_cp/                          Concurrent programming lab (Java)
├── tutorial1-threading/                Threads and synchronization
├── tutorial2-parallel-programming/     Parallel programming
├── tutorial3-RMI-or-GRPC/              Remote invocation with RMI or gRPC
├── tutorial4-Spring-Boot-or-fastapi/   Web services with Spring Boot or FastAPI
└── tutorial5-Restful-services-integration/  Integrating RESTful services
```

These are self contained learning exercises and are independent of the main
search engine. Python virtual environments, caches, and OS metadata are ignored
through `.gitignore`; recreate a Python environment with
`python -m venv` and `pip install -r requirements.txt` where a tutorial provides one.

## Documentation

- Project specification and checklist: `docs/`
- System report and architecture diagrams: `docs/report/`
- JavaDoc: generate it with `mvn javadoc:javadoc` and open the produced
  `index.html`.

## Authors

- Miguel Castela (2022212972)
- Miguel Martins (2022213951)
