
## Get started

Add the plugin to your pom.xml and configure the files to process.

```xml
<plugin>
    <groupId>de.mhus.mvn.tmpl</groupId>
    <version>2.1.0</version>
    <artifactId>tmpl-maven-plugin</artifactId>
    <configuration>
        <files>
            <directory>${basedir}</directory>   
            <includes>
               <include>*</include>
            </includes>
        </files>
    </configuration>
</plugin>
```

And run the plugin with

`mvn de.mhus.mvn.tmpl:tmpl-maven-plugin:2.1.0:tmpl`

## Configuration

Default start/stop characters are '±' and '±'. You can change them with the properties `startChar` and `endChar`.

Configure the files to process with the `files` configuration and the parameters to match tmpl files:

* filePrefix and fileSuffix - The prefix and suffix of the files to process (without file extension)
* fileExtension - The file extension of the files to process. Default is '.tmpl'
* targetDir - The directory where the processed files are stored. Default is the same as the source directory.
* verbose - Print out the processed files

### Examples:

Default configuration:

```
filePrefix=""
fileSuffix="-tmpl"
fileExtension=".tmpl"
targetDir="" # not set
flattenTargetDir=false
```

* `src/main/resources/test1-tmpl.sh` -> `src/main/resources/test1.sh`
* `src/main/resources/test2.sh.tmpl` -> `src/main/resources/test2.sh`

To another target directory:

```
filePrefix=""
fileSuffix="-tmpl"
fileExtension="" # not set
targetDir="docs"
flattenTargetDir=false
```

* `src/main/resources/test-tmpl.sh` -> `docs/src/main/resources/test.sh`

To another target directory, flatten:

```
filePrefix=""
fileSuffix="-tmpl"
fileExtension="" # not set
targetDir="docs"
flattenTargetDir=false
```

* `src/main/resources/test1-tmpl.sh` -> `docs/test1.sh`
* `src/main/test2-tmpl.sh` -> `docs/test2.sh`

## Template engine and language

The mojo is using https://github.com/antlr/stringtemplate4/blob/master/doc/index.md as template engine.

### Examples:

* `±name±` - Attribute reference
* `±if (condition)±` ... `±endif±` - Conditional
* `±supportcode()±` - Template include

## Known attributes

* Environment parameters
* Java System properties
* Project properties
* `project_version` - Project Version
* `project_name` - Project Name
* `project_groupId` - Project GroupId
* `project_artifactId` - Project ArtifactId
* `basedir` - Base directory
* `parent_version` - Parent Version
* `parent_groupId` - Parent GroupId
* `parent_artifactId` - Parent ArtifactId
* `from_name` - From File Name
* `to_name` - To File Name
* `now_datetime` - Current Date and Time in ISO format
* `now_date` - Current Date in ISO format


