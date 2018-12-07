package org.meveo.neo4j.scheduler;

import org.meveo.api.dto.neo4j.EntityOrRelation;

import java.util.Arrays;
import java.util.Collection;

public class CyclicDependencyException extends Exception {


    public CyclicDependencyException(Collection<EntityOrRelation> entityOrRelations) {
        super("There is probably a cyclic dependency between " + Arrays.toString(entityOrRelations.toArray()));
    }
}
