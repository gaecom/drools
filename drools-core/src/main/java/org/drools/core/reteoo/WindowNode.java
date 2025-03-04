/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.common.EventFactHandle;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.Memory;
import org.drools.core.common.MemoryFactory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.reteoo.ObjectTypeNode.ObjectTypeNodeMemory;
import org.drools.core.reteoo.WindowNode.WindowMemory;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.Behavior;
import org.drools.core.rule.BehaviorManager;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.SlidingTimeWindow;
import org.drools.core.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.core.base.ObjectType;
import org.drools.core.common.PropagationContext;
import org.drools.core.util.bitmask.BitMask;

/**
 * <code>WindowNodes</code> are nodes in the <code>Rete</code> network used
 * to manage windows. They support multiple types of windows, like
 * sliding windows, tumbling windows, etc.
 * <p/>
 * This class must act as a lock-gate for all working memory actions on it
 * and propagated down the network in this branch, as there can be concurrent
 * threads propagating events and expiring events working on this node at the
 * same time. It requires it to be thread safe.
 */
public class WindowNode extends ObjectSource
        implements ObjectSinkNode,
                   RightTupleSink,
                   MemoryFactory<WindowMemory> {

    private static final long serialVersionUID = 540l;
    private List<AlphaNodeFieldConstraint> constraints;
    protected BehaviorManager              behavior;
    private EntryPointId                   entryPoint;
    private ObjectSinkNode                 previousRightTupleSinkNode;
    private ObjectSinkNode                 nextRightTupleSinkNode;
    private transient ObjectTypeNode.Id rightInputOtnId = ObjectTypeNode.DEFAULT_ID;

    public WindowNode() {
    }

    /**
     * Construct a <code>WindowNode</code> with a unique id using the provided
     * list of <code>AlphaNodeFieldConstraint</code> and the given <code>ObjectSource</code>.
     *
     * @param id           Node's ID
     * @param constraints  Node's constraints
     * @param behaviors    list of behaviors for this window node
     * @param objectSource Node's object source
     */
    public WindowNode(final int id,
                      final List<AlphaNodeFieldConstraint> constraints,
                      final List<Behavior> behaviors,
                      final ObjectSource objectSource,
                      final BuildContext context) {
        super(id,
              context.getPartitionId(),
              context.getRuleBase().getConfiguration().isMultithreadEvaluation(),
              objectSource,
              context.getRuleBase().getConfiguration().getAlphaNodeHashingThreshold(),
              context.getRuleBase().getConfiguration().getAlphaNodeRangeIndexThreshold());
        // needs to be cloned as the list is managed externally
        this.constraints = new ArrayList<>(constraints);
        this.behavior = new BehaviorManager(behaviors);
        this.entryPoint = context.getCurrentEntryPoint();
        for ( Behavior b :  behaviors ) {
            if ( b instanceof SlidingTimeWindow ) {
                ((SlidingTimeWindow)b).setWindowNode( this );
            }
        }
        hashcode = calculateHashCode();
        initMemoryId( context );
    }

    public short getType() {
        return NodeTypeEnums.WindowNode;
    }

    /**
     * Returns the <code>FieldConstraints</code>
     *
     * @return <code>FieldConstraints</code>
     */
    public List<AlphaNodeFieldConstraint> getConstraints() {
        return this.constraints;
    }

    /**
     * Returns the list of behaviors for this window node
     */
    public Behavior[] getBehaviors() {
        return behavior.getBehaviors();
    }

    public void doAttach(BuildContext context) {
        this.source.addObjectSink(this);
    }

    public void assertObject(final InternalFactHandle factHandle,
                             final PropagationContext pctx,
                             final ReteEvaluator reteEvaluator) {
        EventFactHandle evFh = ( EventFactHandle ) factHandle;
        for (AlphaNodeFieldConstraint constraint : constraints) {
            if (!constraint.isAllowed(evFh, reteEvaluator)) {
                return;
            }
        }

        RightTuple rightTuple = new RightTupleImpl( evFh, this );
        rightTuple.setPropagationContext( pctx );

        InternalFactHandle clonedFh = evFh.cloneAndLink();  // this is cloned, as we need to separate the child RightTuple references
        rightTuple.setContextObject( clonedFh );

        // process the behavior
        final WindowMemory memory = reteEvaluator.getNodeMemory(this);
        if (!behavior.assertFact(memory.behaviorContext, clonedFh, pctx, reteEvaluator)) {
            return;
        }

        this.sink.propagateAssertObject(clonedFh, pctx, reteEvaluator);
    }

    @Override
    public void retractRightTuple(RightTuple rightTuple, PropagationContext pctx, ReteEvaluator reteEvaluator) {
        if (isInUse()) {
            // This retraction could be the effect of an event expiration, but this node could be no
            // longer in use since an incremental update could have concurrently removed it
            WindowMemory memory = reteEvaluator.getNodeMemory( this );
            behavior.retractFact( memory.behaviorContext, rightTuple.getFactHandle(), pctx, reteEvaluator );
        }

        InternalFactHandle clonedFh = ( InternalFactHandle ) rightTuple.getContextObject();
        ObjectTypeNode.doRetractObject(clonedFh, pctx, reteEvaluator);
    }

    @Override
    public void modifyRightTuple(RightTuple rightTuple, PropagationContext context, ReteEvaluator reteEvaluator) {
        EventFactHandle originalFactHandle = ( EventFactHandle ) rightTuple.getFactHandle();
        EventFactHandle cloneFactHandle  = ( EventFactHandle ) rightTuple.getContextObject();
        originalFactHandle.quickCloneUpdate( cloneFactHandle ); // make sure all fields are updated

        // behavior modify
        boolean isAllowed = true;
        for (AlphaNodeFieldConstraint constraint : constraints) {
            if (!constraint.isAllowed(cloneFactHandle, reteEvaluator)) {
                isAllowed = false;
                break;
            }
        }

        if  ( isAllowed ) {
            ModifyPreviousTuples modifyPreviousTuples = new ModifyPreviousTuples(cloneFactHandle.detachLinkedTuples() );

            this.sink.propagateModifyObject(cloneFactHandle, modifyPreviousTuples, context, reteEvaluator);
            modifyPreviousTuples.retractTuples(context, reteEvaluator);
        } else {
            ObjectTypeNode.doRetractObject(cloneFactHandle, context, reteEvaluator);
        }
    }

    public void modifyObject(InternalFactHandle factHandle,
                             ModifyPreviousTuples modifyPreviousTuples,
                             PropagationContext context,
                             ReteEvaluator reteEvaluator) {
        RightTuple rightTuple = modifyPreviousTuples.peekRightTuple(partitionId);

        // if the peek is for a different OTN we assume that it is after the current one and then this is an assert
        while ( rightTuple != null && rightTuple.getInputOtnId().before( getRightInputOtnId() ) ) {
            modifyPreviousTuples.removeRightTuple(partitionId);

            // we skipped this node, due to alpha hashing, so retract now
            rightTuple.setPropagationContext( context );
            rightTuple.retractTuple( context, reteEvaluator );
            rightTuple = modifyPreviousTuples.peekRightTuple(partitionId);
        }

        if ( rightTuple != null && rightTuple.getInputOtnId().equals( getRightInputOtnId()) ) {
            modifyPreviousTuples.removeRightTuple(partitionId);
            rightTuple.reAdd();
            modifyRightTuple( rightTuple, context, reteEvaluator );
        } else {
            // RightTuple does not exist for this node, so create and continue as assert
            assertObject( factHandle, context, reteEvaluator );
        }
    }

    public void byPassModifyToBetaNode(InternalFactHandle factHandle,
                                       ModifyPreviousTuples modifyPreviousTuples,
                                       PropagationContext context,
                                       ReteEvaluator reteEvaluator) {
        sink.byPassModifyToBetaNode(factHandle, modifyPreviousTuples, context, reteEvaluator);
    }

    public void updateSink(final ObjectSink sink,
                           final PropagationContext context,
                           final InternalWorkingMemory workingMemory) {
        final ObjectTypeNodeMemory omem = workingMemory.getNodeMemory( getObjectTypeNode());
        Iterator<InternalFactHandle> it = omem.iterator();

        while (it.hasNext()) {
            assertObject( it.next(), context, workingMemory );
        }
    }

    /**
     * Creates the WindowNode's memory.
     */
    public WindowMemory createMemory(final RuleBaseConfiguration config, ReteEvaluator reteEvaluator) {
        WindowMemory memory = new WindowMemory();
        memory.behaviorContext = this.behavior.createBehaviorContext();
        return memory;
    }

    public String toString() {
        return "[WindowNode(" + this.id + ") constraints=" + this.constraints + "]";
    }

    private int calculateHashCode() {
        return this.source.hashCode() * 17 + ((this.constraints != null) ? this.constraints.hashCode() : 0);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if ( object == null || !(object instanceof WindowNode) || this.hashCode() != object.hashCode() ) {
            return false;
        }
        WindowNode other = (WindowNode) object;
        return this.source.getId() == other.source.getId() && this.constraints.equals(other.constraints) && behavior.equals(other.behavior);
    }

    /**
     * Returns the next node
     *
     * @return The next ObjectSinkNode
     */
    public ObjectSinkNode getNextObjectSinkNode() {
        return this.nextRightTupleSinkNode;
    }

    /**
     * Sets the next node
     *
     * @param next The next ObjectSinkNode
     */
    public void setNextObjectSinkNode(final ObjectSinkNode next) {
        this.nextRightTupleSinkNode = next;
    }

    /**
     * Returns the previous node
     *
     * @return The previous ObjectSinkNode
     */
    public ObjectSinkNode getPreviousObjectSinkNode() {
        return this.previousRightTupleSinkNode;
    }

    /**
     * Sets the previous node
     *
     * @param previous The previous ObjectSinkNode
     */
    public void setPreviousObjectSinkNode(final ObjectSinkNode previous) {
        this.previousRightTupleSinkNode = previous;
    }

    public EntryPointId getEntryPoint() {
        return entryPoint;
    }

    @Override
    public BitMask calculateDeclaredMask(ObjectType modifiedType, List<String> settableProperties) {
        throw new UnsupportedOperationException();
    }

    public ObjectTypeNode.Id getRightInputOtnId() {
        return rightInputOtnId;
    }

    public void setRightInputOtnId(ObjectTypeNode.Id rightInputOtnId) {
        this.rightInputOtnId = rightInputOtnId;
    }

    public static class WindowMemory implements Memory {
        public Behavior.Context[] behaviorContext;

        public short getNodeType() {
            return NodeTypeEnums.WindowNode;
        }

        public SegmentMemory getSegmentMemory() {
            return null;
        }

        public void setSegmentMemory( SegmentMemory segmentMemory ) {
            throw new UnsupportedOperationException();
        }

        public Memory getPrevious() {
            throw new UnsupportedOperationException();
        }

        public void setPrevious( Memory previous ) {
            throw new UnsupportedOperationException();
        }

        public Memory getNext() {
            throw new UnsupportedOperationException();
        }

        public void setNext( Memory next ) {
            throw new UnsupportedOperationException();
        }

        public void nullPrevNext() {
            throw new UnsupportedOperationException();
        }

        public void reset() {
        }

        public Collection<EventFactHandle> getFactHandles() {
            List<EventFactHandle> eventFactHandles = new ArrayList<>(  );
            for (Behavior.Context ctx : behaviorContext) {
                for (EventFactHandle efh : ctx.getFactHandles()) {
                    eventFactHandles.add(efh.getLinkedFactHandle());
                }
            }
            return eventFactHandles;
        }
    }
}
