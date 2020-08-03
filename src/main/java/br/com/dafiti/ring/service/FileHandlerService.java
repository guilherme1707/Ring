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

import br.com.dafiti.ring.mask.JSONDocument;
import br.com.dafiti.ring.model.FileHandler;
import br.com.dafiti.ring.model.ImportLog;
import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.option.ImportLogStatus;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class FileHandlerService extends JSONDocument {

    private final ImportLogService importLogService;
    private static final String APPLICATION_NAME = "Ring Google Sheets API";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.ring/gsheets/tokens";
    /**
     * Global instance of the scopes required by this quickstart. If modifying
     * these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = System.getProperty("user.home") + "/.ring/gsheets/credentials.json";

    @Autowired
    public FileHandlerService(ImportLogService importLogService,
            @Value("${ring.json.file.type}") String jsonType,
            @Value("${ring.enable.native.data.validation}") boolean useNativeValidation) {
        super(jsonType, useNativeValidation);
        this.importLogService = importLogService;
    }

    public FileHandler getFile(ManualInput manualInput, InputStream inputStream, ImportLog log) throws Exception {

        try {
            switch (manualInput.getFileType()) {
                case CSV:
                    return parseCSVFile(manualInput, inputStream, log);
                case XLSX:
                    return parseXLSXFile(manualInput, inputStream, log);
                case GSHEETS:
                    return processSheets(manualInput, log);
                default:
                    return null;
            }
        } catch (Exception e) {
            if (inputStream != null) {
                inputStream.close();
            }
            e.printStackTrace();
            throw new Exception(e.toString());
        }
    }

    private FileHandler parseCSVFile(ManualInput manualInput, InputStream inputStream, ImportLog log) {

        String stringLogCSV = "Setting CSV reader..."
                + "\nQUOTE = " + manualInput.getQuoteChar()
                + "\nDELIMITER = " + manualInput.getDelimiterChar()
                + "\nESPACE = " + manualInput.getEscapeChar()
                + "\nLINE_SEPRATOR = " + manualInput.getLineSeparator()
                + "\nParsing CSV file..";

        importLogService.updateLogText(log,
                null,
                Boolean.FALSE,
                stringLogCSV);

        CsvParserSettings settings = new CsvParserSettings();
        settings.getFormat().setDelimiter(manualInput.getDelimiterChar());
        settings.getFormat().setQuote(manualInput.getQuoteChar());
        settings.getFormat().setQuoteEscape(manualInput.getEscapeChar());
        settings.getFormat().setLineSeparator(manualInput
                .getLineSeparator()
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\b", "\b"));

        CsvParser parser = new CsvParser(settings);
        try {
            List<String[]> csvData = parser.parseAll(inputStream);
            inputStream.close();
            return new FileHandler(csvData);
        } catch (Exception ex) {
            importLogService.updateLogText(log,
                    ImportLogStatus.ERROR,
                    Boolean.TRUE,
                    "ERROR:" + ex.toString());
            Logger.getLogger(FileHandlerService.class.getName()).log(Level.ALL, null, ex);
        }
        return new FileHandler();
    }

    private FileHandler parseXLSXFile(ManualInput manualInput, InputStream inputStream, ImportLog log) throws IOException {
        List<String[]> fileContent = new ArrayList<>();

        importLogService.updateLogText(log,
                ImportLogStatus.RUNNING,
                Boolean.FALSE,
                "Reading XLSX file in sheet " + manualInput.getSheetName() + "..");
        Workbook workbook = new XSSFWorkbook(inputStream);
        org.apache.poi.ss.usermodel.Sheet datatypeSheet = workbook.getSheet(manualInput.getSheetName());
        if(datatypeSheet == null) {
            importLogService.updateLogText(log,
                ImportLogStatus.ERROR,
                Boolean.TRUE,
                "Could not find sheet with name " + manualInput.getSheetName());
            return null;
        }
        Iterator<Row> iterator = datatypeSheet.iterator();

        while (iterator.hasNext()) {

            Row currentRow = iterator.next();
            int rowNum = currentRow.getRowNum();
            int minColIdx = currentRow.getFirstCellNum();
            int maxColIdx = currentRow.getLastCellNum();
            String[] targetRow = new String[maxColIdx - minColIdx];

            for (int i = minColIdx; i < maxColIdx; i++) {
                Cell cell = currentRow.getCell(i);
                if (cell == null || cell.getCellType() == null) {
                    continue;
                }
                
                switch (cell.getCellType()) {
                    case STRING:
                        targetRow[i] = cell.getStringCellValue();
                        break;
                    case NUMERIC:
                        targetRow[i] = Double.toString(cell.getNumericCellValue());
                        break;
                    case BOOLEAN:
                        targetRow[i] = Boolean.toString(cell.getBooleanCellValue());
                        break;
                    case FORMULA:
                        switch (cell.getCachedFormulaResultType()) {
                            case STRING:
                                targetRow[i] = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                targetRow[i] = Double.toString(cell.getNumericCellValue());
                                break;
                            case BOOLEAN:
                                targetRow[i] = Boolean.toString(cell.getBooleanCellValue());
                                break;
                            default:
                                try {
                                Date cellDate = cell.getDateCellValue();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                targetRow[i] = dateFormat.format(cellDate);
                            } catch (Exception e) {
                                importLogService.updateLogText(log,
                                        ImportLogStatus.RUNNING,
                                        Boolean.FALSE,
                                        "WARNING: Could not identify data type in position " + rowNum + ":" + (i + 1) + ". Setting value to null");
                            }
                            break;
                        }
                        break;
                    default:
                        try {
                        Date cellDate = cell.getDateCellValue();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        targetRow[i] = dateFormat.format(cellDate);
                    } catch (Exception e) {
                        importLogService.updateLogText(log,
                                ImportLogStatus.RUNNING,
                                Boolean.FALSE,
                                "WARNING: Could not identify data type in position " + rowNum + ":" + (i + 1) + ". Setting value to null");
                    }
                    break;
                }

            }

            fileContent.add(targetRow);
        }

        workbook.close();
        inputStream.close();
        FileHandler holder = new FileHandler();
        holder.setHeader(fileContent.remove(0));
        holder.setData(fileContent);
        return holder;
    }

    private FileHandler processSheets(ManualInput manualInput, ImportLog log) throws IOException, GeneralSecurityException {

        String stringLogSheets = "Reading Sheets..."
                + "\nKEY = " + manualInput.getSpreadsheetKey()
                + "\nSHEET NAME = " + manualInput.getSheetName();

        importLogService.updateLogText(log,
                null,
                Boolean.FALSE,
                stringLogSheets);

        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = manualInput.getSpreadsheetKey();
        final String range = manualInput.getSheetName() + (manualInput.getSpreadsheetRange().equals("") ? "" : "!" + manualInput.getSpreadsheetRange());
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();
        ArrayList<List<Object>> values = (ArrayList) response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
            importLogService.updateLogText(log,
                    ImportLogStatus.ERROR,
                    Boolean.TRUE,
                    "ERROR: No data found.");
            return null;
        } else {
            return new FileHandler(values);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));// FileHandlerService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

}
