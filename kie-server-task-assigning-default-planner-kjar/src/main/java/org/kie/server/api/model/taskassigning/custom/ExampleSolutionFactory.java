package org.kie.server.api.model.taskassigning.custom;

import org.kie.server.services.taskassigning.core.model.SolutionFactory;
import org.kie.server.services.taskassigning.core.model.TaskAssigningSolution;

public class ExampleSolutionFactory implements SolutionFactory {

    public String getName() {
        return "ExampleSolutionFactory";
    }

    public TaskAssigningSolution<?> newSolution() {
        return new ExampleSolution();
    }
}
