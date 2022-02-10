# a2-ci-server

This project is a tiny ci server for compiling and testing [Maven](https://maven.apache.org/) projects on GitHub.
The server receives webhook push events from GitHub. The application then compile and test the Maven project.
The build log is saved in local database. Commit status and url of build detail will be sent back to GitHub.

## Links

* Project Repo: https://github.com/DD2480-G12/a2-ci-server
* Frontend Repo: https://github.com/DD2480-G12/a2-frontend
* Build History: http://nichujie.xyz/

## Requirement

* JDK 17 or above
* Apache Maven 3.3+
* Docker & docker-compose (for deployment)

## Framework

* Spring Boot: Web server
* SQLite: Data Storage & Persistence
* Docker: Fast Deployment & Runtime Isolation

## Database Schema

Persistent storage of build data was implemented using a SQLite database file with the following schema:

```sql
CREATE TABLE builds (
    uid INTEGER PRIMARY KEY,
    commit_hash TEXT,
    content TEXT,
    timestamp TEXT
);
```

## Configuration

### GitHub App Authentication
We integrate functionalities of GitHub Apps to communicate with GitHub. Please refer to 
[this page](https://docs.github.com/en/developers/apps/building-github-apps/authenticating-with-github-apps) on how to create an app and generate a private key.

**Before running any commands**, you need to place the private key under **the parent directory of
the project directory**, and name it as `webapp.pem`. If you prefer to put it elsewhere, you can override the default settings
by exporting an environment variable:
```yaml
export PEM_LOCATION=/my/custom/directory/webapp.pem
```

### Repository Storage
We clone and save the repositories of push events on local file system.
By default, they are saved in a directory called `repos` under **the parent directory of
the project directory**. You can override this path using:
```yaml
export REPO_DIR=/my/custom/repos
```


## Development

Start the application:

```bash
mvn spring-boot:run
```

Build the package:

```bash
mvn clean package
```

Test:

```bash
mvn test
```

## Deploy

Clone the repo and build the jar file first:

```bash
mvn clean package
```

Start app and expose port `8080`:

```bash
docker-compose -f docker-compose.yml up -d
```

Configure data directories in `.env` file:

```yaml
HOST_REPO_DIR=/home/ncj19991213/ci-repos        # store cloned repo on host
HOST_PEM_LOCATION=/home/ncj19991213/webapp.pem  # your GitHub app credential on host
HOST_DATA_DIR=/opt/ci-server-data               # database & other data on host
```

## CI result notification

The notification of the result of the CI test has been implemented by setting a status on the last
commit in the supplied pull request. We have created a GitHub App which is connected to our repository
and has permissions to create statuses. The CI server authenticates against this app and performs a
POST request to the GitHub API to set statues. In order to authenticate, a private key file must exist
locally.

The unit tests have been implemented by sending a POST request to GitHub and observing the response.
One test case send valid API data and expects a valid response, another sends invalid API data and
expects an invalid response.

## API Documentation

### Handle push events

`POST /push-events`

#### Description

Accepts push events from GitHub webhooks

#### Parameters

None

Request body implemented according to [this](https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#push), the CI server only supports the properties in the example below.

Request body example:

```
{
    "ref":"refs/heads/mustafa/test-branch",
    "after":"ea312d0e44d08a603935b8b926eea76e4ed0e266",
    "repository": {
        "name":"a2-ci-test",
        "clone_url":"https://github.com/DD2480-G12/a2-ci-test.git",
        "owner": {
            "name":"DD2480-G12"
        }
    }
}
```

#### Responses

204 No Content: If the request is accepted by the server

### Build history

`GET /history`

#### Description

Returns the whole stored history of builds

#### Parameters

None

#### Responses

200 OK: If the request is successful

### Response body

Returns an array of BuildInfo, if the history is empty, an empty array will be returned.

BuildInfo

```
{    uid: Integer,    commitId: String,    content: String,    timestamp: String}
```

### Specific build

`GET /history/{id}`

#### Description

Returns the information for a specific build

#### Parameters

`id`: Integer, the uid of the desired build

#### Responses

400 Bad Request: If `id` is not a number\
404 Not Found: If build `id` does not exist\
200 OK: If a build log with the given `id` exists


### Response body

Returns a BuildInfo

BuildInfo

```
{    uid: Integer,    commitId: String,    content: String,    timestamp: String}
```


## SEMAT

#### Current state: between Formed and Collaborating

We are working on forming a team that works as a cohesive unit.
We don’t think we are all the way there yet, as we could coordinate more
and discuss more what issues we should work on next and how we could help
each other. We know each other sufficiently to work together, even though
we of course could know each other much better if we had time for team
building. The team mission is quite clear from the assignment, and it’s
clear that the team is focused on achieving it.

## Statement of Contribution

### Chujie Ni

* Server Setup
* Docker Deployment & Error Fixing
* Integration Test of Webhooks on GitHub
* Add target url to commit status

### Gustav Ekner

* Implemented the Github client (notifications)

### Mustafa Dinler

* Implemented endpoint for push events from GitHub webhook
* Implemented Compile and Test stages
* Wrote implementation details and the way Compile and Test was unit tested
* Actively reviewed Pull Requests

### Mikael Jafari

* Partially implemented and researched sqlite database and spring
* Setup a basic front-end that connects and displays data from our back-end

### Jakob Kratz

* Build history endpoint
* Build info endpoint
* Made database wrapper