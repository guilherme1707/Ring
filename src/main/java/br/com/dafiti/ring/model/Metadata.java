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

import br.com.dafiti.ring.option.Conditional;
import br.com.dafiti.ring.option.DataType;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;


/**
 *
 * @author guilherme.almeida
 */
@Entity
public class Metadata extends Tracker implements Serializable {
    
    private Long id;
    private String fieldName;
    private DataType dataType;
    private Conditional test;
    private String threshold;
    private Integer ordinalPosition;
    private Boolean isBusinessKey;
    private Boolean isActive;
    private ManualInput manualInput;
    private Boolean pending;
    
    public Metadata() {
        this.isActive = true;
        this.pending = false;
        this.isBusinessKey = false;
    }
    
    public Metadata(int ordinalPosition) {
        this();
        this.pending = true;
        this.ordinalPosition = ordinalPosition;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_metadata")
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFieldName() {
        return this.fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    @Enumerated(EnumType.STRING)
    public DataType getDataType() {
        return this.dataType;
    }
    
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }
    
    @Enumerated(EnumType.STRING)
    public Conditional getTest() {
        return this.test;
    }
    
    public void setTest(Conditional test) {
        this.test = test;
    }
    
    public String getThreshold() {
        return this.threshold;
    }
    
    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }
    
    public Integer getOrdinalPosition() {
        return this.ordinalPosition;
    }
    
    public void setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }
    
    public Boolean getIsBusinessKey() {
        return this.isBusinessKey;
    }
    
    public void setIsBusinessKey(Boolean isBusinessKey) {
        this.isBusinessKey = isBusinessKey;
    }
    
    
    public Boolean getIsActive() {
        return this.isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    
    @ManyToOne
    @JoinColumn(name = "fk_manual_input", referencedColumnName = "id_manual_input")
    public ManualInput getManualInput() {
        return this.manualInput;
    }
    
    public void setManualInput(ManualInput manualInput) {
        this.manualInput = manualInput;
    }
    
    @Transient
    public Boolean getPending() {
        return this.pending;
    }
    
    public void setPending(Boolean pending) {
        this.pending = pending;
    }
}
