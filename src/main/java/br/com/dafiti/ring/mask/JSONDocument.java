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
package br.com.dafiti.ring.mask;

import br.com.dafiti.ring.model.FileHandler;
import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.model.Metadata;
import br.com.dafiti.ring.option.Conditional;
import br.com.dafiti.ring.option.DataType;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author guilherme.almeida
 */
public class JSONDocument {

    private final String jsonType;
    protected String lineSeparator;
    protected boolean useNativeValidation;

    public JSONDocument(String jsonType, boolean useNativeValidation) {
        this.jsonType = jsonType.toLowerCase();
        if (this.jsonType.equals("jsonarray")) {
            lineSeparator = ",";
        } else {
            lineSeparator = "\n";
        }
        this.useNativeValidation = useNativeValidation;
    }

    /**
     * @param manualInput
     * @param file
     * @param loadDate
     * @return
     * @throws java.lang.Exception
     */
    public String generateDocuments(ManualInput manualInput, FileHandler file, String loadDate) throws Exception {

        // String of documents to return in the end of function
        StringBuilder documents = new StringBuilder();
        documents.append(this.jsonType.equals("jsonarray") ? "[" : "");

        // get the header of the file as list to be easer to work with
        List<String> header = Arrays.asList(file.getHeader());
        // get the metadata filtering only the required fields expected in file
        List<Metadata> metadataList = manualInput.getMetadata()
                .stream()
                .filter(f -> f.getIsActive())
                .collect(Collectors.toList());
        // This List will be used to store the position of columns in file
        // That are the same setted in ManualInput's Metadata
        // this way the user can input a file with many column but the process with be able to get only the required ones
        // and ignore the columns are not part of setted in metadata
        ArrayList<Integer> fieldIndex = new ArrayList<>();

        for (int i = 0; i < metadataList.size(); i++) {
            Metadata metadata = metadataList.get(i);
            String fieldName = metadata.getFieldName();
            if (header.contains(fieldName)) {
                fieldIndex.add(header.indexOf(fieldName));
            } else {
                throw new Exception("ERROR: missing column in file: field \"" + fieldName + "\" not found!");
            }
        }

        // creates an array with the number of columns in metadata adding 3 more indexs
        // the 3 aditional fields refers to the default columns to generate tables and collections for manual input
        // partition_field, custom_primary_key and load_date
        String[] jsonLine = new String[metadataList.size() + 3];

        int fileRowQty = file.getData().size();

        for (int line = 0; line < fileRowQty; line++) {

            String[] row = file.getData().get(line);
            // validate if the row has the same length of header
            if (row.length != header.size()) {
                throw new Exception("ERROR: row size contains more columns than header: " + Arrays.asList(row).toString());
            }
            String businessKey = "";

            for (int column = 0; column < fieldIndex.size(); column++) {

                String field = header.get(fieldIndex.get(column));
                String value = row[fieldIndex.get(column)];
                Metadata metadata = metadataList.stream()
                        .filter(f -> f.getFieldName().equals(field))
                        .collect(Collectors.toList()).get(0);

                DataType dt = metadata.getDataType();

                if (metadataList.get(metadataList.indexOf(metadata)).getIsBusinessKey()) {
                    businessKey += value;
                }
                if (useNativeValidation) {
                    boolean valid = validate(value, dt, metadata.getTest(), metadata.getThreshold());
                    if (!valid) {
                        throw new Exception("ERROR: error validating data - field: " + field
                                + "\n, value: " + value
                                + "\n, data type: " + dt.toString()
                                + "\n, test: " + metadata.getTest().toString()
                                + "\n, threshold: " + metadata.getThreshold());
                    }
                }
                // validate numerical data type
                // I chose write a json string to be easer to understand
                try {
                    if (value == null || value.isEmpty()) {
                        jsonLine[column] = "\"" + field + "\": null";
                    } else if (dt.equals(DataType.INTEGER)) {
                        jsonLine[column] = "\"" + field + "\": " + Integer.parseInt(value);
                    } else if (dt.equals(DataType.DECIMAL)) {
                        Double doubleValue;
                        try {
                            doubleValue = Double.parseDouble(value);
                        } catch (Exception e) {
                            doubleValue = Double.parseDouble(value.replace(".", "").replace(",", "."));
                        }
                        jsonLine[column] = "\"" + field + "\": " + doubleValue;
                    } else {
                        jsonLine[column] = "\"" + field + "\": \"" + value.replace("\"", "\\\"") + "\"";
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new Exception("ERROR: " + e.toString());
                }
            }

            // add 3 default fields: partition_field, custom_primary_key and load_date
            jsonLine[metadataList.size()] = "\"partition_field\":\"FULL\"";
            // define business key with hash
            if (businessKey.isEmpty()) {
                businessKey = line + loadDate;
            }

            HashFunction hf = Hashing.farmHashFingerprint64();
            Long businessKeyHashFingerPrint = hf.hashBytes(businessKey.getBytes()).asLong();
            jsonLine[metadataList.size() + 1] = "\"custom_primary_key\":" + businessKeyHashFingerPrint;
            // set load_date
            jsonLine[metadataList.size() + 2] = "\"load_date\":\"" + loadDate + "\"";

            String jsonToParse = "{" + String.join(", ", jsonLine) + "}";

            try {
                documents.append(jsonToParse);
                if (line < fileRowQty - 1) {
                    documents.append(this.lineSeparator);
                }
            } catch (Exception e) {
                throw new Exception("ERROR: " + e.toString());
            }
        }

        return documents.append(this.jsonType.equals("jsonarray") ? "]" : "\n").toString();
    }

    /**
     * @param manualInput
     * @param file
     * @param loadDate
     * @param listObjectGenerator
     * @throws java.lang.Exception
     */
    public void generateDocuments(ManualInput manualInput, FileHandler file, String loadDate, ListObjectGenerator listObjectGenerator) throws Exception {

        // get the header of the file as list to be easer to work with
        List<String> header = Arrays.asList(file.getHeader());
        // get the metadata filtering only the required fields expected in file
        List<Metadata> metadataList = manualInput.getMetadata()
                .stream()
                .filter(f -> f.getIsActive())
                .collect(Collectors.toList());
        // This List will be used to store the position of columns in file
        // That are the same setted in ManualInput's Metadata
        // this way the user can input a file with many column but the process with be able to get only the required ones
        // and ignore the columns are not part of setted in metadata
        ArrayList<Integer> fieldIndex = new ArrayList<>();

        for (int i = 0; i < metadataList.size(); i++) {
            Metadata metadata = metadataList.get(i);
            String fieldName = metadata.getFieldName();
            if (header.contains(fieldName)) {
                fieldIndex.add(header.indexOf(fieldName));
            } else {
                throw new Exception("ERROR: missing column in file: field \"" + fieldName + "\" not found!");
            }
        }

        // creates an array with the number of columns in metadata adding 3 more indexs
        // the 3 aditional fields refers to the default columns to generate tables and collections for manual input
        // partition_field, custom_primary_key and load_date
        String[] jsonLine = new String[metadataList.size() + 3];

        for (int line = 0; line < file.getData().size(); line++) {

            String[] row = file.getData().get(line);
            // validate if the row has the same length of header
            if (row.length != header.size()) {
                throw new Exception("ERROR: row size contains more columns than header: " + Arrays.asList(row).toString());
            }
            String businessKey = "";

            for (int column = 0; column < fieldIndex.size(); column++) {

                String field = header.get(fieldIndex.get(column));
                String value = row[fieldIndex.get(column)];
                Metadata metadata = metadataList.stream()
                        .filter(f -> f.getFieldName().equals(field))
                        .collect(Collectors.toList()).get(0);
                
                DataType dt = metadata.getDataType();

                if (metadataList.get(metadataList.indexOf(metadata)).getIsBusinessKey()) {
                    businessKey += value;
                }
                if (useNativeValidation) {
                    boolean valid = validate(value, dt, metadata.getTest(), metadata.getThreshold());
                    if (!valid) {
                        throw new Exception("ERROR: error validating data - field: " + field
                                + "\n, value: " + value
                                + "\n, data type: " + dt.toString()
                                + "\n, test: " + metadata.getTest().toString()
                                + "\n, threshold: " + metadata.getThreshold());
                    }
                }
                // validate numerical data type
                // I chose write a json string to be easer to understand
                try {
                    if (value == null || value.isEmpty()) {
                        jsonLine[column] = "\"" + field + "\": null";
                    } else if (dt.equals(DataType.INTEGER)) {
                        jsonLine[column] = "\"" + field + "\": " + Integer.parseInt(value);
                    } else if (dt.equals(DataType.DECIMAL)) {
                        Double doubleValue;
                        try {
                            doubleValue = Double.parseDouble(value);
                        } catch (Exception e) {
                            doubleValue = Double.parseDouble(value.replace(".", "").replace(",", "."));
                        }
                        jsonLine[column] = "\"" + field + "\": " + doubleValue;
                    } else {
                        jsonLine[column] = "\"" + field + "\": \"" + value.replace("\"", "\\\"") + "\"";
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    throw new Exception("ERROR: " + e.toString());
                }
            }

            // add 3 default fields: partition_field, business_ley and load_date
            jsonLine[metadataList.size()] = "\"partition_field\":\"FULL\"";
            // define business key with hash
            if (businessKey.isEmpty()) {
                businessKey = line + loadDate;
            }

            HashFunction hf = Hashing.farmHashFingerprint64();
            Long businessKeyHashFingerPrint = hf.hashBytes(businessKey.getBytes()).asLong();
            jsonLine[metadataList.size() + 1] = "\"custom_primary_key\":" + businessKeyHashFingerPrint;
            // set load_date
            jsonLine[metadataList.size() + 2] = "\"load_date\":\"" + loadDate + "\"";

            String jsonToParse = "{" + String.join(", ", jsonLine) + "}";

            try {
                listObjectGenerator.add(jsonToParse);
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("ERROR: " + e.toString());
            }
        }

    }

    /**
     *
     * @param value
     * @param dataType
     * @param conditional
     * @param threshold
     * @return
     * @throws java.lang.Exception
     */
    private boolean validate(String value, DataType dataType, Conditional conditional, String threshold) throws Exception {

        if (conditional.equals(Conditional.NONE)) {
            return true;
        }

        if (dataType.equals(DataType.TEXT)) {
            switch (conditional) {
                case EQUAL:
                    return value.equals(threshold);
                case NOT_EQUAL:
                    return !value.equals(threshold);
                case CONTAINS:
                    return value.contains(threshold);
                case NOT_CONTAINS:
                    return !value.contains(threshold);
                case IN:
                    return Arrays.asList(threshold.split(",")).stream()
                            .map(m -> m.trim())
                            .collect(Collectors.toList())
                            .contains(value);
                case NOT_IN:
                    return !Arrays.asList(threshold.split(",")).stream()
                            .map(m -> m.trim())
                            .collect(Collectors.toList())
                            .contains(value);
                case REGEX:
                    Pattern pattern = Pattern.compile(threshold);
                    Matcher matcher = pattern.matcher(value);
                    return matcher.find();
            }
        } else if (dataType.equals(DataType.DECIMAL) || dataType.equals(DataType.INTEGER)) {
            Float number = null;
            Float numThreshold = null;
            try {
                number = Float.parseFloat(value);
                if (!conditional.equals(Conditional.IN) && !conditional.equals(Conditional.NOT_IN)) {
                    numThreshold = Float.parseFloat(threshold);
                }
            } catch (Exception e) {
                try {
                    number = Float.parseFloat(value.replace(".", "").replace(",", "."));
                } catch (Exception ex) {
                    return false;
                }
            }
            switch (conditional) {
                case EQUAL:
                    return number == numThreshold;
                case NOT_EQUAL:
                    return !(number == numThreshold);
                case LOWER_THAN:
                    return number < numThreshold;
                case LOWER_THAN_OR_EQUAL:
                    return number <= numThreshold;
                case GREATER_THAN:
                    return number > numThreshold;
                case GREATER_THAN_OR_EQUAL:
                    return number >= numThreshold;
                case IN:
                    return Arrays.asList(threshold.split(",")).stream()
                            .map(m -> Float.parseFloat(m))
                            .collect(Collectors.toList())
                            .contains(number);
                case NOT_IN:
                    return !Arrays.asList(threshold.split(",")).stream()
                            .map(m -> Float.parseFloat(m))
                            .collect(Collectors.toList())
                            .contains(number);
            }
        } else if (dataType.equals(DataType.DATE) || dataType.equals(DataType.DATE_AND_TIME)) {
            String format = "";
            if (dataType.equals(DataType.DATE_AND_TIME)) {
                format = "yyyy-MM-dd HH:mm:ss";
            } else {
                format = "yyyy-MM-dd";
            }
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date date1 = sdf.parse(value);
            Date date2 = sdf.parse(threshold);

            switch (conditional) {
                case EQUAL:
                    return date1.compareTo(date2) == 0;
                case NOT_EQUAL:
                    return !(date1.compareTo(date2) == 0);
                case LOWER_THAN:
                    return date1.compareTo(date2) < 0;
                case LOWER_THAN_OR_EQUAL:
                    return date1.compareTo(date2) < 0 || date1.compareTo(date2) == 0;
                case GREATER_THAN:
                    return date1.compareTo(date2) > 0;
                case GREATER_THAN_OR_EQUAL:
                    return date1.compareTo(date2) > 0 || date1.compareTo(date2) == 0;
            }
        }

        return false;
    }

}
