package com.bettermountebank.infrastructure.service;

import com.bettermountebank.domain.service.CounterService;
import com.bettermountebank.model.Counter;
import com.bettermountebank.repository.CounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CounterServiceImpl implements CounterService {
    private final MongoOperations mongoOperations;
    private final CounterRepository counterRepository;

    @Override
    public String generateMockPrefix(String counterName) {
        Query query = new Query(Criteria.where("id").is(counterName));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

        Counter counter = mongoOperations.findAndModify(query, update, options, Counter.class);
        if (counter == null) {
            log.error("Failed to generate sequence for counter: {}", counterName);
            throw new RuntimeException("Failed to generate sequence");
        }

        return String.format("BMB_MOCK_%06d", counter.getSeq());
    }
}
