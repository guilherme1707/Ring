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

import br.com.dafiti.ring.model.FileHandler;
import br.com.dafiti.ring.model.ImportLog;
import br.com.dafiti.ring.model.ManualInput;
import br.com.dafiti.ring.option.ImportLogStatus;
import br.com.dafiti.ring.repository.ManualInputRepository;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class ManualInputService {

    private final ManualInputRepository manualInputRepository;
    private final FileHandlerService fileHandlerService;
    private final StorageManagerService storageManagerService;
    private final ImportLogService importLogService;

    @Autowired
    public ManualInputService(ManualInputRepository manualInputRepository,
            FileHandlerService fileHandlerService,
            StorageManagerService storageManagerService,
            ImportLogService importLogService) {
        this.manualInputRepository = manualInputRepository;
        this.fileHandlerService = fileHandlerService;
        this.importLogService = importLogService;
        this.storageManagerService = storageManagerService;
    }

    /**
     * saves a manual input
     *
     * @param manualInput
     * @return
     */
    public ManualInput save(ManualInput manualInput) {

        manualInput.getMetadata()
                .stream()
                .forEach(metadata -> {
                    metadata.setManualInput(manualInput);
                });

        return manualInputRepository.save(manualInput);
    }

    /**
     * return a manual input filtering by name
     *
     * @param name
     * @return
     */
    public ManualInput FindByName(String name) {
        return manualInputRepository.findByName(name);    }

    /**
     *
     * @return
     */
    public Iterable<ManualInput> findAll() {
        return manualInputRepository.findAll();
    }

    /**
     * delete a manual input
     *
     * @param manualInput
     */
    public void delete(ManualInput manualInput) {
        manualInputRepository.delete(manualInput);
    }

    /**
     * return a manual input filtering by id
     *
     * @param id
     * @return
     */
    public ManualInput findById(Long id) {
        return manualInputRepository.findById(id).get();
    }

    public List<ManualInput> findByNameContaining(String search) {
        return manualInputRepository.findByNameContaining(search);
    }

    /**
     * process the file to get the content and insert into mongoDB collection
     * and MySql table
     *
     * @param manualInput
     * @param inputStream
     * @param log
     */
    @Async("ringTaskExecutor")
    public void process(ManualInput manualInput, InputStream inputStream, ImportLog log) {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime logCreatedAt = LocalDateTime.parse(log.getCreatedAt().toString().subSequence(0, 19), dateTimeFormatter);//now();
        String loadDate = dateTimeFormatter.format(logCreatedAt);

        try {
            FileHandler file = fileHandlerService.getFile(manualInput, inputStream, log);
            if (log.getFinalized()) {
                return;
            }
            importLogService.updateLogText(log,
                    ImportLogStatus.RUNNING,
                    false,
                    file.getData().size() + " rows processed.."
                    + "\n Starting storage Module to save processed file..");
            storageManagerService.saveFile(manualInput, fileHandlerService, loadDate, file);
            if (log.getFinalized()) {
                return;
            }

            importLogService.updateLogText(log,
                    ImportLogStatus.SUCCESS,
                    true,
                    "PROCESS ENDED!");
        } catch (Exception e) {
            Logger.getLogger(ManualInputService.class.getName()).log(Level.SEVERE, "Fail processing file!", e);
            importLogService.updateLogText(log,
                    ImportLogStatus.ERROR,
                    true,
                    "ERROR:" + e.toString() + "\n"
                    + Arrays.asList(e.getStackTrace()).stream().map(m -> m.getClassName() + " -> method: " + m.getMethodName() + " -> line number: " + m.getLineNumber() + "\n").collect(Collectors.toList()).toString()
                    + "\nPROCESS ENDED!");
        }

    }

}
