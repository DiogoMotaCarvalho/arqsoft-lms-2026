# Delivery: CI → DEV → UAT → PROD

```text
push → psoft-g1-ci ──(main only)──> DEV ──> psoft-g1-uat ──(acceptance)──> psoft-g1-prod
                       └──────────── the same JAR, selected by build number ────────────┘
```

| Jenkins job | Script Path | Started by |
|---|---|---|
| psoft-g1-ci | jenkins/Jenkinsfile.ci | push (webhook) |
| psoft-g1-uat | jenkins/Jenkinsfile.uat | psoft-g1-ci, with `CI_BUILD` |
| psoft-g1-prod | jenkins/Jenkinsfile.prod | psoft-g1-uat, with `UAT_BUILD` |

1. **CI** runs static analysis, unit tests, integration tests and the PIT gate on every branch. On `main` it packages the JAR with the commit embedded, deploys it to DEV, smoke-tests it, archives it as `validated/` and starts UAT.
2. **UAT** copies the JAR of that exact CI build, deploys it, and waits up to 7 days for a `release-managers` member to accept it. It then re-checks that UAT still runs that deployment, archives the JAR with the approver as `accepted/`, and starts PROD.
3. **PROD** copies the accepted JAR of that exact UAT build, deploys it and archives the full record.

The smoke test (`deploy/smoke-test.sh`) checks that `/actuator/info` reports the expected environment and commit, that `/actuator/health/readiness` is `UP`, that the smoke user can log in and call the API, and that the API rejects requests without a token.

There is no automatic rollback: to go back, run psoft-g1-prod again with an earlier `UAT_BUILD`.

## Server

Ubuntu (or another systemd distribution) with Java 17+ at `/usr/bin/java`, sudo, an SSH server and a `deploy` account that Jenkins can reach with a key. Copy this folder to the server and run, as root:

```sh
sh setup-environments.sh                  # private ports 8181, 8182, 8183
sh setup-environments.sh 2228 2229 2226   # or choose them
```

This creates one service per environment (`psoft-g1@dev`, `@uat`, `@prod`) with its own system user, `/opt/psoft-g1/<env>` for the JAR, `/var/lib/psoft-g1/<env>` for uploads, and `/etc/psoft-g1/<env>.env` for configuration. It allows `deploy` to start and stop only these services. Existing `.env` files are kept; edit one and restart its service to change it.

Each environment uses an in-memory H2 database seeded by the `bootstrap` profile, so data resets on every deployment.

## Jenkins

- **Plugins:** Pipeline, Git, JUnit, SSH Agent, Copy Artifact, Lockable Resources.
- **Agent:** Java 17+, `sh`, `ssh`/`scp`, `curl` and `python3`. The Maven Wrapper downloads Maven on the first build.
- **known_hosts:** add the server's host key for the agent's user (`ssh-keyscan -p PORT HOST >> ~/.ssh/known_hosts`). Do not disable host key checking.
- **Global environment variables** (Manage Jenkins → System):
  - `PSOFT_SSH_TARGET`: `deploy@HOST`
  - `PSOFT_SSH_PORT`: SSH port, default 22
  - `PSOFT_DEV_URL`, `PSOFT_UAT_URL`, `PSOFT_PROD_URL`: each environment's base URL, reachable from the agent
- **Credentials:**
  - `psoft-deploy-ssh`: SSH username with private key, user `deploy`
  - `psoft-smoke-user`: username and password of a user seeded by the bootstrap profile
- **Jobs:** three Pipeline jobs from SCM, named as in the table above. Give psoft-g1-ci the branch specifier `**` so every branch is built; only `main` deploys.
- **Approvers:** a `release-managers` user or group.
