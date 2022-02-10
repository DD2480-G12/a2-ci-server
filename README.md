# a2-ci-server

## Deploy

Build the jar file first:
```bash
mvn clean package
```

Start app:
```bash
docker-compose -f docker-compose.yml up -d
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