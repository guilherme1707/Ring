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
import br.com.dafiti.ring.rest.ApiFilterDTO;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author guilherme.almeida
 */
public abstract class StorageAbstractionTemplate {

    public String tmpFilePath = System.getProperty("user.home") + "/tmp_files/ring/";

    public abstract void createOrUpdateManualInput(ManualInput manualInput, boolean recreate);

    public abstract void saveFile(ManualInput manualInput, JSONDocument JSONDoc, String LoadDateForPartition, FileHandler fileHandler) throws Exception;

    public abstract void deleteManualInput(ManualInput manualInput);

    public abstract File extractCSV(ManualInput manualInput, ApiFilterDTO filter) throws IOException, Exception;
}
