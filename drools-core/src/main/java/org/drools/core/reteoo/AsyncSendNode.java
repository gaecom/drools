/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.common.BetaConstraints;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.Memory;
import org.drools.core.common.MemoryFactory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.UpdateContext;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.AsyncSend;
import org.drools.core.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.core.rule.accessor.DataProvider;
import org.drools.core.common.PropagationContext;
import org.drools.core.util.AbstractBaseLinkedListNode;
import org.drools.core.util.index.TupleList;

public class AsyncSendNode<T extends AsyncSendNode.AsyncSendMemory> extends LeftTupleSource
    implements
    LeftTupleSinkNode,
    MemoryFactory<T> {
    private static final long          serialVersionUID = 510l;

    private String messageId;
    private DataProvider dataProvider;
    private AlphaNodeFieldConstraint[] alphaConstraints;
    private BetaConstraints betaConstraints;

    private LeftTupleSinkNode previousTupleSinkNode;
    private LeftTupleSinkNode nextTupleSinkNode;

    private AsyncSend send;

    private boolean tupleMemoryEnabled;

    private transient ObjectTypeConf objectTypeConf;

    public AsyncSendNode() {
    }

    public AsyncSendNode( final int id,
                          final DataProvider dataProvider,
                          final LeftTupleSource tupleSource,
                          final AlphaNodeFieldConstraint[] constraints,
                          final BetaConstraints binder,
                          final boolean tupleMemoryEnabled,
                          final BuildContext context,
                          final AsyncSend send) {
        super(id, context);
        this.dataProvider = dataProvider;
        setLeftTupleSource(tupleSource);
        this.setObjectCount(leftInput.getObjectCount()); // 'async send' node does not increase the object count
        this.alphaConstraints = constraints;
        this.betaConstraints = (binder == null) ? EmptyBetaConstraints.getInstance() : binder;
        this.betaConstraints.init(context, getType());
        this.tupleMemoryEnabled = tupleMemoryEnabled;
        this.send = send;
        this.messageId = send.getMessageId();

        initMasks(context, tupleSource);

        hashcode = calculateHashCode();
    }

    private int calculateHashCode() {
        int hash = ( 23 * leftInput.hashCode() ) + ( 29 * dataProvider.hashCode() );
        if (send.getResultPattern() != null) {
            hash += 31 * send.getResultPattern().hashCode();
        }
        if (alphaConstraints != null) {
            hash += 37 * Arrays.hashCode( alphaConstraints );
        }
        if (betaConstraints != null) {
            hash += 41 * betaConstraints.hashCode();
        }
        return hash;
    }

    public String getMessageId() {
        return messageId;
    }

    public String toString() {
        return "[AsyncSendNode(" + this.id + "): messageId=" + messageId + "]";
    }

    @Override
    public boolean equals( Object object ) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AsyncSendNode) || this.hashCode() != object.hashCode() ) {
            return false;
        }

        AsyncSendNode other = (AsyncSendNode ) object;

        return this.leftInput.getId() == other.leftInput.getId() &&
               dataProvider.equals( other.dataProvider ) &&
               Objects.equals(send.getResultPattern(), other.send.getResultPattern() ) &&
               Arrays.equals( alphaConstraints, other.alphaConstraints ) &&
               betaConstraints.equals( other.betaConstraints );
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public AlphaNodeFieldConstraint[] getAlphaConstraints() {
        return alphaConstraints;
    }

    public BetaConstraints getBetaConstraints() {
        return betaConstraints;
    }

    public Class<?> getResultClass() {
        return send.getResultClass();
    }

    public void networkUpdated(UpdateContext updateContext) {
        this.leftInput.networkUpdated(updateContext);
    }

    public InternalFactHandle createFactHandle(Tuple leftTuple, PropagationContext context, ReteEvaluator reteEvaluator, Object object ) {
        InternalFactHandle handle = null;
        if ( context.getReaderContext() != null ) {
            handle = context.getReaderContext().createAsyncNodeHandle( leftTuple, reteEvaluator, object, getId(), getObjectTypeConf( reteEvaluator ) );
        }

        if (handle == null) {
            handle = reteEvaluator.createFactHandle( object, getObjectTypeConf( reteEvaluator ), null );
        }
        return handle;
    }

    public ObjectTypeConf getObjectTypeConf( ReteEvaluator reteEvaluator ) {
        if ( objectTypeConf == null ) {
            // use default entry point and object class. Notice that at this point object is assignable to resultClass
            objectTypeConf = new ClassObjectTypeConf( reteEvaluator.getDefaultEntryPointId(), getResultClass(), reteEvaluator.getKnowledgeBase() );
        }
        return objectTypeConf;
    }

    public T createMemory(final RuleBaseConfiguration config, ReteEvaluator reteEvaluator) {
        BetaMemory beta = new BetaMemory( new TupleList(),
                                          null,
                                          this.betaConstraints.createContext(),
                                          NodeTypeEnums.FromNode );
        return (T) new AsyncSendMemory( beta, this.dataProvider );
    }
   

    @Override
    public LeftTuple createPeer(LeftTuple original) {
        FromNodeLeftTuple peer = new FromNodeLeftTuple();
        peer.initPeer( (BaseLeftTuple) original, this );
        original.setPeer( peer );
        return peer;
    }    

    public boolean isLeftTupleMemoryEnabled() {
        return tupleMemoryEnabled;
    }

    public void setLeftTupleMemoryEnabled(boolean tupleMemoryEnabled) {
        this.tupleMemoryEnabled = tupleMemoryEnabled;
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

    public short getType() {
        return NodeTypeEnums.AsyncSendNode;
    } 

    public static class AsyncSendMemory extends AbstractBaseLinkedListNode<Memory>
        implements
        Serializable,
        SegmentNodeMemory {
        private static final long serialVersionUID = 510l;

        private DataProvider      dataProvider;

        private final BetaMemory         betaMemory;
        public Object                    providerContext;

        public AsyncSendMemory( BetaMemory betaMemory,
                                DataProvider dataProvider) {
            this.betaMemory = betaMemory;
            this.dataProvider = dataProvider;
            this.providerContext = dataProvider.createContext();
        }

        public short getNodeType() {
            return NodeTypeEnums.AsyncSendNode;
        }

        public SegmentMemory getSegmentMemory() {
            return betaMemory.getSegmentMemory();
        }

        public void setSegmentMemory(SegmentMemory segmentMemory) {
            betaMemory.setSegmentMemory(segmentMemory);
        }

        public BetaMemory getBetaMemory() {
            return betaMemory;
        }

        public void reset() {
            this.betaMemory.reset();
            this.providerContext = dataProvider.createContext();
        }

        @Override
        public long getNodePosMaskBit() {
            return betaMemory.getNodePosMaskBit();
        }

        @Override
        public void setNodePosMaskBit( long segmentPos ) {
            betaMemory.setNodePosMaskBit( segmentPos );
        }

        @Override
        public void setNodeDirtyWithoutNotify() {
            betaMemory.setNodeDirtyWithoutNotify();
        }

        @Override
        public void setNodeCleanWithoutNotify() {
            betaMemory.setNodeCleanWithoutNotify();
        }
    }
    
    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     boolean leftTupleMemoryEnabled) {
        return new FromNodeLeftTuple(factHandle, this, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(final InternalFactHandle factHandle,
                                     final LeftTuple leftTuple,
                                     final Sink sink) {
        return new FromNodeLeftTuple(factHandle,leftTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     Sink sink,
                                     PropagationContext pctx, boolean leftTupleMemoryEnabled) {
        return new FromNodeLeftTuple(leftTuple, sink, pctx,
                                     leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     Sink sink) {
        return new FromNodeLeftTuple(leftTuple, rightTuple, sink );
    }   
    
    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     Sink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new FromNodeLeftTuple(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled );        
    }

    @Override
    public ObjectTypeNode getObjectTypeNode() {
        return leftInput.getObjectTypeNode();
    }

    public void doAttach( BuildContext context ) {
        super.doAttach(context);
        this.leftInput.addTupleSink( this, context );
    }

    protected boolean doRemove(final RuleRemovalContext context,
                               final ReteooBuilder builder) {

        if ( !this.isInUse() ) {
            getLeftTupleSource().removeTupleSink( this );
            return true;
        }
        return false;
    }

}
