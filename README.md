Workflow Execution Schema Service
=================================

This repo contains a spring boot application which provides a fully
featured [Workflow Execution Service (WES)](https://github.com/ga4gh/workflow-execution-service-schemas) api that can be
used to present a standardized way of submitting and querying workflows. Currently, the only execution engine supported
is [Cromwell](https://github.com/Broadinstitute/cromwell). Other then cromwell, there are no hard external dependencies
(ie a database).

# Requirements

- Java 17+
- [Cromwell](https://github.com/Broadinstitute/cromwell) running somewhere that is accessible to the WES service

# Building

```bash
./mvnw clean install
```

# Running

By default, the WES service will start on port `8090`. You can change this by specifying the `SERVER_PORT` environment
variable.

```bash
java -jar target/cromwell-wes-service-1.0-SNAPSHOT.jar
```

## Basic Configuration

The WES Service has basic configuration for interacting with a local cromwell instance out of the box and provides
support for the File object type without any subsequent configuration.

By default, the API is protected by [DNAstack Passport](https://passport.dnastack.com), however you can configure

## Authentication

By default, the API is setup to use a local installation of [DNAstack Passport](https://passport.dnastack.com) as a
token
issuer, however any valid OAuth OAuth 2.0 Token issuer can be used. There is also a mechanism to run

### Passport Configuration

Many Configuration fields are automatically filled when using passport as the authentication mechanism. The only
required
property is the `WES_AUTH_ISSUER_URI` which can be set as defined below

| Env Variable          | Default                 | Description                              |
|-----------------------|-------------------------|------------------------------------------|
| `WES_AUTH_ISSUER_URI` | `http://localhost:8081` | The URI of a local passport installation |

### External OAuth 2.0 token Issuer

If using an external token issue that is not Passport, you will need to provide additional configuration beyond
the `WES_AUTH_ISSUER_URI`.
Additionally, you will also need to disable passport permission authorization enforcement. This can be accomplished by
disabling the authorization:

`SECURIT_AUTHORIZATION_ENABLED=false`

Additional configuration values available:

| Env Variable                           | Default                                          | Description                                                                                                                                                                                            |
|----------------------------------------|--------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WES_AUTH_TOKEN_ISSUER_ISSUER_URI`     | `http://localhost:8081`                          | The Issuer URI, corresponding to the `iss` field in the JWT                                                                                                                                            |
| `WES_AUTH_TOKEN_ISSUER_AUDIENCES`      | `[${WES_URL}]`                                   | A list of acceptable audiences to enforce corresponding to the `aud` field in the JWD                                                                                                                  |
| `WES_AUTH_TOKEN_ISSUER_SCOPES`         | `[wes]`                                          | A list of scopes to enforce are present in the token, corresponding to the `scope` field in the JWT                                                                                                    |
| `WES_AUTH_TOKEN_ISSUER_JWK_SET_URI`    | `${WES_AUTH_TOKEN_ISSUER_ISSUER_URI}/oauth/jwks` | The URI to fetch the [Json Web Key Set](https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-key-sets) to validate the signature of tokens. Either the JWKS or the RSA Public key is required |
| `WES_AUTH_TOKEN_ISSUER_RSA_PUBLIC_KEY` | `null`                                           | The public key to use to verify the signature of the JWT's. Eiuther the RSA Public Key or JKWS is required                                                                                             |

### No Auth

You can start the WES Service in a mode that does not require any authentication or authorization. This may be useful
if you are using the WES service locally for testing, do not have access to a OAuth 2.0 provider, or want to run the
service behind a reverse proxy which provides authentication

_note_ All endpoints will be accessible without any authentication at all

You can enable the `no-auth` spring profile to turn off all authentication

`SPRING_PROFILES_ACTIVE=no-auth`

### mTLS Authorization

One possible alternative to using OAuth 2.0 is to setup a reverse proxy that
uses [Mutual TLS](https://en.wikipedia.org/wiki/Mutual_authentication).
The benefit to mTLS is that it wraps the authentication of both the server and the client directly in the transport
layer of the
request.

mTLS does not prevent you from also using OAuth 2.0 or any other form of authentication in addition to mTLS, howver it
is not
necessary. You can start the WES Service in the [no-auth](#no-auth) setup

You can see an example nginx configuration using mTLS in front of the WES service [here](nginx/README.md)

## Service Info

The service info served by `WES` can easily be configured through environment variables as well. Please see the
(application.yaml)[src/main/java/resources/application.yaml] or (
ServiceInfo.java)[src/main/java/com/dnastack/wes/model/wes/ServiceInfo.java]
for more information.

## Configuring Cromwell

The WES service can be layered on top of any cromwell API (only versions greater than 38 have been tested) to provide a
fully featured WES API. By default, a cromwell instance on localhost running on port 8000 is used however this can be
easily configured through environment variables.

| Env Variable            | Default                 | Description                                       |
|-------------------------|-------------------------|---------------------------------------------------|
| `WES_CROMWELL_URL`      | `http://localhost:8000` | The cromwell instance to connect to               |
| `WES_CROMWELL_USERNAME` | `null`                  | A username to use if cromwell requires basic auth |
| `WES_CROMWELL_PASSWORD` | `null`                  | A password to use if cromwell requires basic auth |

## Configuring Path Translations

It certain circumstances, File input URI's may need to be mapped into a separate internal representation for cromwell to
be able to localize the files. A good example of this is
with [CromwellOnAzure](https://github.com/microsoft/CromwellOnAzure), which leverages blob fuse to mount Blob storage
containers to the local file system. Outside of Cromwell/Wes users will interact with the files using the microsoft blob
storage api, however internally they must specify files as if they were on the local file system.

Path Translators, allow you to define a `Regex` pattern for a prefix and a replacement string to use instead. Any number
of path translators can be provided, and they will be applied in the order they were defined, allowing you to apply
multiple translators to the same string or object.

Path translators will be applied to the input `params` prior to passing the inputs to cromwell. The original inputs will
always be returned to the user. Path Translators will also be applied to the task logs (the stderr and stdout) as well
as the outputs.

| Env Variable                               | Default | Description                                                                           |
|--------------------------------------------|---------|---------------------------------------------------------------------------------------|
| `WES_PATHTRANSLATIONS_[INDEX]_PREFIX`      | `null`  | The prefix to test inputs and outputs against                                         |
| `WES_PATHTRANSLATIONS_[INDEX]_REPLACEMENT` | `null`  | The replacement string to use instead of the prefix                                   |
| `WES_PATHTRANSLATIONS_[INDEX]_LOCATION`    | `ALL`   | The location that this path translation applies to. values [`ALL`,`INPUTS`,`OUTPUTS`] |

# Storage

The WES service has been designed to run on Cromwell in most of the environments that cromwell currently supports. In
order
to access the log data and write `workflow_attachments` to storage, it may be necessary to provide credentials for file
system access.

By Default, the service will use the local file-system accessible to WES. There is an assumption that Cromwell has
access
to the same file system. Only one file system is supported at a given time

## Local File System

You can enable the local file system by specifying `LOCAL` as the storage client name

`WES_BLOB_STORAGE_CLIENT_NAME=LOCAL`.

| Env Variable                                 | Default    | Description                                                      |
|----------------------------------------------|------------|------------------------------------------------------------------|
| `WES_BLOB_STORAGE_CLIENT_LOCAL_STAGING_PATH` | `uploads/` | The path accessible to cromwell to write workflow attachments to |

# CI

Building containers requires an installation of docker

```bash
./ci/build-docker-image cromwell-wes-service:$(git describe) cromwell-wes-service $(git describe)
./ci/build-docker-e2e-image cromwell-wes-service-e2e:$(git describe) cromwell-wes-service-e2e $(git describe)
```

# Building and Running Tests

```bash
docker run --network host -it cromwell-wes-service-e2e:$(git describe)
```

| Env Variable           | Default                                    | Description                                                         |
|------------------------|--------------------------------------------|---------------------------------------------------------------------|
| `E2E_BASE_URI`         | `http://localhost:8090`                    | The base uri for the e2e tests                                      |
| `E2E_TOKEN_URI`        | `http://localhost:8081/oauth/token`        | The endpoint to get access token from wallet                        |
| `E2E_CLIENT_ID`        | `wes-service-development-client`           | WES client id for development                                       |
| `E2E_CLIENT_SECRET`    | `wes-service-development-secret`           | WES client secret for development                                   |
| `E2E_CLIENT_AUDIENCE`  | `http://localhost:8090`                    | The audience of the access token received from wallet               |
| `E2E_CLIENT_SCOPE`     | `read:execution write:execution`           | The scope of the access token received from wallet                  |
| `E2E_CLIENT_RESOURCES` | `http://localhost:8090/ga4gh/wes/v1/runs/` | The resources accessible with the access token received from wallet |

# REST - API

In general, all endpoints are configured to require an access token. See the specific information for each deployment to
find out how to get an access token.

## Service Info `GET /ga4gh/wes/v1/service-info`

Retrieve information about the service, including the supported workflow versions, auth instructions, as well as some
tags defining default names of options. If the user is authenticated then the `system_state_counts` will be shown. If
multi-tenant support is turned on, only their runs will be shown here.

```json
{
    "workflow_type_versions": {
        "WDL": [
            "draft-2",
            "1.0"
        ]
    },
    "supported_wes_versions": [
        "1.0.0"
    ],
    "supported_filesystem_protocols": [
        "gs",
        "http",
        "file",
        "drs"
    ],
    "workflow_engine_versions": {
        "cromwell": "42"
    },
    "system_state_counts": {
        "EXECUTOR_ERROR": 7,
        "CANCELED": 5,
        "COMPLETE": 11
    },
    "auth_instruction_url": "https://wep-keycloak.staging.dnastack.com/auth/realms/DNAstack/.well-known/openid-configuration",
    "tags": {
        "cromwell-options-attachment-name": "options.json",
        "dependencies-zip-attachment-name": "dependencies.zip"
    }
}
```

## Create a Run `POST /ga4gh/wes/v1/runs`

This endpoint allows for the creation of a new workflow run according to the WES specification. For information on how
to configure the request, please see
the [Workflow Execution Service (WES)](https://github.com/ga4gh/workflow-execution-service-schemas)
documentation. In order to fully support all of cromwells features, there are a conventions that have been followed for
submitting workflows. For more information see
the [cromwell documentation](https://cromwell.readthedocs.io/en/develop/api/RESTAPI/#definitions)

1. All workflows must be `WDL`. We do plan on supporting `CWL` in the future, but this has not been implemented
2. Workflow inputs must be a `json` file corresponding to the standard cromwell inputs json.
3. The workflow can either be a raw WDL file, or a URL to a publicly resolvable wdl

- the `workflow_url` must always be set, therefore if you would like to attach a `WDL` to the request it must have the
  same name as the `workflow_url` and be attached as a `workflow_attachment`
- If the workflow imports any other workflows, these must be included in a `workflow_attachment`
  called `dependencies.zip`. The zip folder will be passed directly to cromwell and files should be resolvable from it

4. There are two ways to specify `options` to pass on to cromwell.

- First, you can define KV pairs in the `workflow_engine_parameters` form property. These are just "string/string"
  properties, representing the cromwell options. The value can be "stringified" json, and only Options understood by
  cromwell will be extracted
- Secondly, you can define a `workflow_attachment` file named `options.json`. This will be interpreted as cromwell
  options, and will be used over the `workflow_engine_parameters`

5. All `tags` defined in the `RunRequest` will be added to cromwell as a `label`

When you submit the run, if it was successfully submitted you will receive an object with the `run_id` to monitor. This
`run_id` is actually the same `ID` used by cromwell

```json
{
    "run_id": "c806516e-ea5b-4505-8d0f-70b0c7bfc48c"
}
```

## List Runs `GET /ga4gh/wes/v1/runs`

Return runs in a paginated list. Pagination has rudimentary supported and it is important to note that
cromwell returns results tail first. That means the most recent entries are reported first and there is no mechanism
to "freeze" the paginated list without heavy caching as perscribed by `WES`. Therefore, entries on any given page are
not static and are subject to change if additional workflows have been submitted between listing calls.

**Request Parameters:**

- `page_size`: Define the numbner of entries to return. If the total number of entries exceeds the `page_size` a next
  page token will be given to access the next page
- `page_token`: Access the next page relative to the previous request. This is mutually exclusive with `page_size`

```json
{
    "runs": [
        {
            "run_id": "5d435f79-7c7b-41fe-9ed0-c333ae32e4de",
            "state": "EXECUTOR_ERROR"
        },
        {
            "run_id": "bb9e719f-30d8-4895-98a9-72ba689c0c83",
            "state": "EXECUTOR_ERROR"
        },
        {
            "run_id": "cf48955b-0707-413b-8696-fcea304ad461",
            "state": "EXECUTOR_ERROR"
        },
        {
            "run_id": "a355ae0f-766d-4158-a41a-3679ad1142e8",
            "state": "EXECUTOR_ERROR"
        },
        {
            "run_id": "81d59e64-0c6c-4b9c-b682-5826763d7d85",
            "state": "EXECUTOR_ERROR"
        }
    ],
    "next_page_token": "cGFnZSUzRDIlMjZwYWdlU2l6ZSUzRDU="
}
```

## Run Summary `GET /ga4gh/wes/v1/runs/{id}`

Return complete information on the WES run, including input paramters, outputs, tasks, logs, execution times, etc. Some
important to note

1. All labels on cromwell are displayed as `tags` in the `RunSummary`
2. Workflow inputs are displayed AS SUBMITTED. This means that if there were any transformations to the input objects
   required for transfer or resolving of `drs`, the original `workflow_params` will be returned
3. Cromwell options are displayed as `workflow_engine_params`
4. the outputs are returned in the same format as cromwell would return

```json
{
    "run_id": "01662c4a-094d-45ac-8376-c58a3f52fecc",
    "request": {
        "workflow_params": {
            "x.p": {
                "age": 42,
                "info": "drs://localhost:8090/4",
                "name": "jonas",
                "children": [
                    {
                        "age": 7,
                        "name": "charlie"
                    }
                ]
            },
            "x.s": "he,llo"
        },
        "workflow": "WDL",
        "tags": {
            "user_id": "c806516e-ea5b-4505-8d0f-70b0c7bfc48c",
            "my": "workflow",
            "cromwell-workflow-id": "cromwell-01662c4a-094d-45ac-8376-c58a3f52fecc"
        },
        "workflow_engine_parameters": {
            "workflow_root": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/",
            "write_to_cache": "false"
        },
        "workflow_url": "echo.wdl"
    },
    "state": "COMPLETE",
    "run_log": {
        "name": "x",
        "start_time": "2019-12-12T19:51:09.511Z[UTC]",
        "end_time": "2019-12-12T19:56:33.302Z[UTC]"
    },
    "task_logs": [
        {
            "name": "x.echo3",
            "cmd": "cat /cromwell_root/genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta > Homo_sapiens_assembly38.fasta/some-out.txt\necho \"done\"",
            "start_time": "2019-12-12T19:51:14.038Z[UTC]",
            "end_time": "2019-12-12T19:56:30.693Z[UTC]",
            "stdout": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-0/stdout",
            "stderr": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-0/stderr",
            "exit_code": 0
        },
        {
            "name": "x.echo3",
            "cmd": "cat /cromwell_root/genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta > Homo_sapiens_assembly38.fasta/some-out.txt\necho \"done\"",
            "start_time": "2019-12-12T19:51:14.039Z[UTC]",
            "end_time": "2019-12-12T19:56:30.694Z[UTC]",
            "stdout": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-1/stdout",
            "stderr": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-1/stderr",
            "exit_code": 0
        },
        {
            "name": "x.echo3",
            "cmd": "cat /cromwell_root/genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta > Homo_sapiens_assembly38.fasta/some-out.txt\necho \"done\"",
            "start_time": "2019-12-12T19:51:14.038Z[UTC]",
            "end_time": "2019-12-12T19:56:30.694Z[UTC]",
            "stdout": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-2/stdout",
            "stderr": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-2/stderr",
            "exit_code": 0
        },
        {
            "name": "x.echo",
            "cmd": "echo jonas",
            "start_time": "2019-12-12T19:51:11.997Z[UTC]",
            "end_time": "2019-12-12T19:54:49.692Z[UTC]",
            "stdout": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo/stdout",
            "stderr": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo/stderr",
            "exit_code": 0
        },
        {
            "name": "x.echo2",
            "cmd": "echo charlie",
            "start_time": "2019-12-12T19:51:11.997Z[UTC]",
            "end_time": "2019-12-12T19:54:16.691Z[UTC]",
            "stdout": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo2/stdout",
            "stderr": "/file-system/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo2/stderr",
            "exit_code": 0
        }
    ],
    "outputs": {
        "x.d2": "charlie",
        "x.d3": [
            "done",
            "done",
            "done"
        ],
        "x.d": "jonas"
    }
}
```

## Run Status `GET /ga4gh/wes/v1/runs/{id}/status`

Return an updated status for a specific run

```json
{
    "run_id": "c806516e-ea5b-4505-8d0f-70b0c7bfc48c",
    "state": "RUNNING"
}
```

## Cancel Run `POST /ga4gh/wes/v1/runs/{id}/status`

Cancel a single run