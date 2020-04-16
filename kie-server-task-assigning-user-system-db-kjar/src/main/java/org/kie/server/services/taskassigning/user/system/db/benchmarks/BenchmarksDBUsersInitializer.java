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

package org.kie.server.services.taskassigning.user.system.db.benchmarks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.kie.server.services.taskassigning.user.system.db.DBUserSystemServiceException;
import org.kie.server.services.taskassigning.user.system.db.DBUsersInitializer;

public class BenchmarksDBUsersInitializer implements DBUsersInitializer {

    private static String INSERT_USER_QUERY = "insert into ta_user values (?, ?, ?)";
    private static String INSERT_USER_GROUP_QUERY = "insert into ta_user_group values (?, ?)";
    private static String DELETE_USERS_QUERY = "delete from ta_user where userid like ?";
    private static String DELETE_USERS_GROUPS_QUERY = "delete from ta_user_group where userid like ?";
    private static String DELETE_USERS_SKILLS_QUERY = "delete from ta_user_skill where userid like ?";

    private static final String HR_USER_PREFIX = "HR-user";
    private static final String IT_USER_PREFIX = "IT-user";
    private static final String ENG_USER_PREFIX = "ENG-user";
    private static final List<String> USER_PREFIXES = Arrays.asList(HR_USER_PREFIX, IT_USER_PREFIX, ENG_USER_PREFIX);

    public static final String USERS_SET_SIZE = "org.kie.server.services.taskassigning.user.system.db.benchmarks.BenchmarksDBUsersInitializer.usersSetSize";

    @Override
    public String getName() {
        return "BenchmarksDBUsersInitializer";
    }

    @Override
    public void initializeUsers(DataSource dataSource) {
        int usersSetSize;
        try {
            usersSetSize = Integer.parseInt(System.getProperty(USERS_SET_SIZE, "0"));
        } catch (NumberFormatException e) {
            throw new DBUserSystemServiceException("usersSetSize wasn't properly set: " + e.getMessage(), e);
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement insertUserStmt = connection.prepareStatement(INSERT_USER_QUERY);
             PreparedStatement insertGroupStmt = connection.prepareStatement(INSERT_USER_GROUP_QUERY);
             PreparedStatement deleteUsersStmt = connection.prepareStatement(DELETE_USERS_QUERY);
             PreparedStatement deleteUsersGroupsStmt = connection.prepareStatement(DELETE_USERS_GROUPS_QUERY);
             PreparedStatement deleteUsersSkillsStmt = connection.prepareStatement(DELETE_USERS_SKILLS_QUERY);
        ) {
            connection.setAutoCommit(false);
            for (String userPrefix : USER_PREFIXES) {
                deleteUsersSkillsStmt.clearParameters();
                deleteUsersSkillsStmt.setString(1, userPrefix + "%");
                deleteUsersSkillsStmt.executeUpdate();

                deleteUsersGroupsStmt.clearParameters();
                deleteUsersGroupsStmt.setString(1, userPrefix + "%");
                deleteUsersGroupsStmt.executeUpdate();

                deleteUsersStmt.clearParameters();
                deleteUsersStmt.setString(1, userPrefix + "%");
                deleteUsersStmt.executeUpdate();
            }

            for (int i = 1; i < usersSetSize + 1; i++) {
                for (String userPrefix : USER_PREFIXES) {
                    String userId = userPrefix + i;
                    String userGroup;

                    insertUserStmt.clearParameters();
                    insertUserStmt.setString(1, userId);
                    insertUserStmt.setShort(2, (short) 1);
                    insertUserStmt.setString(3, userId + " Description");
                    insertUserStmt.executeUpdate();

                    insertGroupStmt.clearParameters();
                    insertGroupStmt.setString(1, userId);
                    insertGroupStmt.setString(2, "user");
                    insertGroupStmt.executeUpdate();

                    if (HR_USER_PREFIX.equals(userPrefix)) {
                        userGroup = "HR";
                    } else if (IT_USER_PREFIX.equals(userPrefix)) {
                        userGroup = "IT";
                    } else {
                        userGroup = "ENG";
                    }

                    insertGroupStmt.clearParameters();
                    insertGroupStmt.setString(1, userId);
                    insertGroupStmt.setString(2, userGroup);
                    insertGroupStmt.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new DBUserSystemServiceException(e.getMessage(), e);
        }
    }
}
