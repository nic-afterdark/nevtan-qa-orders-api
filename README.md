# orders-api

A fake order service. It exists to produce log lines, not to do anything useful.
Deploy it, then open its Logs page and work through the table below.

- Stack: Spring Boot 3.3.4, Java 17, Maven
- Port: `PORT`, default `8080`
- Health: `GET /health`
- Index of every test route: `GET /qa/`

Once it is running it writes traffic on its own: about one line every 2 seconds
(70% info, 20% debug, 8% warn, 2% error), a heartbeat every minute, and a burst
of 20 errors every 5 minutes. So the Logs page is never empty, and a chart drawn
from these lines has a repeating error spike to show.

## Branches

| Branch | What it does |
| --- | --- |
| `main` | Builds and runs. Use it for runtime log testing. |
| `broken` | Fails during `mvn package` with two compile errors. Use it for build log testing. |

## Test cases

Every line is tagged with its case id, so a line on screen can be traced back to
the code that wrote it. Fire a case with its route, then look at the Logs page.

| Id | Route | What it emits | What a correct viewer does |
| --- | --- | --- | --- |
| QA-01 | `/qa/levels` | one line at each of debug, info, warn, error | all four appear; each level filter shows exactly its own line |
| QA-02 | `/qa/stacktrace` | `IllegalStateException` caused by a `SQLException`, ~20 lines | the whole trace stays together as one entry, all of it marked error, `Caused by:` included |
| QA-03 | `/qa/longline` | one line of about 7 KB | wrapped or truncated with a way to see the rest; it must not break the layout |
| QA-04 | `/qa/unicode` | devanagari, CJK, arabic and emoji | renders as text, not as `?` or mojibake |
| QA-05 | `/qa/ansi` | ANSI colour escape codes | either rendered as colour or stripped; raw `[31m` on screen is a bug |
| QA-06 | `/qa/traps` | info and debug lines containing the words ERROR, WARN, FATAL | only the genuine error line is red. Anything else red means the level is being guessed from the message text |
| QA-07 | `/qa/burst?n=500` | n numbered lines as fast as possible | all n arrive, in order, with no gaps in the sequence numbers |
| QA-08 | `/qa/json` | one line of nested JSON | shown intact; pretty printing is a bonus |
| QA-09 | `/qa/whitespace` | blank lines, spaces-only lines, tabs | blank lines either kept or dropped consistently; tabs do not collapse the columns |
| QA-10 | `/qa/block` | a 7 line ASCII table in a single log call | stays as one block, alignment intact |
| QA-11 | `/qa/backdated` | a line whose text carries a timestamp 2 hours old | sits at the bottom, at now. If it jumps 2 hours back, the viewer is parsing the message for a time |
| QA-12 | `/qa/secrets` | a bearer token and a postgres URL with a password | note whether anything is redacted anywhere, including in the download and copy actions |
| QA-13 | `/qa/streams` | raw writes to stdout and stderr, bypassing the logger | both captured. Check whether stderr is force-labelled error, which would be wrong here |
| QA-14 | `/qa/500` | an exception thrown out of a controller, logged by Spring itself | the framework's own error line and trace appear, not only ours |
| QA-15 | `/qa/slow?ms=30000` | silence for 30s, then one line | live tail survives the gap and shows the line without a refresh |
| QA-16 | `/qa/oom` | allocates 8 MB at a time until the JVM or the container dies | the OutOfMemoryError or the kill is visible in the logs, not just a dead container |
| QA-17 | `/qa/crash` | `System.exit(1)` | logs from before the crash survive the restart |
| all | `/qa/all` | QA-01 to QA-13 in order | a single pass over most of the table |

`/qa/burst?n=20000` and `/qa/slow?ms=120000` are the upper limits; larger values
are clamped.

## Running it locally

```bash
mvn package
PORT=8098 java -jar target/orders-api-1.0.0.jar
curl localhost:8098/qa/all
```
