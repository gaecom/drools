/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.util.List;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.common.BaseNode;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.common.UpdateContext;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.Pattern;
import org.drools.core.base.ObjectType;
import org.drools.core.util.bitmask.AllSetBitMask;
import org.drools.core.util.bitmask.BitMask;
import org.drools.core.util.bitmask.EmptyBitMask;

import static org.drools.core.reteoo.PropertySpecificUtil.isPropertyReactive;

public abstract class AbstractTerminalNode extends BaseNode implements TerminalNode {

    private LeftTupleSource tupleSource;

    private BitMask declaredMask = EmptyBitMask.get();
    private BitMask inferredMask = EmptyBitMask.get();
    private BitMask negativeMask = EmptyBitMask.get();

    private LeftTupleNode[] pathNodes;

    private transient PathEndNode[] pathEndNodes;

    private PathMemSpec pathMemSpec;

    private int objectCount;

    public AbstractTerminalNode() { }

    public AbstractTerminalNode(int id, RuleBasePartitionId partitionId, boolean partitionsEnabled, LeftTupleSource source, final BuildContext context) {
        super(id, partitionId, partitionsEnabled);
        this.tupleSource = source;
        this.setObjectCount(getLeftTupleSource().getObjectCount()); // 'terminal' nodes do not increase the count
        context.addPathEndNode(this);
        initMemoryId( context );
    }

    @Override
    public PathMemSpec getPathMemSpec() {
        if (pathMemSpec == null) {
            pathMemSpec = calculatePathMemSpec( null );
        }
        return pathMemSpec;
    }

    @Override
    public void resetPathMemSpec(TerminalNode removingTN) {
        pathMemSpec = removingTN == null ? null : calculatePathMemSpec( null, removingTN );
    }

    @Override
    public void setPathEndNodes(PathEndNode[] pathEndNodes) {
        this.pathEndNodes = pathEndNodes;
    }

    @Override
    public PathEndNode[] getPathEndNodes() {
        return pathEndNodes;
    }

    public int getPathIndex() {
        return tupleSource.getPathIndex() + 1;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(int count) {
        objectCount = count;
    }

    protected void initDeclaredMask(BuildContext context) {
        if ( !(unwrapTupleSource() instanceof LeftInputAdapterNode)) {
            // RTN's not after LIANode are not relevant for property specific, so don't block anything.
            setDeclaredMask( AllSetBitMask.get() );
            return;
        }

        Pattern pattern = context.getLastBuiltPatterns()[0];
        ObjectType objectType = pattern.getObjectType();

        if ( isPropertyReactive(context, objectType) ) {
            List<String> accessibleProperties = pattern.getAccessibleProperties( context.getRuleBase() );
            setDeclaredMask( pattern.getPositiveWatchMask(accessibleProperties) );
            setNegativeMask( pattern.getNegativeWatchMask(accessibleProperties) );
        } else  {
            // if property specific is not on, then accept all modification propagations
            setDeclaredMask( AllSetBitMask.get() );
        }
    }

    public void initInferredMask() {
        LeftTupleSource leftTupleSource = unwrapTupleSource();
        if ( leftTupleSource instanceof LeftInputAdapterNode && ((LeftInputAdapterNode)leftTupleSource).getParentObjectSource() instanceof AlphaNode ) {
            AlphaNode alphaNode = (AlphaNode) ((LeftInputAdapterNode)leftTupleSource).getParentObjectSource();
            setInferredMask( alphaNode.updateMask( getDeclaredMask() ) );
        } else {
            setInferredMask(  getDeclaredMask() );
        }

        setInferredMask( getInferredMask().resetAll( getNegativeMask() ) );
        if ( getNegativeMask().isAllSet() && !getDeclaredMask().isAllSet() ) {
            setInferredMask( getInferredMask().setAll( getDeclaredMask() ) );
        }
    }

    public LeftTupleSource unwrapTupleSource() {
        return tupleSource instanceof FromNode ? tupleSource.getLeftTupleSource() : tupleSource;
    }

    public abstract RuleImpl getRule();
    

    public PathMemory createMemory(RuleBaseConfiguration config, ReteEvaluator reteEvaluator) {
        return initPathMemory( this, new PathMemory(this, reteEvaluator) );
    }

    public static PathMemory initPathMemory( PathEndNode pathEndNode, PathMemory pmem ) {
        PathMemSpec pathMemSpec = pathEndNode.getPathMemSpec();
        pmem.setAllLinkedMaskTest(pathMemSpec. allLinkedTestMask );
        pmem.setSegmentMemories( new SegmentMemory[pathMemSpec.smemCount] );
        return pmem;
    }

    public LeftTuple createPeer(LeftTuple original) {
        RuleTerminalNodeLeftTuple peer = (RuleTerminalNodeLeftTuple) AgendaComponentFactory.get().createTerminalTuple();
        peer.initPeer( (BaseLeftTuple) original, this );
        original.setPeer( peer );
        return peer;
    }

    protected boolean doRemove(final RuleRemovalContext context,
                               final ReteooBuilder builder) {
        getLeftTupleSource().removeTupleSink(this);
        this.tupleSource = null;
        return true;
    }

    public LeftTupleSource getLeftTupleSource() {
        return this.tupleSource;
    }

    public BitMask getDeclaredMask() {
        return declaredMask;
    }

    public BitMask getInferredMask() {
        return inferredMask;
    }
    
    public BitMask getLeftInferredMask() {
        return inferredMask;
    }

    public void setDeclaredMask(BitMask mask) {
        declaredMask = mask;
    }

    public void setInferredMask(BitMask mask) {
        inferredMask = mask;
    }

    public BitMask getNegativeMask() {
        return negativeMask;
    }

    public void setNegativeMask(BitMask mask) {
        negativeMask = mask;
    }

    public void networkUpdated(UpdateContext updateContext) {
        getLeftTupleSource().networkUpdated(updateContext);
    }

    public boolean isInUse() {
        return false;
    }

    public boolean isLeftTupleMemoryEnabled() {
        return false;
    }

    public void setLeftTupleMemoryEnabled(boolean tupleMemoryEnabled) {
        // do nothing, this can only ever be false
    }

    public static LeftTupleNode[] getPathNodes(PathEndNode endNode) {
        LeftTupleNode[] pathNodes = new LeftTupleNode[endNode.getPathIndex() + 1];
        for (LeftTupleNode node = endNode; node != null; node = node.getLeftTupleSource()) {
            pathNodes[node.getPathIndex()] = node;
        }
        return pathNodes;
    }

    public LeftTupleNode[] getPathNodes() {
        if (pathNodes == null) {
            pathNodes = getPathNodes( this );
        }
        return pathNodes;
    }

    public final boolean hasPathNode(LeftTupleNode node) {
        for (LeftTupleNode pathNode : getPathNodes()) {
            if (node.getId() == pathNode.getId()) {
                return true;
            }
        }
        return false;
    }

    public final boolean isTerminalNodeOf(LeftTupleNode node) {
        for (PathEndNode pathEndNode : getPathEndNodes()) {
            if (pathEndNode.hasPathNode( node )) {
                return true;
            }
        }
        return false;
    }

    public LeftTupleSinkPropagator getSinkPropagator() {
        return EmptyLeftTupleSinkAdapter.getInstance();
    }

    @Override
    public final void setPartitionIdWithSinks( RuleBasePartitionId partitionId ) {
        this.partitionId = partitionId;
    }

    @Override
    public ObjectTypeNode getObjectTypeNode() {
        return getLeftTupleSource().getObjectTypeNode();
    }
}
