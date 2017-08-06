package com.cuba.real.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cuba.real.dto.PropertiesDto;
import com.cuba.real.dto.SearchDto;
import com.cuba.real.model.Property;
import com.cuba.real.model.User;
import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Strings;

@Component
public class UserDao {

    private static final String SELECT_ALL_QUERY = "SELECT * FROM cucuyo.props";

    private static final String SEARCH_BASE_QUERY = "SELECT * FROM cucuyo.props where expr(props_index, '{filter:[ %s]}')";

    private static final String SELECT_SEARCH_DESCRIPTION_QUERY = "{type:\"wildcard\", field:\"description\", value:\"*%s*\"}";

    private static final String SELECT_FROM_TO_PRICE_QUERY = "{type:\"range\", field:\"price\", lower:\"%s\", upper:\"%s\"}";

    @Autowired
    CassandraSession cSession;

    public User createUser(User user) {
        getMapper().save(user);
        return user;
    }

    private String escapeQuery(String in) {
        String regex = "([+\\-!\\(\\){}\\[\\]^\"~*?:\\\\]|[&\\|]{2})";
        return in.replaceAll(regex, "\\\\$1").toLowerCase();
    }

    private String buildQuery(SearchDto searchDto) {

        String search = "";

        String searchQuery = searchDto.getSearch();
        searchQuery = (searchQuery != null) ? escapeQuery(searchQuery) : null;
        int from = searchDto.getFrom();
        int to = searchDto.getTo();

        if (Strings.isNullOrEmpty(searchQuery) && from == 0 && to == 0) {
            search = SELECT_ALL_QUERY;
        } else if (Strings.isNullOrEmpty(searchQuery) && (from != 0 || to != 0)) {
            search = String.format(SEARCH_BASE_QUERY, String.format(SELECT_FROM_TO_PRICE_QUERY, from, to));
        } else if (!Strings.isNullOrEmpty(searchQuery) && from == 0 && to == 0) {
            String wildcardAndRange = String.format(SELECT_SEARCH_DESCRIPTION_QUERY, searchQuery);
            search = String.format(SEARCH_BASE_QUERY, wildcardAndRange);
        } else {
            String wildcardAndRange = String.format(SELECT_SEARCH_DESCRIPTION_QUERY, searchQuery) + ","
                    + String.format(SELECT_FROM_TO_PRICE_QUERY, from, to);
            search = String.format(SEARCH_BASE_QUERY, wildcardAndRange);
        }

        return search;
    }

    private Mapper<User> getMapper() {
        MappingManager manager = new MappingManager(cSession.getSession());
        Mapper<User> mapper = manager.mapper(User.class);
        return mapper;
    }
}
