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

import br.com.dafiti.ring.model.DivisionGroup;
import br.com.dafiti.ring.repository.DivisionGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author guilherme.almeida
 */
@Service
public class DivisionGroupService {
    
    private final DivisionGroupRepository divisionGroupRepository;
    
    @Autowired
    public DivisionGroupService(DivisionGroupRepository divisionGroupRepository) {
        this.divisionGroupRepository = divisionGroupRepository;
    }
    
    public DivisionGroup save(DivisionGroup divisionGroup) {
        return this.divisionGroupRepository.save(divisionGroup);
    }
    
    public DivisionGroup findByName(String name) {
        return this.divisionGroupRepository.findByName(name);
    }
    
    public DivisionGroup findById(Long id) {
        return this.divisionGroupRepository.findById(id).get();
    }
    
    public DivisionGroup createIfNotExists(String name) {
        DivisionGroup divisionGroup = this.findByName(name);

        if (divisionGroup == null) {
            DivisionGroup newDivisionGroup = new DivisionGroup();
            newDivisionGroup.setName(name);
            divisionGroup = this.save(newDivisionGroup);
        }

        return divisionGroup;
    }
    
    public Iterable<DivisionGroup> findAll() {
        return divisionGroupRepository.findAll();
    }
}
