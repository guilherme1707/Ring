# Ring [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### A new solution for user generated external data
###### Every day many companies are generating and processing new data, whether it is structured, semi-structured or non structured. All these data are used for analytics and decision taken at any moment, and in the mostly we need to join data from differents sources to improve our analysis and the best way to achieve this is through BigData.
###### Sometimes when we need to create a KIP or a new Dashboard, we may need some data not avalable in our data sources or for any reason it's not possible to extract, so we generate new files to integrate with our tools but it's hard to keep a pattern and define the best way to consume the files in the processes.
###### **That's why Ring was created!** Ring is a tool to keep track of user generated NoSQL data, where you can define a template, upload files and validate uploaded data. This way Ring becomes a new source you can integrate with your tools extracting data via API, and Ring will take care of providing more resilient data to your processes.
###### You can define templates to upload CSV file, XLSX file or extract data by connecting to Google Sheets. By default Ring store data in an aws S3 bucket and Ring also has a class to store data in a MongoDB, but you can store data anywhere you want as json file. Ring storage is modular, so you can code a new class extending StorageAbstractionTemplate class implementing the main methods for CRUD operations and then say to StorageManagerService what module to use.

## BPM

This Diagram show how Ring is expected to work in the business.

![Ring BPM](https://raw.githubusercontent.com/dafiti-group/ring/master/RingBPM.png)

## Instalation

> If you prefer, you can create a docker image and run a container of the application. Step by step available [here](https://github.com/dafiti-group/ring/wiki/Criando-uma-imagem-docker-para-o-Ring) in Portuguese only.

##### REQUIREMENTS

- Tomcat 8 +
- MySQL 5.7 +
- Java 8 +

##### BUILD
Using [Maven](https://maven.apache.org/):

- Access the directory where Ring source code is placed.
- Use the command **mvn package**.
- The file ring.war will be created in subdirecotry *target*.

##### CONFIGURATION

- Create the temporary directory where system will use to handle the files

```shell
mkdir -p ~/tmp_files/ring/
```

- Create the file `~/.ring/ring.properties` with the following content

```properties
####### DATABASE CONFIGURATION #######

# Ring MySQL
spring.datasource.url=jdbc:mysql://<host>:<port>/<db>
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.datasource.validationQuery=SELECT 1

# Ring Secret Key
ring.encrypt.key=<Any key>
 
# Ring Anonymous Access
ring.anonymous.access=true
 
# Log
logging.level.root=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=INFO
logging.level.org.hibernate.SQL=WARN
 
# Timezone
spring.jackson.time-zone=America/Sao_Paulo

server.port=8080

# Ring Enable native validation
ring.enable.native.data.validation=true

# Defines how file will be sent to StorageManagerService class
# as a JSON file = {} or as a JSONArray file = [{},{}]
ring.json.file.type=JSONArray

##### CUSTOM STORAGE CONFIGURATION #######
# Ring has S3 as default configuration

# Ring MongoDB
mongo.datasource.uri=mongodb://127.0.0.1:27017/?gssapiServiceName=mongodb


# Ring AWS S3
aws.s3.access.key=<aws-access-key>
aws.s3.secret.key=<aws-secret-key>
aws.s3.bucket.name=<aws-s3-bucket-name>
aws.s3.bucket.key=ring
aws.s3.bucket.region=<aws-s3-bucket-region>
```

##### SETTING UP GOOGLE SHEETS API `(Optional)`

If you want Ring to be able to connect to google spreadsheet you need to configure the API.
> The configuration is maden by following the Google Sheets java API Quickstart documentation

- Create the directory to store Google Sheets API configuration files
    `mkdir -p ~/.ring/gsheets/tokens/`
- Go to [Google Sheets java API documentation page](https://developers.google.com/sheets/api/quickstart/java)
- Find the button ***Enable the Google Sheets API***, download the file *credentials.json* and save in the directory `~/.ring/gsheets/`.
- Download and run the .jar file configureGoogleSheetsApi.jar with the credentials.json file path.
```shell
## This execution will open the browser to authenticate your Google account
java -jar configureGoogleSheetsApi.jar ${CREDENTIALS_FILE_PATH}
```
- It will genrete a folder named `tokens`, copy the file ***StoredCredential*** from inside generated folder tokens to `~/.ring/gsheets/tokens/`.

Now your API for Google Sheets ready to use with Ring.


##### DEPLOY
Using [Apache Tomcat](http://tomcat.apache.org/):

- Copy the file ring.war to webapps directory of Apache Tomcat.

##### APPLIATION START

- When application runs for the first time, Ring generate an admin user with
    - username: ***ring.manager***
    - passowrd: ***rmanager***
> You can change it's password later
- After your first login go to Configuration section to configure your e-mail service. It's required to create new users and reset users password.

### IMPORTING DATA
#### Data Type

When  inserting data in Ring, an important point to pay attention is about data type, mainly for DECIMAL and DATE values.

- You may define a field as decimal and input a value formatted like 1.350,8 that Ring will try to parse, but is recomended to input decimal values dot separated (for example: 1350.8) to avoid errors in your data.
- For fields defined as DATE or DATE_TIME it's important to follow the pattern format `YYYY-MM-DD` for *DATE* and `YYYY-MM-DD HH:MI:SS` for *DATE_TIME*.

#### Peculiarities of Different File Formats
**CSV FILE**
> When inputing a CSV file it's important to follow the letter for each data type pattern, mainly about DATE and DATA_TIME

**XLSX FILE**
> Fields formatted as Date in the file should be automaticaly parsed to YYYY-MM-DD HH:MI:SS format during import process, and you should not worry about formatted decimal fields, the process should be able to identify the configurations of an xlsx file.

>**Important:** When defining the data type for some field in metadata of a manual input, always use DECIMAL for numerical fields, INTEGER data type is not supported for xlsx files.

**GOOGLE SHEETS**
> You have to be careful about data in google sheets, because the data is retrieved as it is formatted, so if you have a decimal value formatted as money, the process will get a string value like 'R$ 1.154,12' and extraction will fail if your process if configured to receive a decimal value.

#### Fields in File

When you define the metadata of your manual input, the process expect to find all fields in the file, but you have more than the needed columns the process ignore the others.

# User Manual
this section explain the main funcionalities of Ring


## Home
Shows a quicky text about how to use the system
- In the side menu, access the option ***Home***.

## Manual Input
*Manual Input* is a template that says the pattern the user file should follow to upload in the system
- In the side menu, access the option ***Manual Input***.

##### CREATE MANUAL INPUT
- Click in the button Create Manual Input, represented with **+** icon.
- Choose the file type to upload in your manual input
- Define the manual input name in the field ***Manual Input Name***, the field should match the pattern **[0-9a-z_]***.
- Define the group of manual input in the field ***Group*** if you have role LORD, or else the group is setted to the same group of logged user.
- Define the checkbox ***Can be modified*** (Default: checked). If this option is not checked, nobody can make modification in metadata, only an user with role LORD can check again
- Define a description in the field ***Description***. This field is very important to understand it's purpose.
    > ***FOR CSV FILE***
    
    - The fields ***Delimiter***, ***Quote***, ***Escape*** and ***Line Separator*** have a default value and you change change as you wish.
    
    > ***FOR XLSX FILE***
    
    - Define the sheet name in the field ***Sheet Name***. The sheet name to find in file to process.
    
    > ***FOR GOOGLE SHEETS***
    
    - Define the google spreadsheet key in the field ***SpreadSheet Key*** to connect with Google Sheets API.
    - Define the sheet name in the field ***Sheet Name***. The sheet name to find in file to process.
    - Define the range to select the data in the sheet in the field ***Range***. This field is not required, but we suggest you define a range avoid errors. Examples:-> A1:E, B:G, C13:W120
    
- Define the the name of each column in metadata for your manual input in the field ***Field Name***. It defines what header is expected in the coming file to upload. It's not allowed 2 fields with the same name.
- Define the the data type of each column in metadata for your manual input in the field ***Data Type***.
- Define the the validation of each column in metadata for your manual input in the field ***Validation***.
- Define the the Threshold of each column in metadata for your manual input in the field ***Threshold***. It defines what value to campare for validation.
- Define a combination of field to create a business key checking the box ***Business Key***. A business key field combination defines uniq id for the data. Ring don't updates the records, but you may identity version of a record to work with in an integration process
    - ###### ADD FIELD
        > You may add a new field in your metadata template, indication that a field with that name in file header is required. (click in the button represented by **+**)
    - ###### REMOVE FIELD
        > You may remove a field from your template (click in the button represented by **-**) identicating that a field with that name in file header is not required anymore. However, in the backend the field keep saved as part of metadata of your manual input.
        If in the future you try to add a field with the name of a field you have ever removed, then the removed field is activated and take place of new field.
        However, if you try to add a field with the name of a field you have ever removed but with a different data type, it will be created a new version of the field, being renamed with pos-fix ***_v2***, and will be required your file to have a field with pos-fix ***_v2***.
    - ###### HOW TO SET METADATA
        > If you have any doubt about to set up your metadata you can click in button "how to set metadata?" represented by ***?*** icon to get some hints.

##### EDIT MANUAL INPUT
Allows alter a manual input if user has role LORD or ADMIN in the same group
- Click in the edit button represented by a ***pencil*** icon.

##### DELETE MANUAL INPUT
Removes a group from systen
- Click in the delete button represented by a ***trash*** icon.

##### VIEW MANUAL INPUT
Shows the group configuration in view mode
- Click in the view button represented by a ***eye*** icon.
In the view mode you can
- Edit Manual Input: Click in the edit button represented by a ***pencil*** icon.
- Delete Manual Input: Click in the delete button represented by a ***trash*** icon.
- Upload File: Click in the upload button represented by a ***up arrow*** icon.
- See historical log of last 6 uploads in manual input: Click in the log history button represented by a ***clock*** icon.
    - Click in row of log to see the full text output


## Group
A group relate a set of users with a set of manual inputs, so an user can work only with manual inputs related to his group
- In the side menu, access the option ***Group***.

##### CREATE GROUP
- Click in the button Create Group, represented with **+** icon.
- Define the group name in the field ***Group Name***.
- Define the group description in the field ***Description***.
- click in button ***Save***.

##### EDIT GROUP
Allows alter a group if user has role LORD
- Click in the edit button represented by a ***pencil*** icon.

##### VIEW GROUP
Shows the group configuration in view mode
- Click in the view button represented by a ***eye*** icon.

##### DELETE GROUP
Removes a group from systen
- Click in the trash button represented by a ***trash*** icon.


## Search

*Search* is the fastest way to find a manual input in Ring

- In the side menu, access the option *Search*.
- Will be shown the search page with a text box and a button with a ***magnifying glass*** icon.
- Type the content to be searched and click enter or click in the button with a ***magnifying glass*** icon.

## User
An User is requeied to operate the tool.
- In the side menu, access the option ***User***.

##### ADD USER
- Click in the button Add User, represented with **+** icon.
- Define the user e-mail in the field ***E-mail***.
- Define the user name in the system in the field ***Username***.
- Define the first name of user in the field ***First Name***.
- Define the last name of user in the field ***Last Name***.
- Define an access profile in the field ***Role***.
- Define an user group in the field ***Group***.

**LORD:** This User is the integral administrator of the system and can effect any operation.

**ADMIN:** This User has permission to manage (create, delete, edit) new manual input and upload files to his group.

**USER:** This User has permission only to upload files in a manual input of his group.

- It's possible to define whether the user is active or not in the system with the button ***Enabled***.
- click in button ***Save***.

##### EDIT
Allows alter an User and, for LORD, redefine the password of others users:
- Click in the edit button represented by a **pencil** icon.
- Click in the button ***Reset password***.
- An e-mail will be sent to the user with the new password.

##### DELETE
Removes a user from system.

##### CHANGE PASSWORD
Allows the user to alter his own password.

## Configuration
*Configuration* contains the global settings of Ring.

- In the side menu, access the option ***Configuration***
- Enter with the server used to sent e-mails in the field ***Host***.
- Enter with the port of e-mail server in the field ***Port***.
- Enter with the e-mail address in the field ***Address***. This e-mail address will be used to sent system e-mails.
- Enter with the e-mail password in the field ***Password***.
- In the field ***Log Retention*** is possible to define in days a clean up of the *logs of import files*
- Click in the button ***Upload Logo*** to alter the logo of the system with any image file.


## API Rest

- In the side menu, access the option ***API Rest***

In this section you can access the API documentation.
Configure the call to API with header `Content-Type: applcation/json`
Get your user token by calling `/api/auth` sending in the body a JSON with your username and passowrd (your user may have role ADMIN or LORD)

```json
{
    "username":"your.username",
    "password":"your.pass"
}
```

Then you will receive a token to send in header for next API calls. User the header `Authorization: Bearer ${TOKEN}`.
Here is simple examples of URL calls, you can see more details about each URL call in the sction API Rest of application.

```shell
`POST /api/auth` Authenticate user. Return a token if user is valid.
`GET  /api/get?manualInput=<manual input name>`  Return the CSV file of a manual input respecting the filters.
`GET  /api/load?manualInput=<manual input name>` (not available yet) You can yupload a file remotely to your manual input.
`GET  /api/gsheets/extract?manualInput=<manual input name>` (not available yet) allows you to start remotely extact data from Google Sheets to Ring application.
```
