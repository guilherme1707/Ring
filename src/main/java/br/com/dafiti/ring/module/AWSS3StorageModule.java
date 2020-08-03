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
import br.com.dafiti.ring.rest.ApiFilterDTO;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author guilherme.almeida
 */
public class AWSS3StorageModule extends StorageAbstractionTemplate {

    private final String clientRegion;// us-east-1
    private final String bucketName;// dft-dwh-files
    private final String keyName; // transportation_manual_input/teste.csv
    private final String accessKey;
    private final String secretKey;

    private AmazonS3 s3Client;
    private TransferManager transferManager;

    public AWSS3StorageModule(String clientRegion,
            String bucketName,
            String keyName,
            String accessKey,
            String secretKey) {
        this.clientRegion = clientRegion;
        this.bucketName = bucketName;
        this.keyName = keyName;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void createOrUpdateManualInput(ManualInput manualInput, boolean recreate) {
        if (recreate) {
            deleteManualInput(manualInput);
        }
    }

    @Override
    public void saveFile(ManualInput manualInput, JSONDocument JSONDoc, String LoadDateForPartition, FileHandler fileHandler) throws Exception {
        String fileContent = JSONDoc.generateDocuments(manualInput, fileHandler, LoadDateForPartition);

        // get result and write JSON file
        String outputFilePath = tmpFilePath + manualInput.getName() + "_" + manualInput.getId() + ".json.gzip";
        File JSONFile = new File(outputFilePath);
        if (JSONFile.exists()) {
            JSONFile.delete();
        }

        FileOutputStream outputStream = null;
        GZIPOutputStream gzipOutputStream = null;
        try {
            // write a compressed file to upload in S3
            outputStream = new FileOutputStream(JSONFile);
            gzipOutputStream = new GZIPOutputStream(outputStream);
            byte[] strToBytes = fileContent.getBytes();
            gzipOutputStream.write(strToBytes);
        } catch (Exception e) {
        } finally {
            try {
                gzipOutputStream.finish();
                gzipOutputStream.close();
                outputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(AWSS3StorageModule.class.getName()).log(Level.ALL, null, ex);
            }
        }

        // send to S3
        try {
            // you may instantiate AmazonS3 Object as following if you have AWS CLI configured with de credentials located at ~/.aws/credentials
            //s3Client = AmazonS3ClientBuilder.defaultClient();
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withRegion(clientRegion)
                    .build();
            transferManager = TransferManagerBuilder.standard()
                    .withS3Client(s3Client)
                    .build();

            // TransferManager processes all transfers asynchronously,
            // so this call returns immediately.
            String s3Path = keyName + "/" + manualInput.getName()
                    + extractDatePathForS3(LoadDateForPartition, " ")
                    + LoadDateForPartition.replace(" ", "_") + ".json.gzip";
            Upload upload = transferManager.upload(bucketName, s3Path, new File(outputFilePath));

            System.out.println("Object upload started");

            // Optionally, wait for the upload to finish before continuing.
            upload.waitForCompletion();

            if (upload.isDone()) {
                System.out.println("Object upload complete");
            } else {
                System.out.println("Something went wrong");
            }

        } catch (AmazonClientException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (transferManager != null) {
                transferManager.shutdownNow();
            }
            if (s3Client != null) {
                s3Client.shutdown();
            }
        }

        // delete file
        if (JSONFile.exists()) {
            JSONFile.delete();
        }

    }

    @Override
    public void deleteManualInput(ManualInput manualInput) {
        try {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withRegion(clientRegion)
                    .build();

            List<S3ObjectSummary> summaries = listObjects(bucketName, keyName + "/" + manualInput.getName());
            String[] keys = summaries.stream()
                    .map(m -> m.getKey())
                    .toArray(String[]::new);

            DeleteObjectsRequest dor = new DeleteObjectsRequest(bucketName)
                    .withKeys(keys);
            s3Client.deleteObjects(dor);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            s3Client.shutdown();
        }
    }

    @Override
    public File extractCSV(ManualInput manualInput, ApiFilterDTO filter) throws IOException, Exception {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(clientRegion)
                .build();

        try {
            // list files of manual Input in S3
            List<S3ObjectSummary> summaries = listObjects(bucketName, keyName + "/" + manualInput.getName());

            // maps only the keys of files to be downloaded and returned
            List<String> keyList = summaries.stream()
                    .filter(f -> evaluate(filter, f.getKey()))
                    .map(m -> m.getKey())
                    .collect(Collectors.toList());

            // download and save the files in a single file
            File tmpDir = new File(tmpFilePath);
            String outputFilePath = manualInput.getName() + "_" + manualInput.getId();
            File targetCsvFile = File.createTempFile(outputFilePath, ".csv", tmpDir);
            if (targetCsvFile.exists()) {
                targetCsvFile.delete();
            }
            // set compressed json file
            //File fileToDel = new File(outputFilePath + ".json.gizp");
            File compressedFile = File.createTempFile(outputFilePath, ".json.gizp", tmpDir);
            if (compressedFile.exists()) {
                compressedFile.delete();
            }

            // preapare CSV schema
            Builder csvSchemaBuilder = CsvSchema.builder();

            csvSchemaBuilder.setColumnSeparator(filter.getDelimiter());
            csvSchemaBuilder.setQuoteChar(filter.getQuote());
            csvSchemaBuilder.setEscapeChar(filter.getEscape());
            csvSchemaBuilder.setLineSeparator(filter.getLineSeparator());
            
            csvSchemaBuilder.addColumn("partition_field");
            csvSchemaBuilder.addColumn("custom_primary_key");
            csvSchemaBuilder.addColumn("load_date");

            for (Metadata metadata : manualInput.getMetadata()) {
                csvSchemaBuilder.addColumn(metadata.getFieldName());
            }
            
            boolean addHeaderToCsv = true;

            for (String key : keyList) {
                S3Object o = s3Client.getObject(bucketName, key);
                S3ObjectInputStream s3is = o.getObjectContent();
                FileOutputStream fos = new FileOutputStream(compressedFile, false);
                byte[] read_buf = new byte[1024];
                int read_len = 0;
                while ((read_len = s3is.read(read_buf)) > 0) {
                    fos.write(read_buf, 0, read_len);
                }
                fos.close();

                // create a decompressed json file and delete compressed json file
                String decompressedFilePath = decompressGzip(compressedFile.getAbsolutePath(), outputFilePath, ".json", tmpDir);

                if (compressedFile.exists()) {
                    compressedFile.delete();
                }

                // set json file
                File decompressedJsonFile = new File(decompressedFilePath);

                JsonNode jsonTree = new ObjectMapper().readTree(decompressedJsonFile);
                
                CsvSchema csvSchema = null;
                if(addHeaderToCsv) {
                    csvSchema = csvSchemaBuilder.build().withHeader();
                    addHeaderToCsv = false;
                } else {
                    csvSchema = csvSchemaBuilder.build();
                }
                
                CsvMapper csvMapper = new CsvMapper();
                csvMapper.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValue(new FileOutputStream(targetCsvFile, true), jsonTree);

                // delete json file
                if (decompressedJsonFile.exists()) {
                    decompressedJsonFile.delete();
                }
            }

            return targetCsvFile;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.toString());
        }

    }

    public List<S3ObjectSummary> listObjects(String bucketName, String keyName) {
        // list files of manual Input in S3
        ObjectListing listing = s3Client.listObjects(bucketName, keyName);
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();

        while (listing.isTruncated()) {
            listing = s3Client.listNextBatchOfObjects(listing);
            summaries.addAll(listing.getObjectSummaries());
        }
        return summaries;
    }

    /**
     *
     */
    private boolean evaluate(ApiFilterDTO filter, String key) {

        String regex = "[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}:[0-9]{2}:[0-9]{2}\\..+";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(key);

        if (!matcher.find()) {
            return false;
        }

        String filterDate = filter.getLoadDate();
        String loadDate = matcher.group(0).replace("_", " ").replaceAll("\\.json.*", "");
        String operator = filter.getOperator();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date1 = sdf.parse(loadDate);
            Date date2 = sdf.parse(filterDate);

            if (operator.equals("gt")) {
                if (date1.compareTo(date2) > 0) {
                    return true;
                }
            } else if (operator.equals("gte")) {
                if (date1.compareTo(date2) > 0 || date1.compareTo(date2) == 0) {
                    return true;
                }
            } else if (operator.equals("lt")) {
                if (date1.compareTo(date2) < 0) {
                    return true;
                }
            } else if (operator.equals("lte")) {
                if (date1.compareTo(date2) < 0 || date1.compareTo(date2) == 0) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     *
     */
    private String decompressGzip(String input, String output, String extension, File tmpDir) throws IOException {
        File toDel = new File(output);
        if (toDel.exists()) {
            toDel.delete();
        }

        File tmp = File.createTempFile(output, extension, tmpDir);

        try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(input))) {
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
            }
        }
        return tmp.getAbsolutePath();
    }

    private String extractDatePathForS3(String DateFormatText, String splitBy) {

        String date = DateFormatText.split(splitBy)[0];
        String[] dateParts = date.split("-");
        String path = "";
        String[] parts = {"year", "month", "day"};
        for (int i = 0; i < dateParts.length; i++) {
            String part = dateParts[i];
            path += "/" + parts[i] + "=" + part;
        }

        return path + "/";
    }

}
