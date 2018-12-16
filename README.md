# St Nick's App

An app for church members at [St Nick's Church, London](https://www.stnickschurch.org.uk/).

**Our aim:**
> Make it easy & convenient to catch up with SNC sermons and stay up-to-date with church life.

**Our motivation:**
> And he gave the apostles, the prophets, the evangelists, the shepherds and teachers, to equip the saints for the work of ministry, for building up the body of Christ,
>
> _Ephesians 4:11-12 ESV_

In God's providence, this is our prayer for the work of St Nick's & by making it easier to access the teaching, we aim for the app to support this.

## Contributing

We use the following process:

 - **issue tracking** - GitHub issues on this project
 - **contributing** - fork & pull request
 - **license** - contributed code should be released under the MIT license

## Architecture

The current architecture (not ideal) is as follows:

    app -<- REST -<- sermon metadata (JSON), hosted on Azure (managed independently from SNC website)
    app -<- HTTP -<- sermon media (MP4), hosted on AWS (shared with & managed by SNC website)
    app -<- REST -<- church calendar (Google Calendar API)
    upload webapp --- update sermon metadata, hosted on Azure

This has the advantage of not requiring a separate web server for the sermon metadata (without which the app would break), although it is less flexible as it limits the sermon metadata to be a single static object.

## Server

### Azure Storage Account

Creating a storage account suitable for uploading sermon metadata, using an Azure Storage Account for Blob storage:

 - Name: `stnicksapp`
 - Model: `Resource manager`
 - Account kind: `Blob storage`
 - Performance: `Standard`
 - Access tier: `Hot`
 - Replication: `LRS`
 - Secure transfer required: `Disabled`
 - Resource group `stnicksapp`
 - Location: `West Europe`

### Uploading sermon metadata

Install [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest), and run (with a connection string from Access Keys in the Azure console):

    cd uploader
    export AZURE_STORAGE_CONNECTION_STRING="..."
    az storage blob download -c sermons -n v1/sermons -f sermons.old.json && gzip -f sermons.old.json
    python3 prepare.py sermons.tsv sermons.old.json.gz sermons.json.gz
    az storage blob upload -c sermons -n v1/sermons -f sermons.json.gz --content-encoding gzip
