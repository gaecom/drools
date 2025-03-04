/*
 * Copyright 2005 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.reteoo;

import java.util.Map;

import org.drools.core.common.InternalFactHandle;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.Declaration;
import org.drools.core.rule.GroupElement;
import org.drools.core.definitions.rule.impl.QueryImpl;
import org.drools.core.common.PropagationContext;

/**
 * Leaf Rete-OO node responsible for enacting <code>Action</code> s on a
 * matched <code>Rule</code>.
 *
 * @see org.kie.api.definition.rule.Rule
 */
public class QueryTerminalNode extends AbstractTerminalNode implements LeftTupleSinkNode {

    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    private static final long serialVersionUID = 510l;

    public static final short type             = 8;

    /** The rule to invoke upon match. */
    protected QueryImpl query;
    private GroupElement      subrule;
    private int               subruleIndex;
    private Declaration[]     allDeclarations;
    private Declaration[]     requiredDeclarations;

    private LeftTupleSinkNode previousTupleSinkNode;
    private LeftTupleSinkNode nextTupleSinkNode;
    
    private transient ObjectTypeNode.Id leftInputOtnId;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public QueryTerminalNode() {
    }

    /**
     * Constructor
     *
     * @param id node ID
     * @param source the tuple source for this node
     * @param rule the rule this node belongs to
     * @param subrule the subrule this node belongs to
     * @param context the current build context
     */
    public QueryTerminalNode(final int id,
                             final LeftTupleSource source,
                             final RuleImpl rule,
                             final GroupElement subrule,
                             final int subruleIndex,                              
                             final BuildContext context) {
        super( id,
               context.getPartitionId(),
               context.getRuleBase().getConfiguration().isMultithreadEvaluation(),
               source,
               context);
        this.query = (QueryImpl) rule;
        this.subrule = subrule;
        this.subruleIndex = subruleIndex;
        
        initDeclaredMask(context);        
        initInferredMask();
        initDeclarations();

        hashcode = calculateHashCode();
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    public QueryImpl getQuery() {
        return query;
    }

    public RuleImpl getRule() {
        return this.query;
    }

    private int calculateHashCode() {
        return this.query.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if ( object == null || !(object instanceof QueryTerminalNode) || this.hashCode() != object.hashCode() ) {
            return false;
        }
        return query.equals(((QueryTerminalNode) object).query);
    }

    public String toString() {
        return "[QueryTerminalNode(" + this.getId() + "): query=" + this.query.getName() + "]";
    }

    /**
     * @return the subrule
     */
    public GroupElement getSubRule() {
        return this.subrule;
    }
    
    @Override
    public boolean isFireDirect() {
        return false;
    }

    public Declaration[] getAllDeclarations() {
        return this.allDeclarations;
    }

    public Declaration[] getRequiredDeclarations() {
        return this.requiredDeclarations;
    }

    private void initDeclarations() {
        Map<String, Declaration> declMap = subrule.getOuterDeclarations();
        this.allDeclarations = declMap.values().toArray( new Declaration[declMap.size()] );

        this.requiredDeclarations = new Declaration[ query.getParameters().length ];
        int i = 0;
        for ( Declaration declr : query.getParameters() ) {
            this.requiredDeclarations[i++] =  declMap.get( declr.getIdentifier() );
        }
    }
    
    public int getSubruleIndex() {
        return this.subruleIndex;
    }

    /**
     * Returns the previous node
     * @return
     *      The previous TupleSinkNode
     */
    public LeftTupleSinkNode getPreviousLeftTupleSinkNode() {
        return this.previousTupleSinkNode;
    }

    /**
     * Sets the previous node
     * @param previous
     *      The previous TupleSinkNode
     */
    public void setPreviousLeftTupleSinkNode(final LeftTupleSinkNode previous) {
        this.previousTupleSinkNode = previous;
    }

    /**
     * Returns the next node
     * @return
     *      The next TupleSinkNode
     */
    public LeftTupleSinkNode getNextLeftTupleSinkNode() {
        return this.nextTupleSinkNode;
    }

    /**
     * Sets the next node
     * @param next
     *      The next TupleSinkNode
     */
    public void setNextLeftTupleSinkNode(final LeftTupleSinkNode next) {
        this.nextTupleSinkNode = next;
    }

    public short getType() {
        return NodeTypeEnums.QueryTerminalNode;
    }

    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     boolean leftTupleMemoryEnabled) {
        return AgendaComponentFactory.get().createTerminalTuple(factHandle, this, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(final InternalFactHandle factHandle,
                                     final LeftTuple leftTuple,
                                     final Sink sink) {
        return AgendaComponentFactory.get().createTerminalTuple(factHandle,leftTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     Sink sink,
                                     PropagationContext pctx,
                                     boolean leftTupleMemoryEnabled) {
        return AgendaComponentFactory.get().createTerminalTuple(leftTuple,sink, pctx, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     Sink sink) {
        return AgendaComponentFactory.get().createTerminalTuple(leftTuple, rightTuple, sink );
    }   
    
    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     Sink sink,
                                     boolean leftTupleMemoryEnabled) {
        return AgendaComponentFactory.get().createTerminalTuple(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled );
    }    
    
    
    public ObjectTypeNode.Id getLeftInputOtnId() {
        return leftInputOtnId;
    }

    public void setLeftInputOtnId(ObjectTypeNode.Id leftInputOtnId) {
        this.leftInputOtnId = leftInputOtnId;
    }

    public LeftTuple createPeer(LeftTuple original) {
        return null;
    }

    @Override
    public Declaration[] getSalienceDeclarations() {
        throw new UnsupportedOperationException();
    }

    public void doAttach( BuildContext context ) {
        super.doAttach(context);
        getLeftTupleSource().addTupleSink( this, context );
        addAssociation( context, context.getRule() );
    }
}
