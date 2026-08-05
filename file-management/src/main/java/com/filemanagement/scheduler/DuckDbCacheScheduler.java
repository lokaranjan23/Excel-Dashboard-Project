package com.filemanagement.scheduler;

import com.filemanagement.repository.DuckDbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class DuckDbCacheScheduler {

    private final DuckDbRepository duckDbRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void clearCache() throws SQLException {

        duckDbRepository.clearWorkspace();
    }
}
