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
package br.com.dafiti.ring.service;

import br.com.dafiti.ring.model.Metadata;
import br.com.dafiti.ring.option.Conditional;
import br.com.dafiti.ring.option.DataType;
import br.com.dafiti.ring.repository.MetadataRepository;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class MetadataService {

    MetadataRepository metadataRepository;

    @Autowired
    public MetadataService(MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    /**
     * validate the metadata
     *
     * @param metadatas
     * @return
     */
    public boolean validate(List<Metadata> metadatas) {
        
        // get active field names that appears twice
        List<String> activeFieldsDuplicated = metadatas.stream()
                .filter(f -> f.getIsActive())
                .collect(Collectors.groupingBy(g -> g.getFieldName(), Collectors.counting()))
                .entrySet().stream()
                .filter(f -> f.getValue() > 1)
                .map(m -> m.getKey())
                .collect(Collectors.toList());

        // validate if user is tring to add 2 fields with the same name
        if (activeFieldsDuplicated.size() > 0) {
            return false;
        }

        // get  field names that appears twice, wheter it's active or not
        List<String> duplicatedFields = metadatas.stream()
                .collect(Collectors.groupingBy(g -> g.getFieldName(), Collectors.counting()))
                .entrySet().stream()
                .filter(f -> f.getValue() > 1)
                .map(m -> m.getKey())
                .collect(Collectors.toList());

        if (duplicatedFields.size() > 0) {
            for (String fieldName : duplicatedFields) {
                int ordinalPosition = -1;
                for (int i = 0; i < metadatas.size(); i++) {
                    
                    Metadata metadata = metadatas.get(i);
                    
                    if (metadata.getFieldName().equals(fieldName)) {
                        if(ordinalPosition == -1) {
                            ordinalPosition = metadata.getOrdinalPosition();
                            metadatas.remove(i);
                        } else {
                            metadata.setOrdinalPosition(ordinalPosition);
                        }
                    }
                }
            }
        }

        loopMetadata:
        for (int i = 0; i < metadatas.size(); i++) {

            Metadata metadata = metadatas.get(i);
            DataType dataType = metadata.getDataType();
            Conditional condition = metadata.getTest();
            String threshold = metadata.getThreshold();

            if (condition == Conditional.NONE) {
                continue;
            }

            if ((dataType == DataType.INTEGER || dataType == DataType.DECIMAL)
                    && (condition != Conditional.IN || condition != Conditional.NOT_IN)) {
                try {
                    Double.parseDouble(threshold);
                } catch (Exception e) {
                    return false;
                }
            }

            // validate format in case of data comparison
            if (dataType == DataType.DATE || dataType == DataType.DATE_AND_TIME) {

                String regex = "";

                switch (dataType) {
                    case DATE:
                        regex = "^[0-9]{4}-[0-9]{2}-[0-9]{1,2}$";
                        break;
                    case DATE_AND_TIME:
                        regex = "^[0-9]{4}-[0-9]{2}-[0-9]{1,2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";
                        break;
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(threshold);

                if (!matcher.find()) {
                    return false;
                }

            }
        }

        return true;
    }

    /**
     * validate metadata and return a text to explain the result
     *
     * @param metadatas
     * @return
     */
    public String evaluateValidationMessagem(List<Metadata> metadatas) {
        
        List<String> activeFieldsDuplicated = metadatas.stream()
                .filter(f -> f.getIsActive())
                .collect(Collectors.groupingBy(g -> g.getFieldName(), Collectors.counting()))
                .entrySet().stream()
                .filter(f -> f.getValue() > 1)
                .map(m -> m.getKey())
                .collect(Collectors.toList());

        if (activeFieldsDuplicated.size() > 0) {
            return "You can not have 2 or more fields with the same name!";
        }

        loopMetadata:
        for (int i = 0; i < metadatas.size(); i++) {

            Metadata metadata = metadatas.get(i);
            String field = metadata.getFieldName();
            DataType dataType = metadata.getDataType();
            Conditional condition = metadata.getTest();
            String threshold = metadata.getThreshold();

            if (condition == Conditional.NONE) {
                continue;
            }

            if ((dataType == DataType.INTEGER || dataType == DataType.DECIMAL)
                    && (condition != Conditional.IN || condition != Conditional.NOT_IN)) {
                try {
                    Double.parseDouble(threshold);
                } catch (Exception e) {
                    return "Field " + field + ": The threshold is not a valid number, make sure you are using only numerical values and dot (.) instead comma (,) for floating numbers!";
                }
            }

            // validate format in case of data comparison
            if (dataType == DataType.DATE || dataType == DataType.DATE_AND_TIME) {

                String regex = "";

                switch (dataType) {
                    case DATE:
                        regex = "^[0-9]{4}-[0-9]{2}-[0-9]{1,2}$";
                        break;
                    case DATE_AND_TIME:
                        regex = "^[0-9]{4}-[0-9]{2}-[0-9]{1,2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";
                        break;
                }

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(threshold);

                if (!matcher.find()) {
                    return "Field " + field + ": The threshold does not match the date format pattern for validation! Read the \"How to set metadata?\" for more information.";
                }

            }
        }

        return "ok";
    }

    /**
     * remove inactive fields and re-order the fields in the list
     *
     * @param metadata
     */
    public void refreshMetadata(List<Metadata> metadata) {

        for (int i = 0; i < metadata.size(); i++) {
            Metadata mtdt = metadata.get(i);
            if (!mtdt.getIsActive()) {
                metadata.remove(i);
            }
        }

        for (int i = 0; i < metadata.size(); i++) {
            Metadata mtdt = metadata.get(i);
            mtdt.setOrdinalPosition(i + 1);
        }
    }

    /**
     * verify if there is any change of data type between metadata versions
     *
     * @param newMetadataList
     * @param oldMetadataList
     * @return
     */
    public Boolean hasDataTypeChange(List<Metadata> newMetadataList, List<Metadata> oldMetadataList) {

        newMetadataLoop:
        for (Metadata newMetadata : newMetadataList) {
            oldMetadataLoop:
            for (Metadata oldMetadata : oldMetadataList) {
                if (newMetadata.getFieldName().equals(oldMetadata.getFieldName())) {
                    if (!newMetadata.getDataType().equals(oldMetadata.getDataType())) {
                        return true;
                    }
                    continue oldMetadataLoop;
                }
            }
        }

        return false;
    }
}
