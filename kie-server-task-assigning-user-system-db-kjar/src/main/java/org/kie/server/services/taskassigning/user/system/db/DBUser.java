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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.kie.server.services.taskassigning.user.system.api.Group;
import org.kie.server.services.taskassigning.user.system.api.User;

public class DBUser implements User {

    private String id;

    private Set<Group> groups;

    private Set<String> skills;

    public DBUser(String id, Set<Group> groups, Set<String> skills) {
        this.id = id;
        this.groups = groups;
        this.skills = skills;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<Group> getGroups() {
        return groups;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    public Set<String> getSkills() {
        return skills;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DBUser)) {
            return false;
        }
        DBUser dbUser = (DBUser) o;
        return Objects.equals(id, dbUser.id) &&
                Objects.equals(groups, dbUser.groups) &&
                Objects.equals(skills, dbUser.skills);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groups, skills);
    }
}
