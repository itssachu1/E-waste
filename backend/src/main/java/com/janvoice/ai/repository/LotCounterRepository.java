package com.janvoice.ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LotCounterRepository {
    private final JdbcTemplate jdbc;
    public LotCounterRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public int nextNumber(int year) {
        try {
            jdbc.update("INSERT INTO lot_year_counters (year_value, next_number) VALUES (?, 1)", year);
            return 1;
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            jdbc.update("UPDATE lot_year_counters SET next_number = next_number + 1 WHERE year_value = ?", year);
            return jdbc.queryForObject("SELECT next_number FROM lot_year_counters WHERE year_value = ? FOR UPDATE", Integer.class, year);
        }
    }
}
