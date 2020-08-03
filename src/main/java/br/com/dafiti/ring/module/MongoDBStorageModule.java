/*
 * Copyright (c) 2020 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.ring.module;

import br.com.dafiti.ring.mask.JSONDocument;
import br.com.dafiti.ring.mask.StorageAbstractionTemplate;
import br.com.dafiti.ring.model.FileHandler;
import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.model.Metadata;
import br.com.dafiti.ring.option.Conditional;
import br.com.dafiti.ring.option.DataType;
import br.com.dafiti.ring.rest.ApiFilterDTO;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.model.Indexes;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.bson.Document;

/**
 *
 * @author guilherme.almeida
 */
public class MongoDBStorageModule extends StorageAbstractionTemplate {
    
    private final String datasourceUri;
    
    public MongoDBStorageModule(String datasourceUri) throws NoSuchAlgorithmException {
        this.datasourceUri = datasourceUri;
    }

    /**
     * * use the attributes of manualInput object to create and configure a
     * MongoDB collection recreates collection if required, dropping and setting
     *
     * @param manualInput
     * @param recreate
     */
    @Override
    public void createOrUpdateManualInput(ManualInput manualInput, boolean recreate) {
        
        String collectionName = manualInput.getName();
        // connect to mongoDB
        MongoClient client = MongoClients.create(datasourceUri);
        MongoDatabase mongoDB = client.getDatabase("ring");

        List<String> collections = new ArrayList<>();
        ((Iterable<String>) mongoDB.listCollectionNames()).forEach(e -> collections.add(e));
        String validator = getSchemaValidator(manualInput.getMetadata());

        if (recreate && collections.contains(collectionName)) {
            mongoDB.getCollection(collectionName).drop();
            collections.remove(collectionName);
        }

        if (!collections.contains(collectionName)) {
            mongoDB.createCollection(collectionName);
            MongoCollection collection = mongoDB.getCollection(collectionName);
            collection.insertOne(Document.parse("{_initializer_:\"a record is needed to use collection in transaction\", load_date: \"2000-01-01 00:00:00\"}"));
            collection.createIndex(Indexes.text("load_date"));
        }
        mongoDB.runCommand(Document.parse("{ collMod:\"" + collectionName + "\", validator: " + validator + "}"));

        client.close();
    }

    
    /**
     * @param manualInput
     * @param JSONDoc
     * @param LoadDateForPartition
     * @param fileHandler
     */
    @Override
    public void saveFile(ManualInput manualInput
            , JSONDocument JSONDoc
            , String LoadDateForPartition
            , FileHandler fileHandler) throws Exception {
     
        MongoDocumentsGenerator mongoDocumentsGenerator = new MongoDocumentsGenerator();
        JSONDoc.generateDocuments(manualInput, fileHandler, LoadDateForPartition, mongoDocumentsGenerator);
        List<Document> documents = mongoDocumentsGenerator.get();
        
        MongoClient client = MongoClients.create(datasourceUri);
        MongoDatabase mongoDB = client.getDatabase("ring");

        /* Step 1: Start a client session. */
        final ClientSession clientSession = client.startSession();

        /* Step 2: Optional. Define options to use for the transaction. */
        TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.LOCAL)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        /* Step 3: Define the sequence of operations to perform inside the transactions. */
        TransactionBody txnBody = new TransactionBody<String>() {
            public String execute() {
                MongoCollection<Document> collection = mongoDB.getCollection(manualInput.getName());

                /*
                            Important:: You must pass the session to the operations.
                 */
                collection.insertMany(clientSession, documents);
                return "ok";
            }
        };

        try {
            clientSession.withTransaction(txnBody, txnOptions);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientSession.close();
            client.close();
        }
    }

    /**
     * 
     * @param manualInput
     * @param filter
     * @return 
     * @throws java.io.IOException
     */
    @Override
    public File extractCSV(ManualInput manualInput, ApiFilterDTO filter) throws IOException {
        String outputFilePath = tmpFilePath + manualInput.getName()+ "_" + manualInput.getId() + ".csv";

        File fileToDelete = new File(outputFilePath);
        if (fileToDelete.exists()) {
            fileToDelete.delete();
        }

        String command = "mongoexport --db ring --collection ${COLLECTION_NAME} --type csv --fields ${FIELD_LIST} --noHeaderLine --out ${OUTPUT_FILE} ";

        command = command.replace("${COLLECTION_NAME}", manualInput.getName());
        command = command.replace("${OUTPUT_FILE}", outputFilePath);

        String fieldList = "custom_primary_key,load_date";
        List<Metadata> sortedMetadata = manualInput.getMetadata()
                .stream()
                .sorted((s1, s2) -> s1.getOrdinalPosition().compareTo(s2.getOrdinalPosition()))
                .collect(Collectors.toList());

        for (Metadata metadata : sortedMetadata) {
            fieldList += "," + metadata.getFieldName();
        }

        command = command.replace("${FIELD_LIST}", fieldList);

        if (filter.getFilter() != null) {
            command += "--query " + filter.getFilter();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        File tmp = File.createTempFile("manual_input_", ".sh");

        //Define file permisssion.
        tmp.setExecutable(true);
        tmp.setReadable(true);
        tmp.setWritable(true);

        //Write shell command to sh file.
        try (FileWriter writer = new FileWriter(tmp)) {
            writer.write(command);
        }

        //Parse the command to run.
        CommandLine cmdLine = CommandLine.parse("sh " + tmp);

        //Define the executor.
        DefaultExecutor executor = new DefaultExecutor();

        //Define the timeout.
        executor.setWatchdog(new ExecuteWatchdog(300000));

        //Define the work directory.
        Path sandbox = Paths.get(tmpFilePath);
        Files.createDirectories(sandbox);
        executor.setWorkingDirectory(new File(sandbox.toString()));

        //Define the log.
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream, null);
        executor.setStreamHandler(streamHandler);

        //Execute the sh file.
        boolean success = false;
        try {
            success = (executor.execute(cmdLine) == 0);
        } catch (Exception e) {
            return null;
        }

        //Get the command log.
        String logText = outputStream.toString();

        System.out.println(logText);

        //Delete the sh file.
        tmp.delete();

        return new File(outputFilePath);
    }
    
    /**
     *  drop the collection of manual input
     * @param manualInput
     */
    @Override
    public void deleteManualInput(ManualInput manualInput) {
        String collectionName = manualInput.getName();
        // connect to mongoDB
        MongoClient client = MongoClients.create(datasourceUri);
        MongoDatabase mongoDB = client.getDatabase("ring");
        
        mongoDB.getCollection(collectionName).drop();
        client.close();
    }
    
    
    /**
     * read a List of Metadata objects and returns a String in JSON format
     * containing all rules of validations to set in MongoDB collection
     *
     * @param metadataList
     * @return
     */
    private String getSchemaValidator(List<Metadata> metadataList) {
        // https://docs.mongodb.com/manual/reference/operator/query/jsonSchema/#op._S_jsonSchema
        //https://docs.mongodb.com/manual/reference/operator/query/#query-selectors

        StringBuilder validator = new StringBuilder();
        validator.append("{$and: [");

        for (int i = 0; i < metadataList.size(); i++) {
            Metadata metadata = metadataList.get(i);
            validator.append(i == 0 ? "" : ",");

            String name = "\"" + metadata.getFieldName() + "\"";
            String mongoCondition = getMongoTest(metadata.getTest());
            String mongoDataType = "";
            String threshold = metadata.getThreshold();

            switch (metadata.getDataType()) {
                case DATE:
                case DATE_AND_TIME:
                case TEXT:
                    mongoDataType = "\"string\"";
                    break;
                case DECIMAL:
                case INTEGER:
                    mongoDataType = "\"number\"";
                    break;
            }

            switch (metadata.getTest()) {
                case EQUAL:
                case NOT_EQUAL:
                case LOWER_THAN:
                case GREATER_THAN:
                case LOWER_THAN_OR_EQUAL:
                case GREATER_THAN_OR_EQUAL:
                    if (metadata.getDataType() == DataType.TEXT
                            || metadata.getDataType() == DataType.DATE
                            || metadata.getDataType() == DataType.DATE_AND_TIME) {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":\"").append(threshold).append("\"}}");
                    } else {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":").append(threshold).append("}}");
                    }
                    break;
                case REGEX:
                case CONTAINS:
                case NOT_CONTAINS:
                    if (metadata.getTest() == Conditional.NOT_CONTAINS) {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":/^((?!").append(threshold).append(").)*$/g}}");
                    } else {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":/").append(threshold).append("/g}}");
                    }
                    break;
                case IN:
                case NOT_IN:
                    if (metadata.getDataType() == DataType.TEXT
                            || metadata.getDataType() == DataType.DATE
                            || metadata.getDataType() == DataType.DATE_AND_TIME) {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":[\"").append(threshold.replaceAll(",", "\",\"")).append("\"]}}");
                    } else {
                        validator.append("{").append(name).append(": {$type: ").append(mongoDataType).append(", ").append(mongoCondition).append(":[").append(threshold).append("]}}");
                    }
                    break;
                case NONE:
                    validator.append("{").append(name).append(": {$exists: true}}");
                    break;
            }

        }

        validator.append("]}");

        return validator.toString();
    }
    
    
    /**
     * translate the conditional test to mongoDB syntax
     *
     * @param conditional
     * @return
     */
    private String getMongoTest(Conditional conditional) {
        String mongoCondition = "";
        switch (conditional) {
            case EQUAL:
                mongoCondition = "$eq";
                break;
            case NOT_EQUAL:
                mongoCondition = "$ne";
                break;
            case LOWER_THAN:
                mongoCondition = "$lt";
                break;
            case LOWER_THAN_OR_EQUAL:
                mongoCondition = "$lte";
                break;
            case GREATER_THAN:
                mongoCondition = "$gt";
                break;
            case GREATER_THAN_OR_EQUAL:
                mongoCondition = "$gte";
                break;
            case IN:
                mongoCondition = "$in";
                break;
            case NOT_IN:
                mongoCondition = "$nin";
                break;
            case NOT_CONTAINS:
            case CONTAINS:
            case REGEX:
                mongoCondition = "$regex";
                break;
        }

        return mongoCondition;
    }


    
}
