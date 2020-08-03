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
package br.com.dafiti.ring.rest;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author guilherme.almeida
 */
public class ApiFilterDTO {

    private String manualInput;
    private String operator;
    private String loadDate;
    private String delimiter;
    private String quote;
    private String escape;
    private String lineSeparator;

    public String getManualInput() {
        return manualInput;
    }

    public void setManualInput(String manualInput) {
        this.manualInput = manualInput;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getLoadDate() {
        return loadDate;
    }

    public void setLoadDate(String loadDate) {
        this.loadDate = loadDate;
    }

    public Character getDelimiter() {
        return delimiter.toCharArray()[0];
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Character getQuote() {
        return quote.toCharArray()[0];
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public Character getEscape() {
        return escape.toCharArray()[0];
    }

    public void setEscape(String escape) {
        this.escape = escape;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public boolean ok() {

        if (this.manualInput == null || this.manualInput.isEmpty()) {
            return false;
        }

        List<String> operators = Arrays.asList("$eq", "$ne", "$lt", "$lte", "$gt", "$gte");

        if (this.operator == null || this.operator.isEmpty()) {
            this.operator = "gte";
        }

        if (!operators.contains("$" + this.operator)) {
            return false;
        }

        if (this.loadDate == null || this.loadDate.isEmpty()) {
            this.loadDate = "2000-01-01 00:00:00";
        }

        String regex = "^[0-9]{4}-[0-9]{2}-[0-9]{1,2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(loadDate);

        if (!matcher.find()) {
            return false;
        }
        
        if(this.delimiter == null) {
            this.delimiter = ";";
        }
        if(this.quote == null) {
            this.quote = "\"";
        }
        if(this.escape == null) {
            this.escape = "\\";
        }
        if(this.lineSeparator == null) {
            this.lineSeparator = "\n";
        }

        return true;
    }

    public String getFilter() {
        return "'{ \"load_date\": { \"$" + operator + "\" :\"" + loadDate + "\"}}'";
    }

}
