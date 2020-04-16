/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.services.taskassigning.user.system.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.kie.server.services.taskassigning.user.system.api.User;
import org.kie.server.services.taskassigning.user.system.api.UserSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example implementation of a database based UserSystemService.
 * The purpose of this implementation is purely for showing other user system service implementation alternatives
 * and is not intended for production purposes. (relies on core java.sql apis intentionally)
 * <p>
 * Note: User system services must be declared in the resource
 * src/main/resources/META-INF/services/org.kie.server.services.taskassigning.user.system.api.UserSystemService
 */
public class DBUserSystemService implements UserSystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUserSystemService.class);

    private static final String NAME = "DBUserSystemService";

    /**
     * System property for configuring the application server provided datasource to use. Must be a valid JNDI name.
     */
    public static final String DATA_SOURCE_PROPERTY = "org.kie.server.services.taskassigning.user.system.DBUserSystemService.ds";

    /**
     * System property for configuring the database users initializer if any.
     */
    public static final String USERS_INITIALIZER_PROPERTY = "org.kie.server.services.taskassigning.user.system.DBUsersInitializer.name";

    private DataSource dataSource;

    private static final String FIND_ALL_USERS_QUERY = "select u.userid, u.enabled, g.groupid, s.skillid from ta_user u left join ta_user_group g on (u.userid = g.userid) left join ta_user_skill s on (u.userid = s.userid) where u.enabled = ?";

    private static final String FIND_USER_QUERY = "select u.userid, u.enabled, g.groupid, s.skillid from ta_user u left join ta_user_group g on (u.userid = g.userid) left join ta_user_skill s on (u.userid = s.userid) where u.userid = ? and u.enabled = ?";

    public String getName() {
        return NAME;
    }

    public void start() {
        String dataSourceName = System.getProperty(DATA_SOURCE_PROPERTY, "java:jboss/datasources/ExampleDS");
        try {
            InitialContext initialContext = new InitialContext();
            dataSource = (DataSource) initialContext.lookup(dataSourceName);
        } catch (NamingException e) {
            throw new DBUserSystemServiceException("Unable to find data source under name " + dataSourceName, e);
        }
        try {
            initializeUsers();
        } catch (Exception e) {
            throw new DBUserSystemServiceException("An error was produced during DBUsers initialization", e);
        }
    }

    public void test() throws Exception {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(FIND_ALL_USERS_QUERY)) {
            stmt.setShort(1, (short) 1);
            stmt.executeQuery();
        }
    }

    public List<User> findAllUsers() {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(FIND_ALL_USERS_QUERY)) {
            final List<User> result = new ArrayList<>();
            stmt.setShort(1, (short) 1);
            final ResultSet rs = stmt.executeQuery();
            String userId;
            String groupId;
            String skillId;
            final Map<String, DBUser> usersMap = new HashMap<>();
            while (rs.next()) {
                userId = getTrimmedStringValue(rs, 1);
                groupId = getTrimmedStringValue(rs, 3);
                skillId = getTrimmedStringValue(rs, 4);
                if (userId != null && !userId.isEmpty()) {
                    DBUser user = usersMap.get(userId);
                    if (user == null) {
                        user = new DBUser(userId, new HashSet<>(), new HashSet<>());
                        usersMap.put(userId, user);
                        result.add(user);
                    }
                    addGroupIfNotEmpty(user, groupId);
                    addSkillIfNotEmpty(user, skillId);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DBUserSystemServiceException("An error was produced while finding all users: " + e.getMessage(), e);
        }
    }

    public User findUser(String userId) {
        if (userId == null) {
            return null;
        }
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(FIND_USER_QUERY)) {
            stmt.setString(1, userId);
            stmt.setShort(2, (short) 1);
            final ResultSet rs = stmt.executeQuery();
            String groupId;
            String skillId;
            DBUser result = null;
            while (rs.next()) {
                result = new DBUser(userId, new HashSet<>(), new HashSet<>());
                groupId = getTrimmedStringValue(rs, 3);
                skillId = getTrimmedStringValue(rs, 4);
                addGroupIfNotEmpty(result, groupId);
                addSkillIfNotEmpty(result, skillId);
            }
            return result;
        } catch (SQLException e) {
            throw new DBUserSystemServiceException("An error was produced while finding all users: " + e.getMessage(), e);
        }
    }

    private void initializeUsers() {
        final String usersInitializer = System.getProperty(USERS_INITIALIZER_PROPERTY);
        if (usersInitializer == null || usersInitializer.isEmpty()) {
            LOGGER.info("No DBUsersInitializer has been configured");
            return;
        }
        final ServiceLoader<DBUsersInitializer> availableInitializers = ServiceLoader.load(DBUsersInitializer.class, getClass().getClassLoader());
        DBUsersInitializer initializer = null;
        for (DBUsersInitializer availableInitializer : availableInitializers) {
            if (usersInitializer.equals(availableInitializer.getName())) {
                initializer = availableInitializer;
                break;
            }
        }
        if (initializer == null) {
            LOGGER.info("DBUserInitializer: {} was not found in current classpath.", usersInitializer);
            return;
        }
        initializer.initializeUsers(dataSource);
    }

    private static void addGroupIfNotEmpty(DBUser user, String groupId) {
        if (groupId != null && !groupId.isEmpty()) {
            user.getGroups().add(new DBGroup(groupId));
        }
    }

    private static void addSkillIfNotEmpty(DBUser user, String skillId) {
        if (skillId != null && !skillId.isEmpty()) {
            user.getSkills().add(skillId);
        }
    }

    private static String getTrimmedStringValue(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? value.trim() : null;
    }
}
