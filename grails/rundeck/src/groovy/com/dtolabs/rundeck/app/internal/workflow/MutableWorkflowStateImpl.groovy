package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.*

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/15/13
 * Time: 3:41 PM
 */
class MutableWorkflowStateImpl implements MutableWorkflowState {
    def ArrayList<String> mutableNodeSet;
    def ArrayList<String> mutableAllNodes;
    def long stepCount;
    def ExecutionState executionState;
    def Date updateTime;
    def Date startTime;
    def Date endTime;
    def Map<Integer,MutableWorkflowStepState> mutableStepStates;
    def Map<String,MutableWorkflowNodeState> mutableNodeStates;
    private StepIdentifier parentStepId
    def String serverNode

    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount) {
        this(nodeSet,stepCount,null)
    }
    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount, Map<Integer, MutableWorkflowStepStateImpl> steps) {
        this(nodeSet,stepCount,steps,null,null)
    }

    MutableWorkflowStateImpl(List<String> nodeSet, long stepCount, Map<Integer, MutableWorkflowStepStateImpl> steps, StepIdentifier parentStepId, String serverNode) {
        this.serverNode=serverNode
        this.parentStepId = parentStepId
        this.mutableNodeSet = new ArrayList<>()
        this.mutableAllNodes = new ArrayList<>()
        if (null != nodeSet) {
            this.mutableNodeSet.addAll(nodeSet)
        }
        this.mutableAllNodes.addAll(mutableNodeSet)
        this.stepCount = stepCount
        mutableStepStates = new HashMap<Integer, MutableWorkflowStepState>()
        for (int i = 1; i <= stepCount; i++) {
            mutableStepStates[i - 1] = steps && steps[i - 1] ? steps[i - 1] : new MutableWorkflowStepStateImpl(StateUtils.stepIdentifierAppend(parentStepId, StateUtils.stepIdentifier(i)))
        }
        this.executionState = ExecutionState.WAITING
        mutableNodeStates = new HashMap<String, MutableWorkflowNodeState>()
        mutableAllNodes.each { node ->
            mutableNodeStates[node] = new MutableWorkflowNodeStateImpl(node)
        }
        if (mutableNodeStates && mutableStepStates) {
            //link nodes to node step states
            mutableNodeStates.each { String node, MutableWorkflowNodeState nstate ->
                mutableStepStates.each { int index, MutableWorkflowStepState step ->
                    if (step.nodeStep) {
                        getOrCreateMutableNodeStepState(step, node, step.stepIdentifier)
                    }
                }
            }
        }
        if (mutableStepStates && serverNode) {
            mutableStepStates.each { int index, MutableWorkflowStepState step ->
                if (!step.nodeStep && !step.hasSubWorkflow()) {
                    //a workflow step, e.g. plugin, without a sub workflow
                    getOrCreateMutableNodeStepState(step, serverNode, step.stepIdentifier)
                }
            }
        }

    }

    MutableWorkflowStepState getAt(Integer index){
        return mutableStepStates[index-1]
    }

    @Override
    List<WorkflowStepState> getStepStates() {
        return mutableStepStates.sort().values() as List
    }

    @Override
    Map<String,? extends WorkflowNodeState> getNodeStates() {
        return mutableNodeStates
    }

    @Override
    List<String> getNodeSet() {
        return mutableNodeSet
    }

    @Override
    List<String> getAllNodes() {
        return mutableAllNodes
    }

    @Override
    void updateStateForStep(StepIdentifier identifier,StepStateChange stepStateChange, Date timestamp) {
        updateStateForStep(identifier,0,stepStateChange,timestamp)
    }
    @Override
    void updateStateForStep(StepIdentifier identifier, int index,StepStateChange stepStateChange, Date timestamp) {
        touchWFState(timestamp)

        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;
        MutableWorkflowStepState currentStep = locateStepWithContext(identifier, index,states)
        if (identifier.context.size() - index > 1) {
            descendUpdateStateForStep(currentStep, identifier, index, stepStateChange, timestamp)
            return
        }

        //update the step found
        List<MutableStepState> toUpdate=[]
        if (stepStateChange.isNodeState()) {
            //find node state in stepstate
            def nodeName = stepStateChange.nodeName
            toUpdate << updateNodeStepState(currentStep, nodeName, identifier, stepStateChange)

            if (!currentStep.nodeStep && nodeSet) {
                // change to a nodeStep since we have seen a node state for it
                if (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1) {
                    currentStep.setNodeStepTargets(nodeSet)
                }
            }
        } else if (!currentStep.nodeStep) {
            //overall step state
            toUpdate << currentStep.mutableStepState

            if(serverNode && !currentStep.hasSubWorkflow()){
                //treat server node as node owner for this step
                toUpdate << updateNodeStepState(currentStep, serverNode, identifier, stepStateChange)
            }

            toUpdate.each{ toup->
                toup.executionState = updateState(toup.executionState, stepStateChange.stepState.executionState)
            }
        } else {
            toUpdate << currentStep.mutableStepState
            if (nodeSet && (null == currentStep.nodeStepTargets || currentStep.nodeStepTargets.size() < 1)) {
                currentStep.setNodeStepTargets(nodeSet)
            }
        }
        transitionIfWaiting(currentStep.mutableStepState)

        //update state
        toUpdate*.errorMessage = stepStateChange.stepState.errorMessage
        if (stepStateChange.stepState.metadata) {
            toUpdate.each {toup->
                if (null == toup.metadata) {
                    toup.metadata = [:]
                }
            }
            toUpdate*.metadata << stepStateChange.stepState.metadata
        }
        toUpdate.each { toup ->

            if (!toup.startTime) {
                toup.startTime = timestamp
            }
            toup.updateTime = timestamp
            if (toup.executionState.isCompletedState()) {
                toup.endTime = timestamp
            }
        }


        if(stepStateChange.nodeState && currentStep.nodeStep
                || !stepStateChange.nodeState && !currentStep.nodeStep && !currentStep.hasSubWorkflow() && serverNode) {
            //if it was a node state change
            //or a non-node step without a workflow (e.g. plugin), and we are treating the serverNode as the target

            if (stepStateChange.stepState.executionState.isCompletedState()) {
                //if change state is completion:
                finishNodeStepIfNodesFinished(currentStep, timestamp)
            } else if (currentStep.stepState.executionState.isCompletedState()
                    && stepStateChange.stepState.executionState == ExecutionState.RUNNING_HANDLER) {
                //else if current step was completed, but step change is RUNNING_HANDLER
                currentStep.mutableStepState.executionState = ExecutionState.RUNNING_HANDLER
            }
        }

    }

    /**
     * Update a node state due to a node step state change
     * @param currentStep
     * @param nodeName
     * @param identifier
     * @param stepStateChange
     * @return
     */
    private MutableStepState updateNodeStepState(MutableWorkflowStepState currentStep, String nodeName, StepIdentifier identifier, StepStateChange stepStateChange) {
        MutableStepState toUpdate = getOrCreateMutableNodeStepState(currentStep, nodeName, identifier)
        toUpdate.executionState = updateState(toUpdate.executionState, stepStateChange.stepState.executionState)
        if(stepStateChange.stepState.metadata) {
            if (toUpdate.metadata) {
                toUpdate.metadata << stepStateChange.stepState.metadata
            }else{
                toUpdate.metadata = stepStateChange.stepState.metadata
            }
        }
        if(stepStateChange.stepState.errorMessage){
            if (toUpdate.errorMessage) {
                toUpdate.errorMessage += stepStateChange.stepState.errorMessage
            } else {
                toUpdate.errorMessage = stepStateChange.stepState.errorMessage
            }
        }
        if(!toUpdate.startTime && stepStateChange.stepState.startTime){
            toUpdate.startTime = stepStateChange.stepState.startTime
        }
        if(!toUpdate.updateTime && stepStateChange.stepState.updateTime){
            toUpdate.updateTime = stepStateChange.stepState.updateTime
        }
        if(toUpdate.executionState.isCompletedState() && stepStateChange.stepState.endTime){
            toUpdate.endTime = stepStateChange.stepState.endTime
        }

        mutableNodeStates[nodeName].mutableNodeState.executionState = toUpdate.executionState

        //TODO: need to merge this data
        mutableNodeStates[nodeName].mutableNodeState.metadata = toUpdate.metadata
        mutableNodeStates[nodeName].mutableNodeState.errorMessage = toUpdate.errorMessage
        mutableNodeStates[nodeName].mutableNodeState.updateTime = toUpdate.updateTime
        mutableNodeStates[nodeName].mutableNodeState.startTime = toUpdate.startTime
        mutableNodeStates[nodeName].mutableNodeState.endTime = toUpdate.endTime

        mutableNodeStates[nodeName].lastIdentifier = identifier
        toUpdate
    }

    /**
     * For a node and step, create or return the shared node+step mutable state
     * @param currentStep
     * @param nodeName
     * @param identifier
     * @return
     */
    private MutableStepState getOrCreateMutableNodeStepState(MutableWorkflowStepState currentStep, String nodeName, StepIdentifier identifier) {
        if (null == currentStep.nodeStateMap[nodeName]) {
            //create it
            currentStep.mutableNodeStateMap[nodeName] = new MutableStepStateImpl()
        }
        //connect step-oriented state to node-oriented state
        if (null == mutableNodeStates[nodeName]) {
            mutableNodeStates[nodeName] = new MutableWorkflowNodeStateImpl(nodeName)
        }
        if (null == mutableNodeStates[nodeName].mutableStepStateMap[identifier]) {
            mutableNodeStates[nodeName].mutableStepStateMap[identifier] = currentStep.mutableNodeStateMap[nodeName]
        }
        return currentStep.mutableNodeStateMap[nodeName]
    }

/**
     * Descend into a sub workflow to update state
     * @param currentStep
     * @param identifier
     * @param stepStateChange
     * @param timestamp
     */
    private void descendUpdateStateForStep(MutableWorkflowStepState currentStep, StepIdentifier identifier, int index,StepStateChange stepStateChange, Date timestamp) {
        transitionIfWaiting(currentStep.mutableStepState)
        //recurse to the workflow list to find the right index

        MutableWorkflowState subflow = currentStep.hasSubWorkflow() ?
            currentStep.mutableSubWorkflowState :
            currentStep.createMutableSubWorkflowState(null, 0)
        //recursively update subworkflow state for the step in the subcontext
        subflow.updateStateForStep(identifier, index + 1, stepStateChange, timestamp);
    }

    /**
     * If all node step targets are completed, finalize the step state
     * @param currentStep
     * @param timestamp
     * @return
     */
    private finishNodeStepIfNodesFinished(MutableWorkflowStepState currentStep,Date timestamp){
        boolean finished = currentStep.nodeStepTargets.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        if (finished) {
            boolean aborted = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.ABORTED }
            boolean failed = currentStep.nodeStateMap.values()*.executionState.any { it == ExecutionState.FAILED }
            def overall = aborted ? ExecutionState.ABORTED : failed ? ExecutionState.FAILED : ExecutionState.SUCCEEDED
            finalizeNodeStep(overall, currentStep,timestamp)
        }
    }
    /**
     * Finalize the execution state of a Node step, based on the collective state of all target nodes
     * @param overall
     * @param currentStep
     * @param timestamp
     * @return
     */
    private finalizeNodeStep(ExecutionState overall, MutableWorkflowStepState currentStep,Date timestamp){
        def nodeTargets = currentStep.nodeStep?(currentStep.nodeStepTargets?:this.nodeSet):[serverNode]
        boolean finished = currentStep.nodeStateMap && nodeTargets?.every { node -> currentStep.nodeStateMap[node]?.executionState?.isCompletedState() }
        boolean aborted = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.ABORTED }
        boolean abortedAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.ABORTED }
        boolean failed = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.FAILED }
        boolean failedAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.FAILED }
        boolean succeeded = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.any { it == ExecutionState.SUCCEEDED }
        boolean succeededAll = currentStep.nodeStateMap && currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.SUCCEEDED }
        boolean notStartedAll = currentStep.nodeStateMap?.size() == 0 ||
                currentStep.nodeStateMap?.values()*.executionState.every { it == ExecutionState.WAITING || it == null }
        ExecutionState result=overall
        if(finished){
            //all nodes finished
            if(abortedAll){
                result=ExecutionState.ABORTED
            }else if(failedAll){
                result=ExecutionState.FAILED
            }else if(succeededAll){
                result=ExecutionState.SUCCEEDED
            }else{
                result=ExecutionState.NODE_MIXED
            }
        }else if (aborted && !failed && !succeeded) {
            //partial aborted
            result = ExecutionState.ABORTED
        } else if (!aborted && failed && !succeeded) {
            //partial failed
            result = ExecutionState.FAILED
        } else if (!failed && !aborted && succeeded) {
            //partial success
            result = ExecutionState.NODE_PARTIAL_SUCCEEDED
        }else if (notStartedAll) {
            //not started
            result = ExecutionState.NOT_STARTED
        } else {
            result = ExecutionState.NODE_MIXED
        }

        if(currentStep.nodeStep || currentStep.hasSubWorkflow()){
            currentStep.mutableStepState.executionState = result
        }else{
            currentStep.mutableStepState.executionState = updateState(currentStep.mutableStepState.executionState, result)
        }
        currentStep.mutableStepState.endTime=timestamp

        //update any node states which are WAITING to NOT_STARTED
        nodeTargets.each{String node->
            if(!currentStep.mutableNodeStateMap[node]){
                currentStep.mutableNodeStateMap[node] = new MutableStepStateImpl(executionState:ExecutionState.WAITING)
            }
            MutableStepState state = currentStep.mutableNodeStateMap[node]
            if (state && state.executionState == ExecutionState.WAITING) {
                state.executionState = updateState(state.executionState, ExecutionState.NOT_STARTED)
                state.endTime=timestamp
            }else if (state && (state.executionState == ExecutionState.RUNNING || state.executionState == ExecutionState.RUNNING_HANDLER)) {
                state.executionState = updateState(state.executionState, ExecutionState.ABORTED)
                state.endTime=timestamp
            }
        }
    }

    private MutableWorkflowStepState locateStepWithContext(StepIdentifier identifier, int index,Map<Integer, MutableWorkflowStepState> states) {
        MutableWorkflowStepState currentStep
        StepContextId subid = identifier.context[index]
        int ndx=subid.step-1
        if (ndx >= states.size() || null == states[ndx]) {
            states[ndx] = new MutableWorkflowStepStateImpl(StateUtils.stepIdentifier(subid))
            stepCount = states.size()
        }
        currentStep = states[ndx]
        currentStep
    }

    private void touchWFState(Date timestamp) {
        executionState = transitionStateIfWaiting(executionState)
        if (null == this.updateTime || this.updateTime < timestamp) {
            this.updateTime = timestamp
        }
        if (null == this.startTime) {
            this.startTime = timestamp
        }
    }


    private void transitionIfWaiting(MutableStepState step) {
        step.executionState = transitionStateIfWaiting(step.executionState)
    }
    private ExecutionState transitionStateIfWaiting(ExecutionState state) {
        if (waitingState(state)) {
            return updateState(state, ExecutionState.RUNNING)
        }else{
            return state
        }
    }

    private static boolean waitingState(ExecutionState state) {
        null == state || state == ExecutionState.WAITING
    }

    /**
     * Update state change
     * @param fromState
     * @param toState
     * @return
     */
    public static ExecutionState updateState(ExecutionState fromState, ExecutionState toState) {
        if(fromState==toState){
            return toState
        }
        def allowed=[
                (ExecutionState.WAITING):[null,ExecutionState.WAITING],
                (ExecutionState.RUNNING): [null, ExecutionState.WAITING,ExecutionState.RUNNING],
                (ExecutionState.RUNNING_HANDLER):[null,ExecutionState.WAITING,ExecutionState.FAILED, ExecutionState.RUNNING,ExecutionState.RUNNING_HANDLER],
        ]
        ExecutionState.values().findAll{it.isCompletedState()}.each{
            allowed[it]= [it,ExecutionState.RUNNING, ExecutionState.RUNNING_HANDLER, ExecutionState.WAITING]
        }
        if (toState == null) {
//            System.err.println("Cannot change state to ${toState}")
            throw new IllegalStateException("Cannot change state to ${toState}")
        }
        if(!(fromState in allowed[toState])){
//            System.err.println("Cannot change from " + fromState + " to " + toState)
            throw new IllegalStateException("Cannot change from " + fromState + " to " + toState)
        }

        toState
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodenames) {
        updateWorkflowState(null,false,executionState,timestamp,nodenames,this)
    }
    void updateWorkflowState(StepIdentifier identifier, boolean quellFinalState, ExecutionState executionState, Date timestamp, List<String> nodenames, MutableWorkflowState parent) {
        touchWFState(timestamp)
        if (!(quellFinalState && executionState.isCompletedState())) {
            this.executionState = updateState(this.executionState, executionState)
        }
        if (null != nodenames && (null == mutableNodeSet || mutableNodeSet.size() < 1)) {
            mutableNodeSet = new ArrayList<>(nodenames)
            def mutableNodeStates=parent.mutableNodeStates
            def allNodes=parent.allNodes
            mutableNodeSet.each { node ->
                if(!mutableNodeStates[node]){
                    mutableNodeStates[node] = new MutableWorkflowNodeStateImpl(node)
                }
                if(!allNodes.contains(node)){
                    allNodes<<node
                }
                mutableStepStates.keySet().each {int ident->
                    if(mutableStepStates[ident].nodeStep){
                        if(null==mutableStepStates[ident].mutableNodeStateMap[node]){
                            mutableStepStates[ident].mutableNodeStateMap[node]=new MutableStepStateImpl()
                        }
                        mutableNodeStates[node].mutableStepStateMap[StateUtils.stepIdentifierAppend(identifier,StateUtils.stepIdentifier(ident + 1))]= mutableStepStates[ident].mutableNodeStateMap[node]
                    }
                }
            }
        }else if(null!=nodenames){
            def allNodes = parent.allNodes
            nodenames.each { node ->
                if (!allNodes.contains(node)) {
                    allNodes << node
                }
            }
        }
        if(executionState.isCompletedState()){
            cleanupSteps(executionState, timestamp)
            this.endTime=timestamp
        }
    }

    /**
     * Finalize all incomplete steps in the workflow with the given overall state
     * @param executionState
     * @param timestamp
     */
    private void cleanupSteps(ExecutionState executionState, Date timestamp) {
        mutableStepStates.each { i, step ->
            if (!step.stepState.executionState.isCompletedState()) {
                resolveStepCompleted(executionState, timestamp, i + 1, step)
            }
        }
    }

    /**
     *
     * @param executionState
     * @param timestamp
     * @param states
     */

    /**
     * Resolve the completed state of a step based on overall workflow completion state
     * @param executionState
     * @param date
     * @param i
     * @param mutableWorkflowStepState
     */
    def resolveStepCompleted(ExecutionState executionState, Date date, int i, MutableWorkflowStepState mutableWorkflowStepState) {
        boolean finalized=false
        if(mutableWorkflowStepState.nodeStep){
            finalizeNodeStep(executionState,mutableWorkflowStepState,date)
            finalized=true
        }
        if(mutableWorkflowStepState.hasSubWorkflow()){
            finalizeSubWorkflowStep(mutableWorkflowStepState, executionState, date)
        } else if (!mutableWorkflowStepState.nodeStep && serverNode) {
            finalizeNodeStep(executionState, mutableWorkflowStepState, date)
            finalized=true
        }
        if(!finalized){
            finalizeStepExecutionState(mutableWorkflowStepState, executionState, date)
        }
    }

    /**
     * Finalize the execution state of a non node-step. If it has a subworkflow, finalize the sub workflow.  Otherwise
     * finalize the serverNode state.
     * @param mutableWorkflowStepState
     * @param executionState
     * @param date
     */
    private void finalizeSubWorkflowStep(MutableWorkflowStepState mutableWorkflowStepState, ExecutionState executionState, Date date) {
        //resolve the sub workflow
        mutableWorkflowStepState.mutableSubWorkflowState.updateSubWorkflowState(
                mutableWorkflowStepState.stepIdentifier,
                mutableWorkflowStepState.stepIdentifier.context.size(),
                false,
                executionState,
                updateTime,
                null,
                this
        )

    }

    /**
     * Finalize execution state only for a step
     * @param mutableWorkflowStepState
     * @param executionState
     * @param date
     */
    private void finalizeStepExecutionState(MutableWorkflowStepState mutableWorkflowStepState,
                                            ExecutionState executionState, Date date) {
        def curstate = mutableWorkflowStepState.mutableStepState.executionState
        def newstate = executionState
        switch (curstate) {
            case null:
            case ExecutionState.WAITING:
                newstate = ExecutionState.NOT_STARTED
                break
            case ExecutionState.RUNNING:
            case ExecutionState.RUNNING_HANDLER:
                newstate = ExecutionState.ABORTED
                break
        }
        mutableWorkflowStepState.mutableStepState.executionState = updateState(curstate, newstate)
        mutableWorkflowStepState.mutableStepState.endTime = date
    }

    @Override
    void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState, ExecutionState executionState, Date timestamp, List<String> nodeNames, MutableWorkflowState parent) {
        touchWFState(timestamp)
        Map<Integer, MutableWorkflowStepState> states = mutableStepStates;
        if (identifier.context.size() - index > 0) {
            //descend one step
            MutableWorkflowStepState nextStep = locateStepWithContext(identifier, index, states)
            MutableWorkflowState nextWorkflow = nextStep.hasSubWorkflow() ?
                nextStep.mutableSubWorkflowState :
                nextStep.createMutableSubWorkflowState(null, 0)

            transitionIfWaiting(nextStep.mutableStepState)
            //more steps to descend
            nextWorkflow.updateSubWorkflowState(identifier, index + 1, nextStep.nodeStep, executionState, timestamp, nodeNames, parent ?: this);
        } else {
            //update the workflow state for this workflow
            updateWorkflowState(identifier,  quellFinalState, executionState, timestamp, nodeNames, parent ?: this)
        }
    }

    @Override
    public java.lang.String toString() {
        return "WF{" +
                "nodes=" + mutableNodeSet +
                ", stepCount=" + stepCount +
                ", state=" + executionState +
                ", timestamp=" + updateTime +
                ", steps=" + mutableStepStates +
                '}';
    }
}
