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
HOST_REPO_DIR=/home/ncj19991213/ci-repos        # store cloned repo
HOST_PEM_LOCATION=/home/ncj19991213/webapp.pem  # your GitHub app credential
HOST_DATA_DIR=/opt/ci-server-data               # database & other data
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
