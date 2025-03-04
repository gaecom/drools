/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel;

import org.drools.drl.parser.impl.Operator;
import org.drools.core.common.QuadroupleBetaConstraints;
import org.drools.core.rule.constraint.BetaNodeFieldConstraint;
import org.junit.Test;

public class QuadroupleBetaConstraintsTest extends BaseBetaConstraintsTest {

    public QuadroupleBetaConstraintsTest(boolean useLambdaConstraint) {
        this.useLambdaConstraint = useLambdaConstraint;
    }

    @Test
    public void testNoneIndxed() {
        BetaNodeFieldConstraint constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint[] constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4  };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
    }
    
    @Test
    public void testOneIndxed() {
        BetaNodeFieldConstraint constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint[] constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
    }

    @Test
    public void testTwoIndxed() {
        BetaNodeFieldConstraint constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint[] constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint1 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint1 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint4 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint3 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint3 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint4 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = ( BetaNodeFieldConstraint ) getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
    }

    @Test
    public void testThreeIndxed() {
        BetaNodeFieldConstraint constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        BetaNodeFieldConstraint[] constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
        
        constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.NOT_EQUAL.getOperator() );
        constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
    }
    
    @Test
    public void testFourIndxed() {
        BetaNodeFieldConstraint constraint0 = getCheeseTypeConstraint( "cheeseType0", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint1 = getCheeseTypeConstraint( "cheeseType1", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint3 = getCheeseTypeConstraint( "cheeseType2", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint constraint4 = getCheeseTypeConstraint( "cheeseType3", Operator.BuiltInOperator.EQUAL.getOperator() );
        BetaNodeFieldConstraint[] constraints = new BetaNodeFieldConstraint[] { constraint0, constraint1, constraint3, constraint4 };
        checkBetaConstraints( constraints, QuadroupleBetaConstraints.class );
    }

}
