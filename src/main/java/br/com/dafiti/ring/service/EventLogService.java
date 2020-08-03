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

import br.com.dafiti.ring.model.EventLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import br.com.dafiti.ring.repository.EventLogRepository;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Valdiney V GOMES
 */
@Service
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    @Autowired
    public EventLogService(EventLogRepository eventLogRepository) {
        this.eventLogRepository = eventLogRepository;
    }

    public Iterable<EventLog> list() {
        return eventLogRepository.findAll();
    }

    public Iterable<EventLog> listOrderByDateDesc() {
        return eventLogRepository.findAllByOrderByDateDesc();
    }

    public EventLog load(Long id) {
        return eventLogRepository.findById(id).get();
    }

    public void save(EventLog logger) {
        eventLogRepository.save(logger);
    }

    public void delete(Long id) {
        eventLogRepository.deleteById(id);
    }

    public void cleaneup(Date expiration) {
        eventLogRepository.deleteByDateBefore(expiration);
    }
    
    public List<EventLog> listDateBetween(Date dateFrom, Date dateTo) {
        return eventLogRepository.findByDateBetweenOrderByDateDesc(dateFrom, dateTo);
    }    
}
