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
package br.com.dafiti.ring.model;

import br.com.dafiti.ring.option.FileType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 *
 * @author guilherme.almeida
 */
@Entity
public class ManualInput extends Tracker implements Serializable {

    private Long id;
    private String name;
    private FileType fileType;
    private Character delimiterChar;
    private Character quoteChar;
    private Character escapeChar;
    private String lineSeparator;
    private String sheetName;
    private String spreadsheetKey;
    private String spreadsheetRange;
    private List<Metadata> metadata;
    private Set<DivisionGroup> divisionGroups;
    private DivisionGroup originDivisionGroup;
    private boolean alterable;
    private String description;
    
    public ManualInput() {
        
    }

    public ManualInput(DivisionGroup divisionGroup, FileType fileType) {
        this.delimiterChar = ';';
        this.quoteChar = '"';
        this.escapeChar = '\\';
        this.lineSeparator = "\\n";
        this.sheetName = "Sheet1";
        this.alterable = true;
        
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata(1));
        metadataList.add(new Metadata(2));
        this.metadata = metadataList;
        
        this.originDivisionGroup = divisionGroup;
        this.divisionGroups = new HashSet<>(Arrays.asList(divisionGroup));
        this.fileType = fileType;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_manual_input")
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Enumerated(EnumType.STRING)
    public FileType getFileType() {
        return this.fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Character getDelimiterChar() {
        return delimiterChar;
    }

    public void setDelimiterChar(Character delimiterChar) {
        this.delimiterChar = delimiterChar;
    }

    public Character getQuoteChar() {
        return this.quoteChar;
    }

    public void setQuoteChar(Character quoteChar) {
        this.quoteChar = quoteChar;
    }

    public Character getEscapeChar() {
        return escapeChar;
    }

    public void setEscapeChar(Character escapeChar) {
        this.escapeChar = escapeChar;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }
    
    public String getSpreadsheetKey() {
        return spreadsheetKey;
    }

    public void setSpreadsheetKey(String spreadsheetKey) {
        this.spreadsheetKey = spreadsheetKey;
    }

    public String getSpreadsheetRange() {
        return spreadsheetRange;
    }

    public void setSpreadsheetRange(String spreadsheetRange) {
        if(spreadsheetRange != null) {
            this.spreadsheetRange = spreadsheetRange.toUpperCase();
        } else {
            this.spreadsheetRange = spreadsheetRange;
        }
    }
    
    public boolean getAlterable() {
        return this.alterable;
    }
    
    public void setAlterable(boolean alterable) {
        this.alterable = alterable;
    }
    
    @Column(columnDefinition = "text")
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    @OneToMany(mappedBy = "manualInput", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    public List<Metadata> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(List<Metadata> metadata) {
        this.metadata = metadata;
    }
    
    @ManyToMany
    @JoinTable(name = "manual_input_user_division_group",
            joinColumns = @JoinColumn(name = "fk_manual_input"),
            inverseJoinColumns = @JoinColumn(name = "fk_division_group"))
    public Set<DivisionGroup> getDivisionGroups() {
        return this.divisionGroups;
    }
    
    public void setDivisionGroups(Set<DivisionGroup> divisionGroups) {
        this.divisionGroups = divisionGroups;
    }
    
    @ManyToOne
    @JoinColumn(name = "fk_origin_division_group", referencedColumnName = "id_division_group")
    public DivisionGroup getOriginDivisionGroup() {
        return this.originDivisionGroup;
    }
    
    public void setOriginDivisionGroup(DivisionGroup originDivisionGroup) {
        this.originDivisionGroup = originDivisionGroup;
    }

    @Transient
    public String getInfo() {
        StringBuilder info = new StringBuilder();
        info.append("NAME = ").append(this.name);
        info.append("\nmetadata:[");
        boolean addComma = false;
        for (Metadata metadata : this.metadata) {
            if (metadata.getIsActive()) {
                if(addComma) {
                    info.append(",");
                }
                info.append("\n{")
                        .append("  field: \"").append(metadata.getFieldName()).append("\"")
                        .append(", datatype: \"").append(metadata.getDataType()).append("\"")
                        .append(", test: \"").append(metadata.getTest()).append("\"")
                        .append(", threasold: \"").append(metadata.getThreshold()).append("\"")
                        .append(", ordinal_position: ").append(metadata.getOrdinalPosition())
                        .append("}");
                addComma = true;
            }
        }
        info.append("\n]");
        return info.toString();
    }
}
