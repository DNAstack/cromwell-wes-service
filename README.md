Workflow Execution Schema Service
=================================

This repo contains a spring boot application which provides a fully featured [Workflow Execution Service (WES)](https://github.com/ga4gh/workflow-execution-service-schemas) api that
 can be used to present a standardized way of submitting and querying workflows. Currently the only execution engine 
 supported is `Cromwell`


# Building 

```bash
./mvnw clean install
```

# CI

```bash
./ci/build-docker-image wes-service:$(git describe) wes-service $(git describe)
./ci/build-docker-e2e-image wes-service-e2e:$(git describe) wes-service-e2e $(git describe)
```



# Building and Running Tests

```bash
docker run --network host -it wes-service-e2e:$(git describe)
```

| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `E2E_BASE_URI` | `http://localhost:8090` | The base uri for the e2e tests |
| `E2E_TOKEN_URI` | `http://localhost:8081/oauth/token` | The endpoint to get access token from wallet |
| `E2E_CLIENT_ID` | `wes-service-development-client` | WES client id for development |
| `E2E_CLIENT_SECRET` | `wes-service-development-secret` | WES client secret for development |
| `E2E_CLIENT_AUDIENCE` | `http://localhost:8090` | The audience of the access token received from wallet |
| `E2E_CLIENT_SCOPE` | `read:execution write:execution` | The scope of the access token received from wallet |
| `E2E_CLIENT_RESOURCES` | `http://localhost:8090/ga4gh/wes/v1/runs/` | The resources accessible with the access token received from wallet |

# Running

```bash
java -jar target/weservice-1.0-SNAPSHOT.jar
```

### Postgres

The service requires a postgres database. By default, it expects postgres to be running at 
`jdbc:postgresql://localhost/wes-service` with username `wes-service` with no credentials set (ie allowing localhost 
only). Postgres can be configured through env variables

| Env Variable | Default | Description |
| ------------ | ------- | ----------- |
| `SPRING_DATASOURCE_USERNAME` | `wes-service` | The username to authenticate to postgres with |
| `SPRING_DATASOURCE_PASSWORD` | `""` | The password to authenticate to postgres with |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost/wes-service` | The Postgres url to connect to |

### Basic Configuration

The WES Service has basic configuration for interacting with a local cromwell instance out of the box and provides 
support for the File object type without any subsequent configuration. In this setup, object transfer and multi-tenancy
are disabled. By default, the API is protected by [wallet](https://wallet.staging.dnastack.com) (deployed in 
staging) however any valid OIDC token issuer can be configured in place.


| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `WES_ENABLEMULTITENANTSUPPORT` | `false` | If enabled, users will only be able to see workflows they submitted. Identity is defined by the `sub` of the token |

#### Service Info

The service info served by `WES` can easily be configured through environment variables as well. Please see the
(application.yaml)[src/main/java/resources/application.yaml] or (ServiceInfo.java)[src/main/java/com/dnastack/wes/model/wes/ServiceInfo.java]
for more information


### Configuring Cromwell

The WES service can be layered on top of any cromwell API (only versions greater than 38 have been tested) to provide a 
fully featured WES API. By default, a cromwell instance on localhost is used however this can be easily configured 
through environment variables.


| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `WES_CROMWELL_URL` | `http://localhost:8000` | The cromwell instance to connect to |
| `WES_CROMWELL_USERNAME` | `null` | A username to use if cromwell requires basic auth |
| `WES_CROMWELL_PASSWORD` | `null` | A password to use if cromwell requires basic auth |


### Configuring DRS

The WES service is able to resolve `DrsUri`'s passed into it through the `workflow_params` of a new run. At the moment
this support is limited and has the following behaviour:

1. All `DrsUri`'s must be publicly resolvable unless included in the `tokens.json` for a run request
  - If the `drs://<server>` or `drs://<server>/<id>` is included in the `tokens.json` the request will use that token
2. Each `DRS` object will be recursively resolved (in the case of a Bundle) and converted to a supported url for cromwell
  - If multiple `AccessMethod` objects are defined then they will be chosen in the order defined by the `supportedAccessTypes` 
  of the `DrsConfig`
  - `AccessMethod` Objects having an `AccessId` are currently not supported
  - `AccessMethod` Objects having `headers` to access a URL are currently not supported
3. A single `Drs` object is resolved into a single object url. Additionally, if the `DrsObject` is a bundle, then that
object will resolve into an Array. Nested Bundles will result in an N-ary array.

| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `WES_DRS_SUPPORTEDACCESSTYPES` | `file` | The access types that are currently supported for Drs. The order of these dictates the order that urls will be resolved |


### Configuring Path Translations

It certain circumstances, File input URI's may need to be mapped into a separate internal represenation for cromwell to be able to
localize the files. A good example of this is with [CromwellOnAzure](https://github.com/microsoft/CromwellOnAzure), which leverages blob fuse
to mount Blob storage containers to the local file system. Outside of Cromwell/Wes users will interact with the files using the microsoft https
api, however internally they must specify files using as if they were on the local file system.  

Path Translators, allow you to define a `Regex` pattern for a prefix and a replacement string to use instead. Any number of
path translators can be provided, and they will be applied in the order they were defined, allowing you to apply multiple translators to the same string
or object.

Path translators will be applied to the input `params` prior to passing the inputs to cromwell. The original inputs will always be returned to the user. 
Path Translators will also be applied to the task logs (the stderr and stdout) as well as the outputs.

| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `WES_PATHTRANSLATIONS_[INDEX]_PREFIX` | `null` | The prefix to test inputs and outputs against |
| `WES_PATHTRANSLATIONS_[INDEX]_REPLACEMENT` | `null` | The replacement string to use instead of the prefix |
| `WES_PATHTRANSLATIONS_[INDEX]_LOCATION` | `ALL` | The location that this path translation applies to.  values [`ALL`,`INPUTS`,`OUTPUTS`] |



### Configuring the Transfer API

The transfer API provides a way to securely stage files which the workflow execution backend does not conventionally 
have access to. By default this API is disabled, but can be enabled through an environment flag.

When using the Transfer Service through `WES` several assumptions are made about the transfer

1. There is only ever a single destination directory or storage provider, ie a single bucket, container, folder where all transfers will be placed
2. The Transfer service has already been configured to access this single destination
3. The User requestng a transfer has provided all of the tokens required to perform the transfer as part of the `tokens.json` file

Incoming files to be transferred will be moved to the destintation defined by the `stagingDirectory` config variable. In general, the entirety of
the path will be preseverd in the transfer. For example if you have a staging directory of `/my-directory` and a transfer file of `gs://some-bucket/some-file.txt`
then when following transfer will be moved to `/my-directory/0abn578/some-bucket/some-file.txt`. The random string between the staging directory
and the bucket/path is a praefix added to all of the transfers. All files being transferred for the same workflow run will share the same prefix.

| Env Variable | Default | Description |
| ------------ | ------- | ----------  |
| `WES_TRANSFER_ENABLED` | `false` | Flag whether the transfer service should be enabled or not | 
| `WES_TRANSFER_STAGINGDIRECTORY` | `null` | The destination to write all transfer to. This can be a bucket, container, local folder etc |
| `WES_TRANSFER_OBJECTPREFIXWHITELIST` | `[null]` | An Array of object prefixes that will never be transferred. It is assumed in this case that the caller already has access to them |
| `WES_TRANSFER_OBJECTTRANSFERURI` | `null` | The URI of the object transfer service |
| `WES_TRANSFER_MAXMONITORINGFAILURES` | `3` | The total number of API request failures to the transfer service  tolerate before aborting the workflow |
| `WES_TRANSFER_MAXTRANSFERWAITTIMEMS` | `60_000 * 60 * 24 * 3` | The maximum wait time (ms) that any one job should be on hold while waiting for a transfer. If this waitime is exceeded, then the job will be aborted |
| `WES_AUTH_SERVICEACCOUNTAUTHENTICATIONURI` | `null` | The URI to obtain an access token from for the transfer service | 
| `WES_AUTH_SERVICEACCOUNTCLIENTID` | `null` | The client ID to use for obtaining an access token |
| `WES_AUTH_SERVICEACCOUNTSECRET` | `null` | The secret to use for obtaining an access token |



#### Triggering a transfer

The `WES` service will descend into the workflow input tree and identify any leaf values that look "file like". That is, anything
which is a valid URI (gs/https/abs/s3 etc) that could potentially be a file. All of these are wrapped in as an `ObjectWrapper` 
and extracted to be passed to the `TransferService`.  The `TransferService` will accept an array of potential objects to transfer,
as well as a map of `tokens` defined by the `tokens.json`. A transfer will ONLY be triggered if there is a valid token which
will give the transfer service permission to read the set file. A valid token is defined by the `key` being the exact same as the
full object path, OR for the same `shceme/host`.

Once all objects to transfer have been Identified, the workflown inputs are modified to reflect the final destination of the
transfers and NOT the original destination. When the workflow is submitted it will be put on "hold" until all transfer are 
completed asynchronously, and then the hold will be lifted.



# REST - API

In general, all endpoints are configured to require an access token. See the specific information for each deployment to find out
how to get an access token. 

## Service Info `GET /ga4gh/wes/v1/service-info`

Retrieve information about the service, inclduing the supported workflow versions, auth instructions, as well as some tags
defining default names of options. If the user is authenticated then the `system_state_counts` will be shown. If multi-tenant
support is turned on, only their runs will be shown here.

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
       "object-access-token-attachment-name": "tokens.json",
       "cromwell-options-attachment-name": "options.json",
       "dependencies-zip-attachment-name": "dependencies.zip"
     }
   }
```

## Create a Run `POST /ga4gh/wes/v1/runs`

This endpoint allows for the creation of a new workflow run according to the WES specification. For information on how
to configure the request, please see the [Workflow Execution Service (WES)](https://github.com/ga4gh/workflow-execution-service-schemas)
documentation. In order to fully support all of cromwells features, there are a conventions that have been followed for submitting 
workflows. For more information see the [cromwell documentation](https://cromwell.readthedocs.io/en/develop/api/RESTAPI/#definitions)

1. All workflows must be `WDL`. We do plan on supporting `CWL` in the future, but this has not been implemented
2. Workflow inputs must be a `json` file corresponding to the standard cromwell inputs json. 
3. The workflow can either be a raw WDL file, or a URL to a publicly resolvable wdl
  - the `workflow_url` must always be set, therefore if you would like to attach a `WDL` to the request it must have the 
    same name as the `workflow_url` and be attached as a `workflow_attachment`
  - If the workflow imports any other workflows, these must be included in a `workflow_attachment` called `dependencies.zip`.
    The zip folder will be passed directly to cromwell and files should be resolvable from it
4. There are two ways to specify `options` to pass on to cromwell. 
  - First, you can define KV pairs in the `workflow_engine_parameters` form property. These are just "string/string" properties, representing
    the cromwell options. The value can be "stringified" json, and only Options understood by cromwell will be extracted
  - Secondly, you can define a `workflow_attachment` file named `options.json`. This will be interpreted as cromwell options,
    and will be used over the `workflow_engine_parameters`
5. All `tags` defined in the `RunRequest` will be added to cromwell as a `label` 

When you submit the run, if it was successfully submitted you will receive an object with the `run_id` to monitor. This
`run_id` is actually the same `ID` used by cromwell   
   
```json
{ 
  "run_id": "c806516e-ea5b-4505-8d0f-70b0c7bfc48c"
}
```

## List Runs `GET /ga4gh/wes/v1/runs`

List all of the runs in a paginated list. Pagination is rundimentarily supported and it is important to note that cromwell returns
results tail first. That means the most recent entries are reported first and there is no mechanism to "freeze" the paginated list
without heavy caching as perscribed by `wes`. Therefore, entries on any given page are not static and are subject to change if
additional workflows have been submitted between listing calls.

**Request Parameters:** 

- `page_size`: Define the numbner of entries to return. If the total number of entries exceeds the `page_size` a next page token will be given to access the next page
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

Return complete information on the WES run, including input paramters, outputs, tasks, logs, execution times, etc.
Some important to note

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
      "x.s": "ggggggg"
    },
    "workflow": "WDL",
    "tags": {
      "user_id": "c806516e-ea5b-4505-8d0f-70b0c7bfc48c",
      "my": "workflow",
      "cromwell-workflow-id": "cromwell-01662c4a-094d-45ac-8376-c58a3f52fecc"
    },
    "workflow_engine_parameters": {
      "workflow_root": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/",
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
      "stdout": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-0/stdout",
      "stderr": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-0/stderr",
      "exit_code": 0
    },
    {
      "name": "x.echo3",
      "cmd": "cat /cromwell_root/genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta > Homo_sapiens_assembly38.fasta/some-out.txt\necho \"done\"",
      "start_time": "2019-12-12T19:51:14.039Z[UTC]",
      "end_time": "2019-12-12T19:56:30.694Z[UTC]",
      "stdout": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-1/stdout",
      "stderr": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-1/stderr",
      "exit_code": 0
    },
    {
      "name": "x.echo3",
      "cmd": "cat /cromwell_root/genomics-public-data/resources/broad/hg38/v0/Homo_sapiens_assembly38.fasta > Homo_sapiens_assembly38.fasta/some-out.txt\necho \"done\"",
      "start_time": "2019-12-12T19:51:14.038Z[UTC]",
      "end_time": "2019-12-12T19:56:30.694Z[UTC]",
      "stdout": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-2/stdout",
      "stderr": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo3/shard-2/stderr",
      "exit_code": 0
    },
    {
      "name": "x.echo",
      "cmd": "echo jonas",
      "start_time": "2019-12-12T19:51:11.997Z[UTC]",
      "end_time": "2019-12-12T19:54:49.692Z[UTC]",
      "stdout": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo/stdout",
      "stderr": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo/stderr",
      "exit_code": 0
    },
    {
      "name": "x.echo2",
      "cmd": "echo charlie",
      "start_time": "2019-12-12T19:51:11.997Z[UTC]",
      "end_time": "2019-12-12T19:54:16.691Z[UTC]",
      "stdout": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo2/stdout",
      "stderr": "gs://dnastack-cromwell-development-bucket/x/01662c4a-094d-45ac-8376-c58a3f52fecc/call-echo2/stderr",
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