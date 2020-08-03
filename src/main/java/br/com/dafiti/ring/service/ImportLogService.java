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

import br.com.dafiti.ring.model.ImportLog;
import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.option.ImportLogStatus;
import br.com.dafiti.ring.repository.ImportLogRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class ImportLogService {
    
    private final ImportLogRepository importLogRepository;
    
    @Autowired
    public ImportLogService(ImportLogRepository importLogRepository) {
        this.importLogRepository = importLogRepository;
    }
    
    public ImportLog save(ImportLog importLog) {
        return this.importLogRepository.save(importLog);
    }
    
    public List<ImportLog> findByManualInputOrderByCreatedAtDesc(ManualInput manualInput, Pageable pageable) {
        return this.importLogRepository.findByManualInputOrderByCreatedAtDesc(manualInput, pageable);
    }
    
    public ImportLog updateLogText(ImportLog importLog, ImportLogStatus status, boolean finilized, String textToAppend) {
        
        if(status != null) {
            importLog.setStatus(status);
        }
        importLog.setFinalized(finilized);
        
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String log_date_time = dateTimeFormatter.format(now);
        
        String currentLog = importLog.getText();
        String newLog = currentLog + "\n"
                + log_date_time + " - "
                + textToAppend.replace("\n", "\n" + log_date_time + " - ");
        
        importLog.setText(newLog);
        
        return this.save(importLog);
    }
}
