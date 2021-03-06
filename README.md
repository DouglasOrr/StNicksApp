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


## Updating

### Update sermon metadata

Visit the [sermon archive](https://www.stnickschurch.org.uk/sermon-archive/) & update `sermons.tsv`.

    # Add the following into a file uploader/.credentials:
    #    export AZURE_STORAGE_CONNECTION_STRING="..."
    cd uploader
    ./upload.sh

### Update APK

 - Prepare
   - Update `versionCode` and `versionName` in build.gradle
   - Run `AndroidTests` & `JUnitTests`
   - Commit & push
   - _Android Studio > Build > Generate signed bundle / APK > Android App Bundle_
   - `ls -lh android/app/release/app-release.aab`
 - Release
   - `export SNCVERSION=$(date --iso-8601) && echo $SNCVERSION` - this is the version name
   - `git push origin HEAD:refs/tags/$SNCVERSION`
   - _Play Console > App releases > Production/Manage > Create Release_
     - Upload app
     - Check version code
     - Write release notes
     - _Save > Review > Start roll-out to production_


## Dependencies

 - [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)
 - [Android Studio](https://developer.android.com/studio)


## Notes

### Architecture

The current architecture (not ideal) is as follows:

    app -<- REST -<- sermon metadata (JSON), hosted on Azure (managed independently from SNC website)
    app -<- HTTP -<- sermon media (MP4), hosted on AWS (shared with & managed by SNC website)
    app -<- REST -<- church calendar (Google Calendar API)
    upload webapp --- update sermon metadata, hosted on Azure

This has the advantage of not requiring a separate web server for the sermon metadata (without which the app would break), although it is less flexible as it limits the sermon metadata to be a single static object.

### Create an Azure Storage Account

Creating a storage account suitable for uploading sermon metadata, using an Azure Storage Account for Blob storage:

 - Name: `stnicksappDATE`
 - Region: `UK (South)`
 - Account kind: `Blob storage`
 - Performance: `Standard`
 - Access tier: `Hot`
 - Replication: `LRS`
 - Secure transfer required: `Disabled`
 - Resource group `stnicksapp`
 - Location: `West Europe`
